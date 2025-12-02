package com.jamesbranco.bank;

import com.jamesbranco.bank.model.Account;
import com.jamesbranco.bank.model.Role;
import com.jamesbranco.bank.repo.InMemoryStore;
import com.jamesbranco.bank.service.BankService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static final Scanner in = new Scanner(System.in);

    public static void main(String[] args) {
        InMemoryStore store = new InMemoryStore();
        BankService bank = new BankService(store);

        // Seed an admin user
        try {
            bank.registerUser("System Admin", "admin@example.com", "Admin123!", Role.ADMIN);
        } catch (IllegalArgumentException ignored) {
            // If admin already exists
        }

        System.out.println("=== Simple Banking System (Phase 3 – Admin Features) ===");

        String userId = ensureUser(bank);
        Role role = bank.getUserRole(userId);

        if (role == Role.ADMIN) {
            runAdminMenu(bank);
        } else {
            runCustomerMenu(bank, userId);
        }
    }

    // COMMON LOGIN / REGISTER

    private static String ensureUser(BankService bank) {
        while (true) {
            System.out.println("\n1) Register new user");
            System.out.println("2) Login existing user");
            System.out.print("Choose an option: ");
            String choice = in.nextLine().trim();
            try {
                return switch (choice) {
                    case "1" -> register(bank);
                    case "2" -> login(bank);
                    default -> {
                        System.out.println("Invalid choice.");
                        yield null;
                    }
                };
            } catch (Exception ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        }
    }

    private static String register(BankService bank) {
        System.out.print("Name: ");
        String name = in.nextLine().trim();
        System.out.print("Email: ");
        String email = in.nextLine().trim();
        System.out.print("Password: ");
        String pw = in.nextLine().trim();

        String userId = bank.registerUser(name, email, pw, Role.CUSTOMER);
        System.out.println("Registered successfully. Your user ID: " + userId);
        return userId;
    }

    private static String login(BankService bank) {
        System.out.print("Email: ");
        String email = in.nextLine().trim();
        System.out.print("Password: ");
        String pw = in.nextLine().trim();
        String userId = bank.authenticate(email, pw);
        System.out.println("Login successful. User ID: " + userId);
        return userId;
    }

    //  MENU

    private static void runCustomerMenu(BankService bank, String userId) {
        System.out.println("\nLogged in as CUSTOMER/EMPLOYEE");
        while (true) {
            System.out.println("\n--- Customer Menu ---");
            System.out.println("1) Open new account");
            System.out.println("2) List my accounts");
            System.out.println("3) Deposit");
            System.out.println("4) Withdraw");
            System.out.println("5) Transfer between my accounts");
            System.out.println("6) View transactions for an account");
            System.out.println("0) Exit");
            System.out.print("Choose an option: ");

            String choice = in.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> openAccount(bank, userId);
                    case "2" -> listAccounts(bank, userId);
                    case "3" -> deposit(bank, userId);
                    case "4" -> withdraw(bank, userId);
                    case "5" -> transfer(bank, userId);
                    case "6" -> viewTransactions(bank, userId);
                    case "0" -> {
                        System.out.println("Goodbye!");
                        return;
                    }
                    default -> System.out.println("Invalid option. Try again.");
                }
            } catch (Exception ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        }
    }

    // ADMIN MENU

    private static void runAdminMenu(BankService bank) {
        System.out.println("\nLogged in as ADMIN");
        while (true) {
            System.out.println("\n--- Admin Menu ---");
            System.out.println("1) List all users");
            System.out.println("2) List all accounts");
            System.out.println("3) Freeze an account");
            System.out.println("4) Unfreeze an account");
            System.out.println("5) View transactions for any account");
            System.out.println("0) Exit");
            System.out.print("Choose an option: ");

            String choice = in.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> listAllUsers(bank);
                    case "2" -> listAllAccounts(bank);
                    case "3" -> freezeAccount(bank);
                    case "4" -> unfreezeAccount(bank);
                    case "5" -> viewAnyAccountTransactions(bank);
                    case "0" -> {
                        System.out.println("Goodbye!");
                        return;
                    }
                    default -> System.out.println("Invalid option. Try again.");
                }
            } catch (Exception ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        }
    }

    private static void listAllUsers(BankService bank) {
        var users = bank.getAllUsers();
        System.out.println("\nUsers:");
        users.forEach(u ->
                System.out.printf("ID=%s | Name=%s | Email=%s | Role=%s%n",
                        u.id, u.name, u.email, u.role)
        );
    }

    private static List<Account> listAllAccounts(BankService bank) {
        var accounts = bank.getAllAccounts();
        System.out.println("\nAccounts:");
        int i = 1;
        for (Account a : accounts) {
            System.out.printf("%d) ID=%s | No=%s | Owner=%s | Balance=%s | Frozen=%s%n",
                    i++, a.getId(), a.getAccountNumber(), a.getOwnerUserId(),
                    a.getBalance(), a.isFrozen());
        }
        return accounts.stream().toList();
    }

    private static void freezeAccount(BankService bank) {
        var accounts = listAllAccounts(bank);
        if (accounts.isEmpty()) return;
        System.out.print("Choose account number to FREEZE: ");
        int idx = Integer.parseInt(in.nextLine().trim()) - 1;
        Account a = accounts.get(idx);
        bank.freezeAccount(a.getId());
        System.out.println("Account frozen.");
    }

    private static void unfreezeAccount(BankService bank) {
        var accounts = listAllAccounts(bank);
        if (accounts.isEmpty()) return;
        System.out.print("Choose account number to UNFREEZE: ");
        int idx = Integer.parseInt(in.nextLine().trim()) - 1;
        Account a = accounts.get(idx);
        bank.unfreezeAccount(a.getId());
        System.out.println("Account unfrozen.");
    }

    private static void viewAnyAccountTransactions(BankService bank) {
        var accounts = listAllAccounts(bank);
        if (accounts.isEmpty()) return;
        System.out.print("Choose account number to view transactions: ");
        int idx = Integer.parseInt(in.nextLine().trim()) - 1;
        Account a = accounts.get(idx);
        var txList = bank.getTransactions(a.getId());
        if (txList.isEmpty()) {
            System.out.println("No transactions for this account yet.");
            return;
        }
        System.out.println("\nTransactions:");
        txList.forEach(t -> System.out.printf(
                "%s | %-14s | %s | %s%n",
                t.getTimestamp(), t.getType(), t.getAmount(), t.getNote()
        ));
    }

    // CUSTOMER HELPERS

    private static void openAccount(BankService bank, String userId) {
        String accountId = bank.openCheckingAccount(userId);
        System.out.println("Opened new account with ID: " + accountId);
        listAccounts(bank, userId);
    }

    private static List<Account> listAccounts(BankService bank, String userId) {
        List<Account> accounts = bank.getAccountsForUser(userId);
        if (accounts.isEmpty()) {
            System.out.println("You have no accounts yet.");
            return accounts;
        }
        System.out.println("\nYour accounts:");
        for (int i = 0; i < accounts.size(); i++) {
            Account a = accounts.get(i);
            System.out.printf("%d) %s | Balance: %s | Frozen=%s%n",
                    i + 1, a.getAccountNumber(), a.getBalance(), a.isFrozen());
        }
        return accounts;
    }

    private static Account chooseAccount(BankService bank, String userId, String prompt) {
        List<Account> accounts = listAccounts(bank, userId);
        if (accounts.isEmpty()) throw new IllegalStateException("No accounts to choose from.");

        System.out.print(prompt + " (enter number): ");
        int idx = Integer.parseInt(in.nextLine().trim()) - 1;
        if (idx < 0 || idx >= accounts.size()) throw new IllegalArgumentException("Invalid account choice.");
        return accounts.get(idx);
    }

    private static BigDecimal readAmount(String label) {
        System.out.print(label + " amount: ");
        String input = in.nextLine().trim();
        return new BigDecimal(input);
    }

    private static void deposit(BankService bank, String userId) {
        Account acct = chooseAccount(bank, userId, "Choose account to deposit into");
        BigDecimal amount = readAmount("Deposit");
        System.out.print("Note (optional): ");
        String note = in.nextLine().trim();
        bank.deposit(acct.getId(), amount, note);
        System.out.println("Deposit successful. New balance: " + bank.getBalance(acct.getId()));
    }

    private static void withdraw(BankService bank, String userId) {
        Account acct = chooseAccount(bank, userId, "Choose account to withdraw from");
        BigDecimal amount = readAmount("Withdraw");
        System.out.print("Note (optional): ");
        String note = in.nextLine().trim();
        bank.withdraw(acct.getId(), amount, note);
        System.out.println("Withdrawal successful. New balance: " + bank.getBalance(acct.getId()));
    }

    private static void transfer(BankService bank, String userId) {
        System.out.println("Choose FROM account:");
        Account from = chooseAccount(bank, userId, "From");
        System.out.println("Choose TO account:");
        Account to = chooseAccount(bank, userId, "To");

        if (from.getId().equals(to.getId())) {
            System.out.println("Cannot transfer to the same account.");
            return;
        }

        BigDecimal amount = readAmount("Transfer");
        System.out.print("Note (optional): ");
        String note = in.nextLine().trim();
        bank.transfer(from.getId(), to.getId(), amount, note);
        System.out.println("Transfer complete.");
        System.out.println("From balance: " + bank.getBalance(from.getId()));
        System.out.println("To balance:   " + bank.getBalance(to.getId()));
    }

    private static void viewTransactions(BankService bank, String userId) {
        Account acct = chooseAccount(bank, userId, "Choose account to view transactions");
        var txList = bank.getTransactions(acct.getId());
        if (txList.isEmpty()) {
            System.out.println("No transactions for this account yet.");
            return;
        }
        System.out.println("\nTransactions:");
        txList.forEach(t -> System.out.printf(
                "%s | %-14s | %s | %s%n",
                t.getTimestamp(), t.getType(), t.getAmount(), t.getNote()
        ));
    }
}
