package com.jamesbranco.bank.service;

import com.jamesbranco.bank.model.*;
import com.jamesbranco.bank.repo.InMemoryStore;
import com.jamesbranco.bank.security.PasswordUtil;
import com.jamesbranco.bank.util.IdGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

public class BankService {
    private final InMemoryStore store;

    public BankService(InMemoryStore store) {
        this.store = store;
    }

    // Users

    public String registerUser(String name, String email, String plaintextPassword, Role role) {
        store.findUserByEmail(email)
                .ifPresent(u -> { throw new IllegalArgumentException("Email already registered"); });

        String id = IdGenerator.newId();
        String hash = PasswordUtil.hash(plaintextPassword);
        store.saveUser(new InMemoryStore.UserRecord(id, name, email, hash, role.name()));
        return id;
    }

    public String authenticate(String email, String plaintextPassword) {
        var user = store.findUserByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!PasswordUtil.matches(plaintextPassword, user.passwordHash)) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        return user.id;
    }

    // find role of a user
    public Role getUserRole(String userId) {
        var user = store.findUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return Role.valueOf(user.role);
    }

    // Accounts

    public String openCheckingAccount(String userId) {
        var user = store.findUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        String accountId = IdGenerator.newId();
        String acctNum = IdGenerator.newAccountNumber();
        var acct = new Account(accountId, user.id, acctNum);
        store.saveAccount(acct);
        return accountId;
    }

    public BigDecimal getBalance(String accountId) {
        return store.findAccountById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"))
                .getBalance();
    }

    public List<Account> getAccountsForUser(String userId) {
        return store.findAccountsByUserId(userId);
    }

    public void deposit(String accountId, BigDecimal amount, String note) {
        var acct = store.findAccountById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        if (acct.isFrozen()) {
            throw new IllegalStateException("Account is frozen");
        }
        acct.deposit(amount);
        var tx = new Transaction(IdGenerator.newId(), accountId,
                TransactionType.DEPOSIT, amount, Instant.now(), note);
        store.appendTransaction(tx);
    }

    public void withdraw(String accountId, BigDecimal amount, String note) {
        var acct = store.findAccountById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        if (acct.isFrozen()) {
            throw new IllegalStateException("Account is frozen");
        }
        acct.withdraw(amount);
        var tx = new Transaction(IdGenerator.newId(), accountId,
                TransactionType.WITHDRAWAL, amount, Instant.now(), note);
        store.appendTransaction(tx);
    }

    public void transfer(String fromAccountId, String toAccountId, BigDecimal amount, String note) {
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Cannot transfer to same account");
        }

        var from = store.findAccountById(fromAccountId)
                .orElseThrow(() -> new IllegalArgumentException("From account not found"));
        var to = store.findAccountById(toAccountId)
                .orElseThrow(() -> new IllegalArgumentException("To account not found"));

        if (from.isFrozen()) {
            throw new IllegalStateException("From account is frozen");
        }

        from.withdraw(amount);
        to.deposit(amount);

        store.appendTransaction(new Transaction(IdGenerator.newId(), fromAccountId,
                TransactionType.TRANSFER_OUT, amount, Instant.now(), note));
        store.appendTransaction(new Transaction(IdGenerator.newId(), toAccountId,
                TransactionType.TRANSFER_IN, amount, Instant.now(), note));
    }

    public List<Transaction> getTransactions(String accountId) {
        return store.getTransactions(accountId);
    }

    // ADMIN FEATURES

    public Collection<InMemoryStore.UserRecord> getAllUsers() {
        return store.findAllUsers();
    }

    public Collection<Account> getAllAccounts() {
        return store.findAllAccounts();
    }

    public void freezeAccount(String accountId) {
        var acct = store.findAccountById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        acct.freeze();
    }

    public void unfreezeAccount(String accountId) {
        var acct = store.findAccountById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        acct.unfreeze();
    }
}
