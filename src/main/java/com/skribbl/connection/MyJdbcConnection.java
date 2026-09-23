package com.skribbl.connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyJdbcConnection {

    private static final String DB_HOST =
            getEnv("DB_HOST", "localhost");

    private static final String DB_PORT =
            getEnv("DB_PORT", "3306");

    private static final String DB_NAME =
            getEnv("DB_NAME", "skribbl");

    private static final String DB_USER =
            getEnv("DB_USER", "root");

    private static final String DB_PASSWORD =
            getEnv("DB_PASSWORD", "root");

    private static final String URL =
            "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME
            + "?useSSL=true"
            + "&serverTimezone=UTC"
            + "&allowPublicKeyRetrieval=true";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                "MySQL JDBC Driver not found", e
            );
        }
    }

    private static String getEnv(String key, String fallback) {
        String value = System.getenv(key);

        return (value != null && !value.isEmpty())
                ? value
                : fallback;
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                URL,
                DB_USER,
                DB_PASSWORD
        );
    }
}