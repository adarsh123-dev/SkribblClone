package com.skribbl.connection;

import java.sql.Connection;
import java.sql.DriverManager;

public class MyJdbcConnection {

    private static final String URL =
            "jdbc:mysql://localhost:3306/skribbl?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "root";

    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (Exception e) {
            throw new RuntimeException("Database connection failed", e);
        }
    }
}
