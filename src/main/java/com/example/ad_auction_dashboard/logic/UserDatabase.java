package com.example.ad_auction_dashboard.logic;

import java.sql.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UserDatabase {
    // Database configuration
    private static final String DB_FOLDER = System.getProperty("user.home") + File.separator + ".ad_auction_dashboard";
    private static final String DB_NAME = "userdb";
    private static final String URL = "jdbc:h2:file:" + DB_FOLDER + File.separator + DB_NAME;
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    static {
        try {
            // Make sure database directory exists
            new File(DB_FOLDER).mkdirs();

            // Check if database already exists
            File dbFile = new File(DB_FOLDER + File.separator + DB_NAME + ".mv.db");

            // Print the absolute path for the database file
            System.out.println("Database location: " + dbFile.getAbsolutePath());

            // Load H2 driver
            Class.forName("org.h2.Driver");

            // Initialize database schema and admin user if needed
            initDatabase();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("H2 JDBC Driver not found", e);
        }
    }

    /**
     * Initializes the database schema if it doesn't exist yet
     */
    private static void initDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Create the users table if it doesn't exist
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS users (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "username VARCHAR(50) NOT NULL UNIQUE, " +
                    "email VARCHAR(255) NOT NULL UNIQUE, " +
                    "phone VARCHAR(20) NOT NULL UNIQUE, " +
                    "password VARCHAR(255) NOT NULL, " +
                    "role VARCHAR(10) NOT NULL DEFAULT 'user'" +
                    ")"
            );

            // Check if admin user exists, create if not
            ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) FROM users WHERE username = 'admin'"
            );

            if (rs.next() && rs.getInt(1) == 0) {
                // Create default admin user with required credentials
                stmt.execute(
                    "INSERT INTO users (username, email, phone, password, role) " +
                        "VALUES ('admin', 'admin@example.com', '1234567890', 'admin123', 'admin')"
                );
                System.out.println("Default admin user created with username 'admin' and password 'admin123'");
            }

            // List all tables for debugging
            System.out.println("Tables in the database:");
            ResultSet tables = stmt.executeQuery("SHOW TABLES");
            while (tables.next()) {
                System.out.println("- " + tables.getString(1));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper method to get a database connection
     */
    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void addUser(String username, String email, String phone, String password, String role) {
        String sql = "INSERT INTO users (username, email, phone, password, role) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, phone);
            stmt.setString(4, password);
            stmt.setString(5, role);
            stmt.executeUpdate();
            System.out.println("User added: " + username);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void deleteUser(int id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateUser(int id, String username, String email, String phone, String password, String role) {
        String sql = "UPDATE users SET username = ?, email = ?, phone = ?, password = ?, role = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, phone);
            stmt.setString(4, password);
            stmt.setString(5, role);
            stmt.setInt(6, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean authenticateUser(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            return rs.next(); // Returns true if user exists
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getUserRole(String username) {
        String sql = "SELECT role FROM users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
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

    public static User getUser(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new User(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("email"),
                    rs.getString("phone"),
                    rs.getString("role")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Utility method to list all users in the database
     */
    public static void listAllUsers() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM users");
            System.out.println("\nUsers in database:");
            while (rs.next()) {
                System.out.println(rs.getString("username") + " - " + rs.getString("role"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Test method to check database setup
    public static void main(String[] args) {
        System.out.println("Database URL: " + URL);

        // Initialize database
        initDatabase();

        // List all users
        listAllUsers();

        System.out.println("Database test completed.");
    }

    // User class to represent user data
    public static class User {
        private int id;
        private String username;
        private String email;
        private String phone;
        private String role;

        public User(int id, String username, String email, String phone, String role) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.phone = phone;
            this.role = role;
        }

        // Getters
        public int getId() { return id; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
        public String getRole() { return role; }
    }
}