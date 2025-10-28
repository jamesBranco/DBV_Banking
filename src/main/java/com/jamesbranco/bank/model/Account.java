package com.jamesbranco.bank.model;

import java.math.BigDecimal;
import java.util.Objects;

public class Account {
    private final String id;
    private final String ownerUserId;
    private final String accountNumber;
    private BigDecimal balance;

    public Account(String id, String ownerUserId, String accountNumber) {
        this.id = Objects.requireNonNull(id);
        this.ownerUserId = Objects.requireNonNull(ownerUserId);
        this.accountNumber = Objects.requireNonNull(accountNumber);
        this.balance = BigDecimal.ZERO.setScale(2);
    }

    public String getId() { return id; }
    public String getOwnerUserId() { return ownerUserId; }
    public String getAccountNumber() { return accountNumber; }
    public BigDecimal getBalance() { return balance; }

    public void deposit(BigDecimal amount) {
        requirePositive(amount);
        balance = balance.add(amount).setScale(2);
    }

    public void withdraw(BigDecimal amount) {
        requirePositive(amount);
        if (balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds");
        }
        balance = balance.subtract(amount).setScale(2);
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }
}
