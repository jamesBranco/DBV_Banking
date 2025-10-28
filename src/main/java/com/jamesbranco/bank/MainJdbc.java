package com.jamesbranco.bank;

import com.jamesbranco.bank.model.Role;
import com.jamesbranco.bank.service.BankServiceJdbc;

import java.math.BigDecimal;

public class MainJdbc {
    public static void main(String[] args) {
        var bank = new BankServiceJdbc();

        // Register or login (if already exists, catch exception)
        String userId;
        try {
            userId = bank.registerUser("James Branco", "james.jdbc@example.com", "StrongPass!123", Role.CUSTOMER);
        } catch (IllegalArgumentException e) {
            // already registered; authenticate to get id would usually be another method
            userId = bank.authenticate("james.jdbc@example.com", "StrongPass!123");
        }

        String a1 = bank.openCheckingAccount(userId);
        String a2 = bank.openCheckingAccount(userId);

        bank.deposit(a1, new BigDecimal("500.00"), "Initial deposit (JDBC)");
        bank.withdraw(a1, new BigDecimal("50.00"), "ATM (JDBC)");
        bank.transfer(a1, a2, new BigDecimal("120.00"), "Move to savings (JDBC)");

        System.out.println("A1 balance (JDBC): " + bank.getBalance(a1));
        System.out.println("A2 balance (JDBC): " + bank.getBalance(a2));
        System.out.println("A1 tx (JDBC): " + bank.getTransactions(a1).size());
        System.out.println("A2 tx (JDBC): " + bank.getTransactions(a2).size());
    }
}
