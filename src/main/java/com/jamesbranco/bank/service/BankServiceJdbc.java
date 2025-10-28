package com.jamesbranco.bank.service;

import com.jamesbranco.bank.jdbc.Jdbc;
import com.jamesbranco.bank.model.*;
import com.jamesbranco.bank.security.PasswordUtil;
import com.jamesbranco.bank.util.IdGenerator;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class BankServiceJdbc {

    // --- Users ---
    public String registerUser(String name, String email, String plaintextPassword, Role role) {
        String id = IdGenerator.newId();
        String hash = PasswordUtil.hash(plaintextPassword);
        String sql = "INSERT INTO users (id, name, email, password_hash, role) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = Jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, name);
            ps.setString(3, email);
            ps.setString(4, hash);
            ps.setString(5, role.name());
            ps.executeUpdate();
            return id;
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Duplicate")) {
                throw new IllegalArgumentException("Email already registered");
            }
            throw new RuntimeException(e);
        }
    }

    public String authenticate(String email, String plaintextPassword) {
        String sql = "SELECT id, password_hash FROM users WHERE email=?";
        try (Connection c = Jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Invalid credentials");
                String id = rs.getString("id");
                String hash = rs.getString("password_hash");
                if (!PasswordUtil.matches(plaintextPassword, hash))
                    throw new IllegalArgumentException("Invalid credentials");
                return id;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // --- Accounts ---
    public String openCheckingAccount(String userId) {
        String id = IdGenerator.newId();
        String acctNum = IdGenerator.newAccountNumber();
        String sql = "INSERT INTO accounts (id, owner_user_id, account_number, balance, status) VALUES (?, ?, ?, 0.00, 'OPEN')";
        try (Connection c = Jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, userId);
            ps.setString(3, acctNum);
            ps.executeUpdate();
            return id;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public BigDecimal getBalance(String accountId) {
        String sql = "SELECT balance FROM accounts WHERE id=?";
        try (Connection c = Jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Account not found");
                return rs.getBigDecimal("balance");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void appendTransaction(Connection c, String accountId, TransactionType type, BigDecimal amount, String note) throws SQLException {
        String txId = IdGenerator.newId();
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO transactions (id, account_id, type, amount, note) VALUES (?,?,?,?,?)")) {
            ps.setString(1, txId);
            ps.setString(2, accountId);
            ps.setString(3, type.name());
            ps.setBigDecimal(4, amount);
            ps.setString(5, note);
            ps.executeUpdate();
        }
    }

    public void deposit(String accountId, BigDecimal amount, String note) {
        String sql = "UPDATE accounts SET balance = balance + ? WHERE id=?";
        try (Connection c = Jdbc.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setBigDecimal(1, amount);
                ps.setString(2, accountId);
                int updated = ps.executeUpdate();
                if (updated == 0) throw new IllegalArgumentException("Account not found");
                appendTransaction(c, accountId, TransactionType.DEPOSIT, amount, note);
                c.commit();
            } catch (Exception ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void withdraw(String accountId, BigDecimal amount, String note) {
        String check = "SELECT balance FROM accounts WHERE id=? FOR UPDATE";
        String update = "UPDATE accounts SET balance = balance - ? WHERE id=?";
        try (Connection c = Jdbc.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps1 = c.prepareStatement(check)) {
                ps1.setString(1, accountId);
                try (ResultSet rs = ps1.executeQuery()) {
                    if (!rs.next()) throw new IllegalArgumentException("Account not found");
                    BigDecimal bal = rs.getBigDecimal(1);
                    if (bal.compareTo(amount) < 0) throw new IllegalStateException("Insufficient funds");
                }
            }
            try (PreparedStatement ps2 = c.prepareStatement(update)) {
                ps2.setBigDecimal(1, amount);
                ps2.setString(2, accountId);
                ps2.executeUpdate();
            }
            appendTransaction(c, accountId, TransactionType.WITHDRAWAL, amount, note);
            c.commit();
            c.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void transfer(String fromAccountId, String toAccountId, BigDecimal amount, String note) {
        if (fromAccountId.equals(toAccountId)) throw new IllegalArgumentException("Cannot transfer to same account");
        String lock = "SELECT id, balance FROM accounts WHERE id IN (?, ?) FOR UPDATE";
        try (Connection c = Jdbc.getConnection()) {
            c.setAutoCommit(false);
            // Lock both accounts
            try (PreparedStatement ps = c.prepareStatement(lock)) {
                ps.setString(1, fromAccountId);
                ps.setString(2, toAccountId);
                List<String> seen = new ArrayList<>();
                BigDecimal fromBal = null;
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String id = rs.getString("id");
                        seen.add(id);
                        if (id.equals(fromAccountId)) fromBal = rs.getBigDecimal("balance");
                    }
                }
                if (!seen.contains(fromAccountId) || !seen.contains(toAccountId))
                    throw new IllegalArgumentException("Account not found");
                if (fromBal.compareTo(amount) < 0) throw new IllegalStateException("Insufficient funds");
            }
            try (PreparedStatement deb = c.prepareStatement("UPDATE accounts SET balance = balance - ? WHERE id=?");
                 PreparedStatement cre = c.prepareStatement("UPDATE accounts SET balance = balance + ? WHERE id=?")) {
                deb.setBigDecimal(1, amount);
                deb.setString(2, fromAccountId);
                deb.executeUpdate();
                cre.setBigDecimal(1, amount);
                cre.setString(2, toAccountId);
                cre.executeUpdate();
            }
            appendTransaction(c, fromAccountId, TransactionType.TRANSFER_OUT, amount, note);
            appendTransaction(c, toAccountId, TransactionType.TRANSFER_IN, amount, note);
            c.commit();
            c.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Transaction> getTransactions(String accountId) {
        String sql = "SELECT id, type, amount, note, created_at FROM transactions WHERE account_id=? ORDER BY created_at DESC";
        List<Transaction> out = new ArrayList<>();
        try (Connection c = Jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Transaction(
                            rs.getString("id"),
                            accountId,
                            TransactionType.valueOf(rs.getString("type")),
                            rs.getBigDecimal("amount"),
                            rs.getTimestamp("created_at").toInstant(),
                            rs.getString("note")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return out;
    }
}
