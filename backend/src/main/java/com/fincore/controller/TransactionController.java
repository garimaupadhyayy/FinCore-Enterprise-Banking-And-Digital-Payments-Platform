package com.fincore.controller;

import com.fincore.dto.TransferRequest;
import com.fincore.model.Account;
import com.fincore.model.Transaction;
import com.fincore.service.AccountService;
import com.fincore.service.TransactionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final AccountService accountService;

    public TransactionController(TransactionService transactionService, AccountService accountService) {
        this.transactionService = transactionService;
        this.accountService = accountService;
    }

    @PostMapping("/transfer")
    public Transaction transfer(@RequestBody TransferRequest request) {
        return transactionService.transfer(
                request.getFromAccountNumber(),
                request.getToAccountNumber(),
                request.getAmount()
        );
    }

    @GetMapping("/history/{accountNumber}")
    public List<Transaction> history(@PathVariable String accountNumber) {
        Account account = accountService.getByAccountNumber(accountNumber);
        return transactionService.getHistoryForAccount(account.getId());
    }
}
