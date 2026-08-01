package com.fincore.repository;

import com.fincore.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByFromAccountIdOrToAccountIdOrderByTimestampDesc(Long fromId, Long toId);

    List<Transaction> findByFlaggedFraudTrue();

    // used by fraud-check logic: count recent transactions from an account
    List<Transaction> findByFromAccountIdAndTimestampAfter(Long accountId, LocalDateTime since);
}
