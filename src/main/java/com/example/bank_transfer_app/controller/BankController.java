package com.example.bank_transfer_app.controller;


import java.math.BigDecimal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.bank_transfer_app.dto.TransferRequest;
import com.example.bank_transfer_app.model.Customer;
import com.example.bank_transfer_app.model.TransactionStatus;
import com.example.bank_transfer_app.repository.BankTransactionRepository;
import com.example.bank_transfer_app.repository.CustomerRepository;
import com.example.bank_transfer_app.service.BankService;
import com.example.bank_transfer_app.service.TransactionAuditService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;
    private final TransactionAuditService auditService;
    private final CustomerRepository customerRepository;
    private final BankTransactionRepository bankTransactionRepository;

  
    @GetMapping("/")
    public String dashboard(@RequestParam(required = false) String keyword, Model model) {
        if (keyword != null && !keyword.isEmpty()) {
            model.addAttribute("customers", customerRepository.searchByName(keyword));
        } else {
            model.addAttribute("customers", customerRepository.findAllOrdered());
        }
        
        model.addAttribute("totalBalance", customerRepository.getTotalBalance());
        model.addAttribute("customerCount", customerRepository.countCustomersNative());
        model.addAttribute("latestTransactions", bankTransactionRepository.findAllLatest());
        model.addAttribute("successCount", bankTransactionRepository.countByStatus(TransactionStatus.SUCCESS));
        model.addAttribute("failedCount", bankTransactionRepository.countByStatus(TransactionStatus.FAILED));
        
        if (!model.containsAttribute("transferRequest")) {
            model.addAttribute("transferRequest", new TransferRequest());
        }
        if (!model.containsAttribute("newCustomer")) {
            model.addAttribute("newCustomer", new Customer());
        }
        
        return "bank"; 
    }

   
    @PostMapping("/transfer")
    public String transferMoney(@Valid @ModelAttribute("transferRequest") TransferRequest request, 
                                BindingResult result, 
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.transferRequest", result);
            redirectAttributes.addFlashAttribute("transferRequest", request);
            return "redirect:/";
        }

        try {
          
            bankService.transfer(request.getFromAccount(), request.getToAccount(), request.getAmount(), request.isSimulateFailure());
            redirectAttributes.addFlashAttribute("successMessage", "Transfer completed successfully!");
        } catch (Exception e) {
          
            auditService.recordFailure(request.getFromAccount(), request.getToAccount(), request.getAmount(), e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/";
    }

    
    @PostMapping("/customers")
    public String addCustomer(@ModelAttribute("newCustomer") Customer customer, RedirectAttributes redirectAttributes) {
        try {
            if(customerRepository.countAccounts(customer.getAccountNumber()) > 0) {
                redirectAttributes.addFlashAttribute("customerError", "Account number already exists.");
                return "redirect:/";
            }
            bankService.createCustomer(customer);
            redirectAttributes.addFlashAttribute("customerSuccess", "Customer added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("customerError", "Failed to add customer.");
        }
        return "redirect:/";
    }

   
    @GetMapping("/accounts/{accountNumber}/history")
    public String accountHistory(@PathVariable String accountNumber, Model model) {
        Customer customer = customerRepository.getByAccountNumber(accountNumber);
        model.addAttribute("customer", customer);
        model.addAttribute("transactions", bankTransactionRepository.findHistory(accountNumber));
        model.addAttribute("totalSent", bankTransactionRepository.getTotalAmountSent(accountNumber));
        return "history"; 
        
    }

    @GetMapping("/large-transfers")
    public String largeTransfers(@RequestParam(defaultValue = "10000") BigDecimal min, Model model) {
        model.addAttribute("transactions", bankTransactionRepository.findLargeTransfers(min));
        model.addAttribute("keyword", "Large Transfers (Min: " + min + ")");
        return "history";
    }

    @PostMapping("/deposit")
    public String depositAccount(@RequestParam String accountNo, @RequestParam BigDecimal amount, RedirectAttributes redirectAttributes) {
        bankService.deposit(accountNo, amount);
        redirectAttributes.addFlashAttribute("successMessage", "Deposit Successful!");
        return "redirect:/";
    }

    @PostMapping("/withdraw")
    public String withdrawAccount(@RequestParam String accountNo, @RequestParam BigDecimal amount, RedirectAttributes redirectAttributes) {
        bankService.withdraw(accountNo, amount);
        redirectAttributes.addFlashAttribute("successMessage", "Withdrawal Successful!");
        return "redirect:/";
    }
}

