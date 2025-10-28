
-- Simple Banking System - MySQL Schema (Beginner-Friendly)
-- Engine: InnoDB, Charset: utf8mb4, Collation: utf8mb4_0900_ai_ci (MySQL 8.0+)

-- 0) Create and use database
CREATE DATABASE IF NOT EXISTS simple_banking
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;
USE simple_banking;

-- 1) Users
-- - Stores app users (customers, employees, admins)
-- - role uses an ENUM to keep it simple
CREATE TABLE IF NOT EXISTS users (
  id            CHAR(36)      NOT NULL,          -- UUID as text (easy from Java)
  name          VARCHAR(100)  NOT NULL,
  email         VARCHAR(255)  NOT NULL,
  password_hash CHAR(64)      NOT NULL,          -- SHA-256 hex
  role          ENUM('CUSTOMER','EMPLOYEE','ADMIN') NOT NULL DEFAULT 'CUSTOMER',
  created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB;

-- 2) Accounts
-- - Each account belongs to one user
-- - balance stored as DECIMAL(12,2)
CREATE TABLE IF NOT EXISTS accounts (
  id              CHAR(36)     NOT NULL,          -- UUID as text
  owner_user_id   CHAR(36)     NOT NULL,          -- FK -> users.id
  account_number  CHAR(12)     NOT NULL,          -- simple 12-char number (unique)
  balance         DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  status          ENUM('OPEN','FROZEN','CLOSED') NOT NULL DEFAULT 'OPEN',
  created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_accounts_number (account_number),
  KEY idx_accounts_owner (owner_user_id),
  CONSTRAINT fk_accounts_user
    FOREIGN KEY (owner_user_id) REFERENCES users(id)
    ON DELETE CASCADE
    ON UPDATE RESTRICT
) ENGINE=InnoDB;

-- 3) Transactions
-- - One row per movement of money
-- - amount must be positive
-- - type kept as ENUM to match Java's TransactionType
CREATE TABLE IF NOT EXISTS transactions (
  id          CHAR(36)     NOT NULL,
  account_id  CHAR(36)     NOT NULL,            -- FK -> accounts.id
  type        ENUM('DEPOSIT','WITHDRAWAL','TRANSFER_IN','TRANSFER_OUT') NOT NULL,
  amount      DECIMAL(12,2) NOT NULL,
  note        VARCHAR(255),
  created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_tx_account (account_id),
  CONSTRAINT fk_tx_account
    FOREIGN KEY (account_id) REFERENCES accounts(id)
    ON DELETE CASCADE
    ON UPDATE RESTRICT,
  CONSTRAINT chk_tx_positive_amount CHECK (amount > 0)
) ENGINE=InnoDB;

-- 4) Helpful indexes (already included above)
-- - uq_users_email (unique email)
-- - uq_accounts_number (unique account number)
-- - idx_accounts_owner (owner_user_id for lookups)
-- - idx_tx_account (account_id for history lookups)

-- 5) Optional starter data (safe to remove)
-- NOTE: IDs are example UUIDs. Replace as needed.
INSERT INTO users (id, name, email, password_hash, role)
VALUES
  ('11111111-1111-1111-1111-111111111111', 'James Branco', 'james@example.com',
   'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', -- 'password123' (SHA-256) for demo only
   'CUSTOMER')
ON DUPLICATE KEY UPDATE email = email;

INSERT INTO accounts (id, owner_user_id, account_number, balance, status)
VALUES
  ('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', '123456789012', 250.00, 'OPEN')
ON DUPLICATE KEY UPDATE account_number = account_number;

INSERT INTO transactions (id, account_id, type, amount, note)
VALUES
  ('33333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222222', 'DEPOSIT', 250.00, 'Initial deposit')
ON DUPLICATE KEY UPDATE note = note;

-- 6) Simple view: recent transactions per account (last 30 days)
CREATE OR REPLACE VIEW v_recent_transactions AS
SELECT
  t.account_id,
  t.id            AS transaction_id,
  t.type,
  t.amount,
  t.note,
  t.created_at
FROM transactions t
WHERE t.created_at >= (CURRENT_TIMESTAMP - INTERVAL 30 DAY)
ORDER BY t.created_at DESC;

-- 7) Basic permissions example (optional, for local dev)
-- CREATE USER 'bank_user'@'localhost' IDENTIFIED BY 'bank_pass';
-- GRANT SELECT, INSERT, UPDATE, DELETE ON simple_banking.* TO 'bank_user'@'localhost';
-- FLUSH PRIVILEGES;

-- Done!
