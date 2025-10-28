package com.jamesbranco.bank.util;

import java.util.UUID;

public final class IdGenerator {
    private IdGenerator() {}
    public static String newId() { return UUID.randomUUID().toString(); }
    public static String newAccountNumber() {
        return String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits()))
                .substring(0, 12);
    }
}
