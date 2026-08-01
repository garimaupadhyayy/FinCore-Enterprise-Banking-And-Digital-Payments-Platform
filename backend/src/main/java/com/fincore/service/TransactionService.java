package com.fincore.service;

import com.fincore.exception.AccountFrozenException;
import com.fincore.exception.AccountNotFoundException;
import com.fincore.exception.InsufficientFundsException;
import com.fincore.model.Account;
import com.fincore.model.Transaction;
import com.fincore.repository.AccountRepository;
import com.fincore.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Core banking logic.
 *
 * Concurrency note: two customers could try to move money in/out of the SAME
 * account at the exact same moment (e.g. two browser tabs, or two people
 * transferring to the same beneficiary account). To avoid a lost-update race
 * condition we take a per-account-number lock before mutating balances, and
 * wrap the whole operation in a DB transaction so that if anything fails
 * midway, ALL changes are rolled back (no "half transfer" is ever persisted).
 */
@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    // one lock per account number, created on demand, so unrelated accounts
    // never block each other during concurrent transfers
    private final ConcurrentHashMap<String, ReentrantLock> accountLocks = new ConcurrentHashMap<>();

    private static final int FRAUD_WINDOW_MINUTES = 10;
    private static final int FRAUD_TRANSACTION_THRESHOLD = 5;

    public TransactionService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    private ReentrantLock lockFor(String accountNumber) {
        return accountLocks.computeIfAbsent(accountNumber, k -> new ReentrantLock());
    }

    @Transactional
    public Transaction transfer(String fromAccountNumber, String toAccountNumber, BigDecimal amount) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero");
        }

        // Always lock in a consistent (alphabetical) order across both accounts
        // to prevent deadlocks when two transfers happen in opposite directions
        // at the same time (A->B and B->A concurrently).
        String first = fromAccountNumber.compareTo(toAccountNumber) < 0 ? fromAccountNumber : toAccountNumber;
        String second = fromAccountNumber.compareTo(toAccountNumber) < 0 ? toAccountNumber : fromAccountNumber;

        ReentrantLock lock1 = lockFor(first);
        ReentrantLock lock2 = lockFor(second);

        lock1.lock();
        try {
            lock2.lock();
            try {
                Account from = accountRepository.findByAccountNumber(fromAccountNumber)
                        .orElseThrow(() -> new AccountNotFoundException("Sender account not found: " + fromAccountNumber));
                Account to = accountRepository.findByAccountNumber(toAccountNumber)
                        .orElseThrow(() -> new AccountNotFoundException("Recipient account not found: " + toAccountNumber));

                if (from.getStatus() != Account.AccountStatus.ACTIVE) {
                    throw new AccountFrozenException("Sender account is not active: " + fromAccountNumber);
                }
                if (to.getStatus() != Account.AccountStatus.ACTIVE) {
                    throw new AccountFrozenException("Recipient account is not active: " + toAccountNumber);
                }
                if (from.getBalance().compareTo(amount) < 0) {
                    throw new InsufficientFundsException("Insufficient balance in account: " + fromAccountNumber);
                }

                // mutate balances
                from.setBalance(from.getBalance().subtract(amount));
                to.setBalance(to.getBalance().add(amount));

                accountRepository.save(from);
                accountRepository.save(to);

                Transaction txn = new Transaction(from, to, amount, Transaction.TransactionType.TRANSFER);
                txn.setStatus(Transaction.TransactionStatus.SUCCESS);

                // Fraud rule: flag if this sender has made too many transactions
                // in a short rolling window (simple rule-based detection, no ML needed)
                LocalDateTime windowStart = LocalDateTime.now().minusMinutes(FRAUD_WINDOW_MINUTES);
                List<Transaction> recent = transactionRepository
                        .findByFromAccountIdAndTimestampAfter(from.getId(), windowStart);

                if (recent.size() + 1 >= FRAUD_TRANSACTION_THRESHOLD) {
                    txn.setFlaggedFraud(true);
                }

                return transactionRepository.save(txn);

            } finally {
                lock2.unlock();
            }
        } finally {
            lock1.unlock();
            // If anything above threw, @Transactional rolls back both balance
            // updates AND the transaction record together — an atomic transfer.
        }
    }

    public List<Transaction> getHistoryForAccount(Long accountId) {
        return transactionRepository.findByFromAccountIdOrToAccountIdOrderByTimestampDesc(accountId, accountId);
    }

    public List<Transaction> getFlaggedTransactions() {
        return transactionRepository.findByFlaggedFraudTrue();
    }
}
