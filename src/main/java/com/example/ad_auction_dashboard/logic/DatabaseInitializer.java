package com.example.ad_auction_dashboard.logic;

import java.nio.file.*;
import java.sql.*;

public class DatabaseInitializer {
    // Default admin credentials
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_EMAIL = "admin@example.com";
    private static final String DEFAULT_ADMIN_PHONE = "1234567890";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin123";

    public static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(
            "jdbc:mysql://localhost:3306",
            "root",
            "COMP2211");
             Statement stmt = conn.createStatement()) {

            // Check if database exists
            ResultSet rs = stmt.executeQuery("SHOW DATABASES LIKE 'ad_auction_user'");
            if (!rs.next()) {
                // Database does not exist, so create it
                stmt.executeUpdate("CREATE DATABASE ad_auction_user");
                System.out.println("Database created successfully.");

                // Now create and initialize the database with the schema
                String sqlFile = "src/main/resources/database/user_management.sql";
                String sql = new String(Files.readAllBytes(Paths.get(sqlFile)));

                // Use the newly created database
                stmt.executeUpdate("USE ad_auction_user");
                stmt.executeUpdate(sql);
                System.out.println("Database schema initialized successfully.");

                // Add default admin user
                addDefaultAdmin();
            } else {
                System.out.println("Database already exists. Skipping initialization.");
            }

        } catch (Exception e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Adds a default admin user to the database if no admin exists yet
     */
    private static void addDefaultAdmin() {
        try (Connection conn = DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/ad_auction_user",
            "root",
            "COMP2211");
             Statement stmt = conn.createStatement()) {

            // Check if admin already exists
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE username = '" + DEFAULT_ADMIN_USERNAME + "'");
            if (rs.next() && rs.getInt(1) == 0) {
                // Admin doesn't exist, add it
                PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO users (username, email, phone, password, role) VALUES (?, ?, ?, ?, 'admin')");
                pstmt.setString(1, DEFAULT_ADMIN_USERNAME);
                pstmt.setString(2, DEFAULT_ADMIN_EMAIL);
                pstmt.setString(3, DEFAULT_ADMIN_PHONE);
                pstmt.setString(4, DEFAULT_ADMIN_PASSWORD);
                pstmt.executeUpdate();

                System.out.println("Default admin account created successfully:");
                System.out.println("Username: " + DEFAULT_ADMIN_USERNAME);
                System.out.println("Password: " + DEFAULT_ADMIN_PASSWORD);
            } else {
                System.out.println("Default admin already exists.");
            }
        } catch (Exception e) {
            System.err.println("Error adding default admin: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Utility method to verify database structure and check if it's properly set up
     */
    public static void verifyDatabaseStructure() {
        try (Connection conn = DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/ad_auction_user",
            "root",
            "COMP2211");
             Statement stmt = conn.createStatement()) {

            // Get table structure
            ResultSet rs = stmt.executeQuery("DESCRIBE users");
            System.out.println("Current database structure:");
            System.out.println("=========================");

            while (rs.next()) {
                System.out.println(rs.getString("Field") + " - " + rs.getString("Type"));
            }
            System.out.println("=========================");

        } catch (Exception e) {
            System.err.println("Error verifying database structure: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Initialize the database only if it doesn't exist
        initializeDatabase();

        // Verify the database structure
        verifyDatabaseStructure();
    }
}