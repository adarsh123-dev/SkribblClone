package com.skribbl.connection;

public class ConnectionTest {

    public static void main(String[] args) {

        if (MyJdbcConnection.getConnection() != null) {
            System.out.println("Database Connected Successfully");
        }
    }
}