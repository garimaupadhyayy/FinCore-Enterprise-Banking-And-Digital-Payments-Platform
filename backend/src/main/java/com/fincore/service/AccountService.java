package com.fincore.service;

import com.fincore.exception.AccountNotFoundException;
import com.fincore.model.Account;
import com.fincore.model.Customer;
import com.fincore.repository.AccountRepository;
import com.fincore.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;

    public AccountService(AccountRepository accountRepository, CustomerRepository customerRepository) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
    }

    public List<Account> getAccountsForUsername(String username) {
        Customer customer = customerRepository.findByUsername(username)
                .orElseThrow(() -> new AccountNotFoundException("Customer not found: " + username));
        return accountRepository.findByCustomerId(customer.getId());
    }

    public Account getByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
    }

    public List<Account> getPendingApprovals() {
        return accountRepository.findByStatus(Account.AccountStatus.PENDING_APPROVAL);
    }

    public Account approveAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: id=" + accountId));
        account.setStatus(Account.AccountStatus.ACTIVE);
        return accountRepository.save(account);
    }

    public Account freezeAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: id=" + accountId));
        account.setStatus(Account.AccountStatus.FROZEN);
        return accountRepository.save(account);
    }
}
