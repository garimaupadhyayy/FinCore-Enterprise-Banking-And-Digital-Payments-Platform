package com.fincore.controller;

import com.fincore.model.Account;
import com.fincore.model.Transaction;
import com.fincore.service.AccountService;
import com.fincore.service.TransactionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AccountService accountService;
    private final TransactionService transactionService;

    public AdminController(AccountService accountService, TransactionService transactionService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    @GetMapping("/accounts/pending")
    public List<Account> pendingAccounts() {
        return accountService.getPendingApprovals();
    }

    @PutMapping("/accounts/{id}/approve")
    public Account approve(@PathVariable Long id) {
        return accountService.approveAccount(id);
    }

    @PutMapping("/accounts/{id}/freeze")
    public Account freeze(@PathVariable Long id) {
        return accountService.freezeAccount(id);
    }

    @GetMapping("/transactions/flagged")
    public List<Transaction> flaggedTransactions() {
        return transactionService.getFlaggedTransactions();
    }
}
