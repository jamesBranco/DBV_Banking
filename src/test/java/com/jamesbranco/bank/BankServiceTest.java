package com.jamesbranco.bank;

import com.jamesbranco.bank.model.Role;
import com.jamesbranco.bank.repo.InMemoryStore;
import com.jamesbranco.bank.service.BankService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class BankServiceTest {
    @Test
    void depositWithdrawTransferFlow() {
        var svc = new BankService(new InMemoryStore());
        var userId = svc.registerUser("Test", "t@e.com", "pw", Role.CUSTOMER);
        var a1 = svc.openCheckingAccount(userId);
        var a2 = svc.openCheckingAccount(userId);

        svc.deposit(a1, new BigDecimal("200.00"), "seed");
        svc.withdraw(a1, new BigDecimal("50.00"), "atm");
        svc.transfer(a1, a2, new BigDecimal("25.00"), "move");

        assertEquals(new BigDecimal("125.00"), svc.getBalance(a1));
        assertEquals(new BigDecimal("25.00"), svc.getBalance(a2));
        assertEquals(3, svc.getTransactions(a1).size()); // deposit, withdraw, transfer_out
        assertEquals(1, svc.getTransactions(a2).size()); // transfer_in
    }
}
