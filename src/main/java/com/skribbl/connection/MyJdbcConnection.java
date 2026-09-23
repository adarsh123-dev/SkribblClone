package com.skribbl.connection;

import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Simple JDBC connection helper that works BOTH locally and when deployed.
 *
 * It supports two ways of configuring the connection:
 *
 *  1. A single full URL via the MYSQL_PUBLIC_URL (or MYSQL_URL) env var,
 *     in the form: mysql://user:password@host:port/database
 *     This is exactly what Railway's "Public Network" MySQL connect panel
 *     gives you — just set one Railway variable:
 *         MYSQL_PUBLIC_URL = ${{MySQL.MYSQL_PUBLIC_URL}}
 *     and this class parses it automatically.
 *
 *  2. Individual DB_HOST / DB_PORT / DB_NAME / DB_USER / DB_PASSWORD env
 *     vars, used only if MYSQL_PUBLIC_URL/MYSQL_URL isn't set.
 *
 * If NEITHER is set (e.g. running locally in your IDE with nothing
 * configured), it falls back to a local MySQL on localhost with
 * user/pass "root"/"root", so local dev still works with zero setup.
 */
public class MyJdbcConnection {

    private static final String URL;
    private static final String DB_USER;
    private static final String DB_PASSWORD;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found", e);
        }

        // Prefer a single full connection URL if Railway (or anything else) provides one.
        String fullUrl = firstNonEmpty(System.getenv("MYSQL_PUBLIC_URL"), System.getenv("MYSQL_URL"));

        if (fullUrl != null) {
            // fullUrl looks like: mysql://user:password@host:port/database
            URI uri = URI.create(fullUrl);
            String userInfo = uri.getUserInfo(); // "user:password"
            String[] parts = userInfo.split(":", 2);
            DB_USER = parts[0];
            DB_PASSWORD = parts.length > 1 ? parts[1] : "";
            String host = uri.getHost();
            int port = uri.getPort() != -1 ? uri.getPort() : 3306;
            String dbName = uri.getPath().startsWith("/") ? uri.getPath().substring(1) : uri.getPath();
            URL = "jdbc:mysql://" + host + ":" + port + "/" + dbName
                    + "?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        } else {
            // Fall back to individual vars (or localhost/root/root for local dev).
            String dbHost = getEnv("DB_HOST", "localhost");
            String dbPort = getEnv("DB_PORT", "3306");
            String dbName = getEnv("DB_NAME", "skribbl");
            DB_USER = getEnv("DB_USER", "root");
            DB_PASSWORD = getEnv("DB_PASSWORD", "root");
            URL = "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName
                    + "?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        }
    }

    private static String getEnv(String key, String fallback) {
        String value = System.getenv(key);
        return (value != null && !value.isEmpty()) ? value : fallback;
    }

    private static String firstNonEmpty(String... values) {
        for (String v : values) {
            if (v != null && !v.isEmpty()) return v;
        }
        return null;
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, DB_USER, DB_PASSWORD);
    }
}
