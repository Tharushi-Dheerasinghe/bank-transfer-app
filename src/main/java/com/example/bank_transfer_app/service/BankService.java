package com.example.bank_transfer_app.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bank_transfer_app.exception.AccounNotFoundException;
import com.example.bank_transfer_app.exception.InsufficientBalanceException;
import com.example.bank_transfer_app.model.BankTransaction;
import com.example.bank_transfer_app.model.Customer;
import com.example.bank_transfer_app.model.TransactionStatus;
import com.example.bank_transfer_app.repository.BankTransactionRepository;
import com.example.bank_transfer_app.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) 
public class BankService {

    private final CustomerRepository customerRepository;
    private final BankTransactionRepository bankTransactionRepository;

   
   @Transactional(rollbackFor = Exception.class)
  
    public BankTransaction transfer(String fromAccount, String toAccount, BigDecimal amount, boolean simulateFailure) {
        
        
        if (fromAccount.equals(toAccount)) {
            throw new RuntimeException("Sender and receiver must be different accounts");
        }

        
        Customer sender = customerRepository.getByAccountNumber(fromAccount);
        if (sender == null) {
            throw new AccounNotFoundException("Sender account not found");
        }
        Customer receiver = customerRepository.getByAccountNumber(toAccount);
        if (receiver == null) {
            throw new AccounNotFoundException("Receiver account not found");
        }

        int rowsUpdated = customerRepository.debit(fromAccount, amount);
        if (rowsUpdated == 0) {
            throw new InsufficientBalanceException("Insufficient balance in account " + fromAccount);
        }

        if (simulateFailure) {
            throw new RuntimeException("Simulated system failure after the withdrawal");
        }

        customerRepository.credit(toAccount, amount);

        BankTransaction transaction = new BankTransaction();
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription("Transfer completed");
        
        return bankTransactionRepository.save(transaction);
    }        

   
    @Transactional
    public Customer createCustomer(Customer customer) {
        return customerRepository.save(customer);
    }
}

