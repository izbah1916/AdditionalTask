package org.example.db;

import org.example.config.Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Db {
    static {
        try { Class.forName("org.postgresql.Driver"); } catch (ClassNotFoundException ignored) {}
    }
    public static Connection get() throws SQLException {
        return DriverManager.getConnection(Config.DB_URL, Config.DB_USER, Config.DB_PASS);
    }
    private Db() {}
}
