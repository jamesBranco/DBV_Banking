package com.jamesbranco.bank.repo;

import com.jamesbranco.bank.model.Account;
import com.jamesbranco.bank.model.Transaction;

import java.util.*;

public class InMemoryStore {

    public static final class UserRecord {
        public final String id, name, email, passwordHash, role;

        public UserRecord(String id, String name, String email, String passwordHash, String role) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.passwordHash = passwordHash;
            this.role = role;
        }
    }

    private final Map<String, UserRecord> usersById = new HashMap<>();
    private final Map<String, UserRecord> usersByEmail = new HashMap<>();
    private final Map<String, Account> accountsById = new HashMap<>();
    private final Map<String, List<Transaction>> txByAccountId = new HashMap<>();

    public Optional<UserRecord> findUserByEmail(String email) {
        return Optional.ofNullable(usersByEmail.get(email));
    }

    public Optional<UserRecord> findUserById(String id) {
        return Optional.ofNullable(usersById.get(id));
    }

    public void saveUser(UserRecord u) {
        usersById.put(u.id, u);
        usersByEmail.put(u.email, u);
    }

    public void saveAccount(Account a) {
        accountsById.put(a.getId(), a);
    }

    public Optional<Account> findAccountById(String id) {
        return Optional.ofNullable(accountsById.get(id));
    }

    public List<Account> findAccountsByUserId(String userId) {
        List<Account> out = new ArrayList<>();
        for (Account a : accountsById.values()) {
            if (a.getOwnerUserId().equals(userId)) {
                out.add(a);
            }
        }
        return out;
    }

    public void appendTransaction(Transaction t) {
        txByAccountId.computeIfAbsent(t.getAccountId(), k -> new ArrayList<>()).add(t);
    }

    public List<Transaction> getTransactions(String accountId) {
        return Collections.unmodifiableList(txByAccountId.getOrDefault(accountId, List.of()));
    }

    // NEW – used by admin features
    public Collection<UserRecord> findAllUsers() {
        return Collections.unmodifiableCollection(usersById.values());
    }

    // NEW – used by admin features
    public Collection<Account> findAllAccounts() {
        return Collections.unmodifiableCollection(accountsById.values());
    }
}
