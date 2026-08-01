package com.fincore.controller;

import com.fincore.model.Account;
import com.fincore.service.AccountService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/my")
    public List<Account> myAccounts(Authentication authentication) {
        return accountService.getAccountsForUsername(authentication.getName());
    }

    @GetMapping("/{accountNumber}")
    public Account getAccount(@PathVariable String accountNumber) {
        return accountService.getByAccountNumber(accountNumber);
    }
}
