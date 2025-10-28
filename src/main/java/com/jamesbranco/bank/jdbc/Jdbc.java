package com.jamesbranco.bank.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class Jdbc {
    private static final Properties PROPS = new Properties();
    static {
        try (InputStream in = Jdbc.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) PROPS.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load application.properties", e);
        }
    }
    private Jdbc() {}

    public static Connection getConnection() throws SQLException {
        String url = PROPS.getProperty("db.url");
        String user = PROPS.getProperty("db.user");
        String pass = PROPS.getProperty("db.password");
        if (url == null) throw new IllegalStateException("db.url not set");
        return DriverManager.getConnection(url, user, pass);
    }
}
