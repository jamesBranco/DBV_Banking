package com.jamesbranco.bank;

import com.jamesbranco.bank.model.Role;
import com.jamesbranco.bank.repo.InMemoryStore;
import com.jamesbranco.bank.service.BankService;

import java.math.BigDecimal;

public class Main {
    public static void main(String[] args) {
        var store = new InMemoryStore();
        var bank  = new BankService(store);

        String userId = bank.registerUser("James Branco", "james@example.com", "StrongPass!123", Role.CUSTOMER);
        bank.authenticate("james@example.com", "StrongPass!123");

        String a1 = bank.openCheckingAccount(userId);
        String a2 = bank.openCheckingAccount(userId);

        bank.deposit(a1, new BigDecimal("250.00"), "Initial deposit");
        bank.withdraw(a1, new BigDecimal("25.50"), "Snacks");
        bank.transfer(a1, a2, new BigDecimal("100.00"), "Move to savings");

        System.out.println("A1 balance: " + bank.getBalance(a1));
        System.out.println("A2 balance: " + bank.getBalance(a2));
        System.out.println("A1 tx: " + bank.getTransactions(a1).size());
        System.out.println("A2 tx: " + bank.getTransactions(a2).size());
    }
}
