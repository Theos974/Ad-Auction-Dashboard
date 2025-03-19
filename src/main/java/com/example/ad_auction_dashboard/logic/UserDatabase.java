package com.example.ad_auction_dashboard.logic;

import java.sql.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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

            // Create the users table if it doesn't exist - NOTICE: Table name is USERS (uppercase)
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS USERS (" +
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
                "SELECT COUNT(*) FROM USERS WHERE username = 'admin'"
            );

            if (rs.next() && rs.getInt(1) == 0) {
                // Create default admin user with required credentials
                stmt.execute(
                    "INSERT INTO USERS (username, email, phone, password, role) " +
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
        String sql = "INSERT INTO USERS (username, email, phone, password, role) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, phone);
            stmt.setString(4, password);
            stmt.setString(5, role);
            stmt.executeUpdate();
            System.out.println("User added: " + username);

            // If the user is an admin, grant them access to all campaigns
            if ("admin".equals(role)) {
                int userId = getUser(username).getId();
                // We'll pass the userId itself as the grantedBy parameter for simplicity
                CampaignDatabase.grantNewAdminAccessToAllCampaigns(userId, userId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean isUserUnique(String username, String email, String phone){
        try (Connection conn = getConnection();
        Statement stmt = conn.createStatement())   {
            ResultSet all = stmt.executeQuery("SELECT username, email, phone FROM USERS");
            while (all.next()){
                if (Objects.equals(all.getString(1), username) || Objects.equals(all.getString(2), email) || Objects.equals(all.getString(3), phone)){
                    return false;
                }
            }
        } catch (Exception e){System.err.println(e);}
        return true;
    }

    public static void deleteUser(int id) {
        String sql = "DELETE FROM USERS WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateUser(int id, String username, String email, String phone, String password, String role) {
        String sql = "UPDATE USERS SET username = ?, email = ?, phone = ?, password = ?, role = ? WHERE id = ?";

        // Capture the old role to check if it's being changed to admin
        String oldRole = null;
        try (Connection conn = getConnection();
             PreparedStatement checkStmt = conn.prepareStatement("SELECT role FROM USERS WHERE id = ?")) {
            checkStmt.setInt(1, id);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                oldRole = rs.getString("role");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, phone);
            stmt.setString(4, password);
            stmt.setString(5, role);
            stmt.setInt(6, id);
            stmt.executeUpdate();

            // If the role is being changed to admin, grant access to all campaigns
            if ("admin".equals(role) && !"admin".equals(oldRole)) {
                // Use the first admin in the system or the user itself as the granter
                int granterId = getFirstAdminId();
                if (granterId == -1) {
                    granterId = id; // If no admin found, use the user itself
                }
                CampaignDatabase.grantNewAdminAccessToAllCampaigns(id, granterId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the ID of the first admin user in the system
     * @return First admin ID or -1 if none found
     */
    private static int getFirstAdminId() {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT id FROM USERS WHERE role = 'admin' LIMIT 1")) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static boolean authenticateUser(String username, String password) {
        String sql = "SELECT * FROM USERS WHERE username = ? AND password = ?";
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
        String sql = "SELECT role FROM USERS WHERE username = ?";
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
        String sql = "SELECT * FROM USERS WHERE username = ?";
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
                    rs.getString("role"),
                    rs.getString("password")
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
            ResultSet rs = stmt.executeQuery("SELECT * FROM USERS");
            System.out.println("\nUsers in database:");
            while (rs.next()) {
                System.out.println(rs.getString("username") + " - " + rs.getString("role"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Add to UserDatabase class
    public static boolean changeUserRole(String username, String newRole) {
        // First get the current role
        String oldRole = getUserRole(username);

        // Update the role
        String sql = "UPDATE USERS SET role = ? WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newRole);
            stmt.setString(2, username);

            boolean updated = stmt.executeUpdate() > 0;

            if (updated && "admin".equals(newRole) && !"admin".equals(oldRole)) {
                // User is now an admin, grant access to all campaigns
                User user = getUser(username);
                if (user != null) {
                    // Find an admin to assign as granter or use this user
                    int adminGranterId = getFirstAdminId();
                    if (adminGranterId == -1 || adminGranterId == user.getId()) {
                        adminGranterId = user.getId(); // If no other admin found, use the user itself
                    }
                    CampaignDatabase.grantNewAdminAccessToAllCampaigns(user.getId(), adminGranterId);
                }
            }

            return updated;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM USERS")) {

            while (rs.next()) {
                users.add(new User(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("email"),
                    rs.getString("phone"),
                    rs.getString("role"),
                    rs.getString("password")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
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
        private String password;

        public User(int id, String username, String email, String phone, String role, String password) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.phone = phone;
            this.role = role;
            this.password = password;
        }

        // Getters
        public int getId() { return id; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
        public String getRole() { return role; }
        public String getPassword() { return password; }

        public boolean isAdmin() {
            return "admin".equals(role);
        }

        public boolean isEditor() {
            return "admin".equals(role) || "editor".equals(role);
        }

        public boolean isViewer() {
            return true; // Everyone has at least viewer permissions
        }
    }
}