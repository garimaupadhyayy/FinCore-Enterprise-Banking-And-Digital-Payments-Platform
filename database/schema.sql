-- =========================================================
-- FinCore Database Schema
-- Banking & Digital Payments Platform
-- =========================================================

CREATE DATABASE IF NOT EXISTS fincore_db;
USE fincore_db;

-- ---------------------------------------------------------
-- Table: customers
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,      -- BCrypt hash
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    role ENUM('CUSTOMER', 'ADMIN') NOT NULL DEFAULT 'CUSTOMER',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---------------------------------------------------------
-- Table: accounts
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(20) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL,
    account_type ENUM('SAVINGS', 'CURRENT') NOT NULL,
    balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    status ENUM('PENDING_APPROVAL', 'ACTIVE', 'FROZEN', 'CLOSED') NOT NULL DEFAULT 'PENDING_APPROVAL',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_account_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    CONSTRAINT chk_balance_non_negative CHECK (balance >= 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------
-- Table: transactions
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_account_id BIGINT,
    to_account_id BIGINT,
    amount DECIMAL(15,2) NOT NULL,
    type ENUM('TRANSFER', 'DEPOSIT', 'WITHDRAWAL') NOT NULL,
    status ENUM('SUCCESS', 'FAILED', 'ROLLED_BACK') NOT NULL DEFAULT 'SUCCESS',
    flagged_fraud BOOLEAN NOT NULL DEFAULT FALSE,
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_txn_from_account FOREIGN KEY (from_account_id) REFERENCES accounts(id),
    CONSTRAINT fk_txn_to_account FOREIGN KEY (to_account_id) REFERENCES accounts(id),
    CONSTRAINT chk_amount_positive CHECK (amount > 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------
-- Table: audit_log  (tracks admin actions for compliance)
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_username VARCHAR(50) NOT NULL,
    action VARCHAR(100) NOT NULL,
    target_entity VARCHAR(100),
    details VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Indexes for common query patterns
CREATE INDEX idx_accounts_customer ON accounts(customer_id);
CREATE INDEX idx_txn_from_account ON transactions(from_account_id);
CREATE INDEX idx_txn_to_account ON transactions(to_account_id);
CREATE INDEX idx_txn_timestamp ON transactions(timestamp);


-- =========================================================
-- STORED PROCEDURE: Calculate and credit monthly interest
-- for all active SAVINGS accounts (simple flat rate demo)
-- =========================================================
DELIMITER $$

CREATE PROCEDURE sp_apply_monthly_interest(IN p_rate_percent DECIMAL(5,2))
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_account_id BIGINT;
    DECLARE v_balance DECIMAL(15,2);
    DECLARE cur CURSOR FOR
        SELECT id, balance FROM accounts
        WHERE account_type = 'SAVINGS' AND status = 'ACTIVE';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;

    read_loop: LOOP
        FETCH cur INTO v_account_id, v_balance;
        IF done THEN
            LEAVE read_loop;
        END IF;

        UPDATE accounts
        SET balance = balance + (v_balance * p_rate_percent / 100)
        WHERE id = v_account_id;

        INSERT INTO transactions (from_account_id, to_account_id, amount, type, status)
        VALUES (NULL, v_account_id, (v_balance * p_rate_percent / 100), 'DEPOSIT', 'SUCCESS');

    END LOOP;

    CLOSE cur;
END$$

DELIMITER ;


-- =========================================================
-- STORED PROCEDURE: Get full statement for an account
-- between two dates
-- =========================================================
DELIMITER $$

CREATE PROCEDURE sp_get_statement(
    IN p_account_id BIGINT,
    IN p_start_date DATETIME,
    IN p_end_date DATETIME
)
BEGIN
    SELECT t.id, t.type, t.amount, t.status, t.flagged_fraud, t.timestamp,
           fa.account_number AS from_account, ta.account_number AS to_account
    FROM transactions t
    LEFT JOIN accounts fa ON t.from_account_id = fa.id
    LEFT JOIN accounts ta ON t.to_account_id = ta.id
    WHERE (t.from_account_id = p_account_id OR t.to_account_id = p_account_id)
      AND t.timestamp BETWEEN p_start_date AND p_end_date
    ORDER BY t.timestamp DESC;
END$$

DELIMITER ;


-- =========================================================
-- TRIGGER: Fraud flag on rapid successive transactions
-- If an account sends 5+ transactions in the last 10 minutes,
-- auto-flag the newest transaction as fraud for admin review.
-- =========================================================
DELIMITER $$

CREATE TRIGGER trg_fraud_check_after_insert
AFTER INSERT ON transactions
FOR EACH ROW
BEGIN
    DECLARE recent_count INT;

    IF NEW.from_account_id IS NOT NULL THEN
        SELECT COUNT(*) INTO recent_count
        FROM transactions
        WHERE from_account_id = NEW.from_account_id
          AND timestamp >= (NEW.timestamp - INTERVAL 10 MINUTE);

        IF recent_count >= 5 THEN
            UPDATE transactions
            SET flagged_fraud = TRUE
            WHERE id = NEW.id;
        END IF;
    END IF;
END$$

DELIMITER ;


-- =========================================================
-- TRIGGER: Prevent transactions on frozen/closed accounts
-- at the database layer (defense in depth, backend also checks this)
-- =========================================================
DELIMITER $$

CREATE TRIGGER trg_block_frozen_account_txn
BEFORE INSERT ON transactions
FOR EACH ROW
BEGIN
    DECLARE v_status VARCHAR(20);

    IF NEW.from_account_id IS NOT NULL THEN
        SELECT status INTO v_status FROM accounts WHERE id = NEW.from_account_id;
        IF v_status IN ('FROZEN', 'CLOSED') THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cannot process transaction: sender account is frozen or closed';
        END IF;
    END IF;
END$$

DELIMITER ;


-- =========================================================
-- SEED DATA (for demo/testing)
-- Default admin password is "admin123" (BCrypt hash below)
-- =========================================================
INSERT INTO customers (username, password, full_name, email, role)
VALUES ('admin', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5L5ZAqA5Y5EYQ4vqPnQKvxAeCnP3q', 'Bank Admin', 'admin@fincore.com', 'ADMIN');

-- Note: replace the hash above with one generated via BCryptPasswordEncoder
-- (see README for a quick way to generate it), then insert demo customers/accounts as needed.
