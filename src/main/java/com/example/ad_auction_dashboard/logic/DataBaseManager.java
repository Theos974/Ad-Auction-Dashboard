package com.example.ad_auction_dashboard.logic;

import java.io.File;
import java.net.URL;
import java.sql.*;

public class DataBaseManager {
    // Database connection string
    private static String dbUrl = null;

    // Initialize the database connection URL
    static {
        try {
            // Get the resource URL
            URL resourceUrl = DataBaseManager.class.getClassLoader().getResource("userdata.db");

            if (resourceUrl != null) {
                // Convert URL to file path for SQLite
                String dbPath = new File(resourceUrl.toURI()).getAbsolutePath();
                dbUrl = "jdbc:sqlite:" + dbPath;
            } else {
                // Fall back to a relative path if the resource is not found
                dbUrl = "jdbc:sqlite:userdata.db";
                System.err.println("Warning: Database resource not found, using fallback path.");
            }
        } catch (Exception e) {
            System.err.println("Error initializing database URL: " + e.getMessage());
            // Fall back to a relative path if there's an exception
            dbUrl = "jdbc:sqlite:userdata.db";
        }
    }
    // Get database connection
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }
    // Authenticate a user
    public static boolean authenticateUser(String username, String password) {
        String query = "SELECT * FROM users WHERE username = ? AND password = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            stmt.setString(2, password); // In a real app, you'd hash passwords

            ResultSet rs = stmt.executeQuery();
            return rs.next(); // Return true if user exists

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Get user role
    public static String getUserRole(String username) {
        String query = "SELECT role FROM users WHERE username = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("role");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }
}