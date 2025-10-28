package com.jamesbranco.bank.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public class Transaction {
    private final String id;
    private final String accountId;
    private final TransactionType type;
    private final BigDecimal amount;
    private final Instant timestamp;
    private final String note;

    public Transaction(String id, String accountId, TransactionType type,
                       BigDecimal amount, Instant timestamp, String note) {
        if (amount == null || amount.signum() <= 0)
            throw new IllegalArgumentException("Amount must be positive");
        this.id = Objects.requireNonNull(id);
        this.accountId = Objects.requireNonNull(accountId);
        this.type = Objects.requireNonNull(type);
        this.amount = amount.setScale(2);
        this.timestamp = Objects.requireNonNull(timestamp);
        this.note = note == null ? "" : note;
    }

    public String getId() { return id; }
    public String getAccountId() { return accountId; }
    public TransactionType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public Instant getTimestamp() { return timestamp; }
    public String getNote() { return note; }
}
