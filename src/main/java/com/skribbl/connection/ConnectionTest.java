package com.skribbl.connection;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * Standalone sanity check for the JDBC connection — run this BEFORE deploying
 * to Tomcat, so you know your MySQL URL/user/password are correct and the
 * mysql-connector jar is on the classpath.
 *
 * How to run (from project root, after `mvn compile`):
 *   java -cp "target/classes:$(find ~/.m2 -name 'mysql-connector-j-*.jar' | head -1)" com.skribbl.connection.ConnectionTest
 *
 * Or just run the main() method directly from your IDE (Eclipse/IntelliJ) —
 * that's usually the easiest way for this assignment.
 */
public class ConnectionTest {

    public static void main(String[] args) {
        System.out.println("Trying to connect to MySQL...");

        try (Connection con = MyJdbcConnection.getConnection()) {
            if (con != null && !con.isClosed()) {
                DatabaseMetaData meta = con.getMetaData();
                System.out.println("SUCCESS! Connected to database.");
                System.out.println("  URL     : " + meta.getURL());
                System.out.println("  User    : " + meta.getUserName());
                System.out.println("  Driver  : " + meta.getDriverName() + " " + meta.getDriverVersion());
                System.out.println("  DB      : " + meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion());
            } else {
                System.out.println("FAILED: got a null/closed connection.");
            }
        } catch (SQLException e) {
            System.out.println("FAILED to connect to the database.");
            System.out.println("Reason: " + e.getMessage());
            System.out.println();
            System.out.println("Checklist:");
            System.out.println("  1. Is MySQL running? (check with `mysql -u root -p` on the command line)");
            System.out.println("  2. Did you run sql/schema.sql to create the 'skribbl' database?");
            System.out.println("  3. Are DB_USER / DB_PASSWORD in MyJdbcConnection.java correct?");
            System.out.println("  4. Is the mysql-connector-j jar on your classpath?");
            e.printStackTrace();
        }
    }
}
