package com.example.ad_auction_dashboard.logic;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/**
 * This class manages campaign database operations.
 * It handles saving campaigns to a local database and loading them back.
 */
public class CampaignDatabase {

    // Database folder path
    private static final String DB_FOLDER = System.getProperty("user.home") + File.separator + ".ad_auction_dashboard";
    private static final String DB_NAME = "userdb";
    private static final String URL = "jdbc:h2:file:" + DB_FOLDER + File.separator + DB_NAME;
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    // Flag to track if database is initialized
    private static boolean databaseInitialized = false;

    private static final int BATCH_SIZE = 1500000;
    private static final int THREAD_POOL_SIZE = 3; // One for each log type
    /**
     * Initialize database folder and ensure it exists
     */
    public static void initDatabaseFolder() {
        try {
            Files.createDirectories(Paths.get(DB_FOLDER));
        } catch (Exception e) {
            System.err.println("Error creating database folder: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get H2 database connection
     */
    private static Connection getConnection() throws SQLException {
        try {
            // Load H2 driver
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("H2 JDBC driver not found: " + e.getMessage());
            throw new SQLException("H2 JDBC driver not found", e);
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * Ensures the database is initialized - creates tables if they don't exist
     * Call this before any database operation
     */
    public static boolean ensureDatabaseInitialized() {
        if (databaseInitialized) {
            return true;
        }

        initDatabaseFolder();

        try (Connection conn = getConnection()) {
            createTablesIfNotExist(conn);



            databaseInitialized = true;
            return true;
        } catch (SQLException e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Save a campaign to the database and assign access to the creator
     *
     * @param campaignMetrics The campaign to save
     * @param campaignName The name of the campaign
     * @param userId The ID of the user saving the campaign
     * @return The ID of the saved campaign, or -1 if an error occurred
     */
    public static int saveCampaign(CampaignMetrics campaignMetrics, String campaignName, int userId) {
        if (!ensureDatabaseInitialized()) {
            return -1;
        }

        try (Connection conn = getConnection()) {
            // Begin transaction
            conn.setAutoCommit(false);

            try {
                // Insert campaign record
                int campaignId = insertCampaignRecord(conn, campaignMetrics, campaignName, userId);

                savePrecomputedMetrics(conn, campaignId, campaignMetrics);

                // Insert log data
                insertImpressionLogs(conn, campaignMetrics.getImpressionLogs(), campaignId);
                insertClickLogs(conn, campaignMetrics.getClickLogs(), campaignId);
                insertServerLogs(conn, campaignMetrics.getServerLogs(), campaignId);

                // Grant access to the user who created the campaign
                assignCampaignToUserInternal(conn, campaignId, userId, userId);

                // Grant access to all admins
                grantAccessToAllAdmins(conn, campaignId, userId);

                // Commit transaction
                conn.commit();

                System.out.println("Campaign saved successfully with ID: " + campaignId);
                return campaignId;
            } catch (SQLException e) {
                // Rollback on error
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error rolling back transaction: " + ex.getMessage());
                }
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Error saving campaign: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Grant access to a campaign to all admin users
     *
     * @param conn Database connection
     * @param campaignId The campaign ID
     * @param assignedBy The user ID of the user granting access
     * @throws SQLException If a database error occurs
     */
    private static void grantAccessToAllAdmins(Connection conn, int campaignId, int assignedBy) throws SQLException {
        // First, get all admin users - Using correct case "USERS" instead of "users"
        try (PreparedStatement stmt = conn.prepareStatement(
            "SELECT id FROM USERS WHERE role = 'admin' AND id != ?")) {

            stmt.setInt(1, assignedBy); // Exclude the user who created the campaign (they already have access)

            try (ResultSet rs = stmt.executeQuery()) {
                // Grant access to each admin
                while (rs.next()) {
                    int adminId = rs.getInt("id");
                    assignCampaignToUserInternal(conn, campaignId, adminId, assignedBy);
                }
            }
        }
    }

    /**
     * Grant a new admin access to all existing campaigns
     *
     * @param adminId The ID of the new admin user
     * @param grantedBy The ID of the user granting access (usually another admin)
     * @return True if successful, false otherwise
     */
    public static boolean grantNewAdminAccessToAllCampaigns(int adminId, int grantedBy) {
        if (!ensureDatabaseInitialized()) {
            return false;
        }

        try (Connection conn = getConnection()) {
            // Get all campaigns
            try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT campaign_id FROM Campaigns")) {

                try (ResultSet rs = stmt.executeQuery()) {
                    // Grant access to each campaign
                    while (rs.next()) {
                        int campaignId = rs.getInt("campaign_id");
                        assignCampaignToUserInternal(conn, campaignId, adminId, grantedBy);
                    }
                }
            }

            return true;
        } catch (SQLException e) {
            System.err.println("Error granting admin access to campaigns: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Load a campaign from the database with optimized parallel loading
     */
    public static Campaign loadCampaign(int campaignId) {
        initDatabaseFolder();

        try (Connection conn = getConnection()) {
            // Create tables if they don't exist
            createTablesIfNotExist(conn);

            // Load campaign properties
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT bounce_pages_threshold, bounce_seconds_threshold FROM Campaigns WHERE campaign_id = ?"
            );
            stmt.setInt(1, campaignId);

            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                System.err.println("Campaign not found with ID: " + campaignId);
                return null;
            }

            int bouncePagesThreshold = rs.getInt("bounce_pages_threshold");
            int bounceSecondsThreshold = rs.getInt("bounce_seconds_threshold");

            // Load logs
            ImpressionLog[] impressionLogs = loadImpressionLogs(conn, campaignId);
            ClickLog[] clickLogs = loadClickLogs(conn, campaignId);
            ServerLog[] serverLogs = loadServerLogs(conn, campaignId);

            // Create and return Campaign object
            Campaign campaign = new Campaign(impressionLogs, clickLogs, serverLogs);
            System.out.println("Campaign loaded successfully with ID: " + campaignId);
            return campaign;
        } catch (Exception e) {
            System.err.println("Error loading campaign: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    /**
     * Check if a campaign exists and get its properties
     */
    private static boolean checkCampaignExists(int campaignId) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT 1 FROM Campaigns WHERE campaign_id = ?")) {
            stmt.setInt(1, campaignId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Error checking campaign: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get the count of logs for a specific type
     */
    private static int getLogCount(String tableName, int campaignId) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM " + tableName + " WHERE campaign_id = ?")) {
            stmt.setInt(1, campaignId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            System.err.println("Error counting logs: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }


    /**
     * Gets a campaign's metadata by ID
     */
    public static CampaignInfo getCampaignById(int campaignId) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT campaign_name, start_date, end_date, bounce_pages_threshold, bounce_seconds_threshold " +
                     "FROM Campaigns WHERE campaign_id = ?")) {

            stmt.setInt(1, campaignId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    CampaignInfo info = new CampaignInfo(
                        campaignId,
                        rs.getString("campaign_name"),
                        null, // creation_date
                        rs.getTimestamp("start_date") != null ?
                            rs.getTimestamp("start_date").toLocalDateTime() : null,
                        rs.getTimestamp("end_date") != null ?
                            rs.getTimestamp("end_date").toLocalDateTime() : null,
                        0 // userId (not needed for this purpose)
                    );

                    // Set bounce thresholds
                    info.setBouncePagesThreshold(rs.getInt("bounce_pages_threshold"));
                    info.setBounceSecondsThreshold(rs.getInt("bounce_seconds_threshold"));

                    return info;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Get a list of all saved campaigns
     *
     * @return A list of CampaignInfo objects containing campaign metadata
     */
    public static List<CampaignInfo> getAllCampaigns() {
        if (!ensureDatabaseInitialized()) {
            return new ArrayList<>();
        }

        List<CampaignInfo> campaigns = new ArrayList<>();

        try (Connection conn = getConnection()) {
            // Query for all campaigns
            try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT campaign_id, campaign_name, creation_date, start_date, end_date, user_id FROM Campaigns")) {

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        CampaignInfo campaign = new CampaignInfo(
                            rs.getInt("campaign_id"),
                            rs.getString("campaign_name"),
                            rs.getTimestamp("creation_date") != null ?
                                rs.getTimestamp("creation_date").toLocalDateTime() : null,
                            rs.getTimestamp("start_date") != null ?
                                rs.getTimestamp("start_date").toLocalDateTime() : null,
                            rs.getTimestamp("end_date") != null ?
                                rs.getTimestamp("end_date").toLocalDateTime() : null,
                            rs.getInt("user_id")
                        );
                        campaigns.add(campaign);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching campaigns: " + e.getMessage());
            e.printStackTrace();
        }

        return campaigns;
    }

    /**
     * Get campaigns for a specific user
     *
     * @param userId The user's ID
     * @return A list of CampaignInfo objects for that user
     */
    public static List<CampaignInfo> getUserCampaigns(int userId) {
        if (!ensureDatabaseInitialized()) {
            return new ArrayList<>();
        }

        List<CampaignInfo> campaigns = new ArrayList<>();

        try (Connection conn = getConnection()) {
            // Query for user's campaigns
            try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT campaign_id, campaign_name, creation_date, start_date, end_date, user_id " +
                    "FROM Campaigns WHERE user_id = ?")) {

                stmt.setInt(1, userId);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        CampaignInfo campaign = new CampaignInfo(
                            rs.getInt("campaign_id"),
                            rs.getString("campaign_name"),
                            rs.getTimestamp("creation_date") != null ?
                                rs.getTimestamp("creation_date").toLocalDateTime() : null,
                            rs.getTimestamp("start_date") != null ?
                                rs.getTimestamp("start_date").toLocalDateTime() : null,
                            rs.getTimestamp("end_date") != null ?
                                rs.getTimestamp("end_date").toLocalDateTime() : null,
                            rs.getInt("user_id")
                        );
                        campaigns.add(campaign);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching user campaigns: " + e.getMessage());
            e.printStackTrace();
        }

        return campaigns;
    }

    /**
     * Delete a campaign from the database with improved performance and error handling
     *
     * @param campaignId The ID of the campaign to delete
     * @return true if the campaign was deleted successfully, false otherwise
     */
    public static boolean deleteCampaign(int campaignId) {
        if (!ensureDatabaseInitialized()) {
            return false;
        }

        long startTime = System.currentTimeMillis(); // For performance tracking
        int[] deleteStats = new int[5]; // To track counts of deleted rows

        try (Connection conn = getConnection()) {
            // Set a more appropriate isolation level for bulk deletes
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

            // Begin transaction
            conn.setAutoCommit(false);

            try {
                // 1. First, delete the campaign metrics (from our added table)
                try (PreparedStatement deleteCampaignMetrics = conn.prepareStatement(
                    "DELETE FROM CampaignMetrics WHERE campaign_id = ?")) {
                    deleteCampaignMetrics.setInt(1, campaignId);
                    deleteStats[0] = deleteCampaignMetrics.executeUpdate();
                    System.out.println("Deleted " + deleteStats[0] + " campaign metrics records");
                }

                // 2. Delete all detail records (foreign key dependencies)
                // Check counts first for better logging
                int impressionCount = getLogCount("ImpressionLogs", campaignId);
                int clickCount = getLogCount("ClickLogs", campaignId);
                int serverLogCount = getLogCount("ServerLogs", campaignId);

                System.out.println("Deleting campaign #" + campaignId +
                    " with " + impressionCount + " impressions, " +
                    clickCount + " clicks, " + serverLogCount + " server logs");

                // Use a larger batch size for better performance with many records
                if (impressionCount > 10000 || clickCount > 10000 || serverLogCount > 10000) {
                    conn.setNetworkTimeout(null, 120000); // Increase timeout for large deletions
                }

                // Delete in reverse dependency order (child tables first)
                try (PreparedStatement deleteServerLogs = conn.prepareStatement(
                    "DELETE FROM ServerLogs WHERE campaign_id = ?")) {
                    deleteServerLogs.setInt(1, campaignId);
                    deleteStats[1] = deleteServerLogs.executeUpdate();
                }

                try (PreparedStatement deleteClickLogs = conn.prepareStatement(
                    "DELETE FROM ClickLogs WHERE campaign_id = ?")) {
                    deleteClickLogs.setInt(1, campaignId);
                    deleteStats[2] = deleteClickLogs.executeUpdate();
                }

                try (PreparedStatement deleteImpressionLogs = conn.prepareStatement(
                    "DELETE FROM ImpressionLogs WHERE campaign_id = ?")) {
                    deleteImpressionLogs.setInt(1, campaignId);
                    deleteStats[3] = deleteImpressionLogs.executeUpdate();
                }

                // Delete assignments
                try (PreparedStatement deleteAssignments = conn.prepareStatement(
                    "DELETE FROM CampaignAssignments WHERE campaign_id = ?")) {
                    deleteAssignments.setInt(1, campaignId);
                    deleteStats[4] = deleteAssignments.executeUpdate();
                }

                // Finally delete campaign
                boolean success = false;
                try (PreparedStatement deleteCampaign = conn.prepareStatement(
                    "DELETE FROM Campaigns WHERE campaign_id = ?")) {
                    deleteCampaign.setInt(1, campaignId);
                    int rowsDeleted = deleteCampaign.executeUpdate();
                    success = rowsDeleted > 0;

                    // Only commit if campaign record was actually deleted
                    if (success) {
                        conn.commit();
                        // Clear any caches that might contain data for this campaign
                        clearCaches();

                        // Log successful deletion with statistics
                        long duration = System.currentTimeMillis() - startTime;
                        System.out.println("Successfully deleted campaign #" + campaignId +
                            " in " + duration + "ms. Deleted " +
                            deleteStats[1] + " server logs, " +
                            deleteStats[2] + " clicks, " +
                            deleteStats[3] + " impressions, " +
                            deleteStats[4] + " assignments");
                    } else {
                        // Campaign wasn't found, roll back
                        conn.rollback();
                        System.err.println("No campaign found with ID " + campaignId + " to delete");
                    }

                    return success;
                }
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error rolling back transaction: " + ex.getMessage());
                }

                // Categorize errors for better troubleshooting
                if (e.getSQLState() != null) {
                    if (e.getSQLState().startsWith("23")) {
                        // Integrity constraint violation
                        System.err.println("Constraint violation deleting campaign #" + campaignId +
                            ": " + e.getMessage());
                    } else if (e.getSQLState().startsWith("08")) {
                        // Connection exception
                        System.err.println("Database connection error while deleting campaign #" +
                            campaignId + ": " + e.getMessage());
                    } else {
                        System.err.println("SQL error deleting campaign #" + campaignId +
                            " (" + e.getSQLState() + "): " + e.getMessage());
                    }
                }

                throw e;
            } finally {
                // Reset connection settings
                conn.setAutoCommit(true);

                // For very large deletions, trigger garbage collection to free memory
                if (deleteStats[1] + deleteStats[2] + deleteStats[3] > 1000000) {
                    System.gc();
                }
            }
        } catch (SQLException e) {
            long duration = System.currentTimeMillis() - startTime;
            System.err.println("Error deleting campaign #" + campaignId +
                " after " + duration + "ms: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Check if a user has access to a campaign
     *
     * @param userId The user ID to check
     * @param campaignId The campaign ID to check
     * @return true if the user has access, false otherwise
     */
    public static boolean canUserAccessCampaign(int userId, int campaignId) {
        if (!ensureDatabaseInitialized()) {
            return false;
        }

        try (Connection conn = getConnection()) {
            // First check if user is the creator of the campaign
            try (PreparedStatement creatorStmt = conn.prepareStatement(
                "SELECT 1 FROM Campaigns WHERE campaign_id = ? AND user_id = ?")) {

                creatorStmt.setInt(1, campaignId);
                creatorStmt.setInt(2, userId);

                try (ResultSet creatorRs = creatorStmt.executeQuery()) {
                    if (creatorRs.next()) {
                        return true; // User is the creator
                    }
                }
            }

            // Check if user has been assigned access
            try (PreparedStatement accessStmt = conn.prepareStatement(
                "SELECT 1 FROM CampaignAssignments WHERE user_id = ? AND campaign_id = ?")) {

                accessStmt.setInt(1, userId);
                accessStmt.setInt(2, campaignId);

                try (ResultSet rs = accessStmt.executeQuery()) {
                    return rs.next(); // If any row exists, user has access
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking campaign access: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Cache for admin status to avoid repeated lookups
    private static final Map<Integer, Boolean> adminStatusCache = new HashMap<>();

    // Cache for accessible campaigns to avoid repeated queries
    private static final Map<Integer, List<CampaignInfo>> userCampaignsCache = new HashMap<>();

    /**
     * Load accessible campaigns for a user
     * Includes campaigns created by the user and campaigns assigned to the user
     * Uses caching for better performance
     *
     * @param userId The user ID
     * @return List of CampaignInfo objects the user can access
     */
    public static List<CampaignInfo> getAccessibleCampaigns(int userId) {
        if (!ensureDatabaseInitialized()) {
            return new ArrayList<>();
        }

        // Check cache first
        if (userCampaignsCache.containsKey(userId)) {
            return new ArrayList<>(userCampaignsCache.get(userId));
        }

        List<CampaignInfo> campaigns = new ArrayList<>();

        try (Connection conn = getConnection()) {
            // Check if the user is an admin (use cache if available)
            boolean isAdmin = false;

            if (adminStatusCache.containsKey(userId)) {
                isAdmin = adminStatusCache.get(userId);
            } else {
                try (PreparedStatement adminCheckStmt = conn.prepareStatement(
                    "SELECT role FROM USERS WHERE id = ?")) {

                    adminCheckStmt.setInt(1, userId);

                    try (ResultSet adminRs = adminCheckStmt.executeQuery()) {
                        if (adminRs.next() && "admin".equals(adminRs.getString("role"))) {
                            isAdmin = true;
                        }
                    }

                    // Cache the result
                    adminStatusCache.put(userId, isAdmin);
                }
            }

            // Simplified query with only necessary columns and no DISTINCT for better performance
            try (PreparedStatement stmt = conn.prepareStatement(
                isAdmin ?
                    "SELECT campaign_id, campaign_name FROM Campaigns" :
                    "SELECT c.campaign_id, c.campaign_name FROM Campaigns c " +
                        "LEFT JOIN CampaignAssignments a ON c.campaign_id = a.campaign_id " +
                        "WHERE c.user_id = ? OR a.user_id = ?")) {

                if (!isAdmin) {
                    stmt.setInt(1, userId);
                    stmt.setInt(2, userId);
                }

                try (ResultSet rs = stmt.executeQuery()) {
                    // Create lightweight campaign info objects with just ID and name for the listing
                    while (rs.next()) {
                        int campaignId = rs.getInt("campaign_id");
                        String campaignName = rs.getString("campaign_name");

                        // Create minimal campaign info for listing
                        CampaignInfo campaign = new CampaignInfo(
                            campaignId, campaignName, null, null, null, 0);

                        campaigns.add(campaign);
                    }
                }
            }

            // Cache the result
            userCampaignsCache.put(userId, new ArrayList<>(campaigns));

        } catch (SQLException e) {
            System.err.println("Error getting accessible campaigns: " + e.getMessage());
            e.printStackTrace();
        }

        return campaigns;
    }

    /**
     * Clears all caches - should be called when campaigns or permissions change
     */
    public static void clearCaches() {
        adminStatusCache.clear();
        userCampaignsCache.clear();
    }

    /**
     * Get a list of user IDs who have access to a specific campaign
     *
     * @param campaignId The campaign ID
     * @return List of user IDs with access to the campaign
     */
    public static List<Integer> getUsersWithAccess(int campaignId) {
        if (!ensureDatabaseInitialized()) {
            return new ArrayList<>();
        }

        List<Integer> userIds = new ArrayList<>();

        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT user_id FROM CampaignAssignments WHERE campaign_id = ?")) {

                stmt.setInt(1, campaignId);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        userIds.add(rs.getInt("user_id"));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting users with access: " + e.getMessage());
            e.printStackTrace();
        }

        return userIds;
    }

    /**
     * Assign a campaign to a user
     *
     * @param campaignId The campaign ID
     * @param userId The user ID to assign access to
     * @param assignedById The user ID of the admin making the assignment
     * @return true if successful, false otherwise
     */
    public static boolean assignCampaignToUser(int campaignId, int userId, int assignedById) {
        if (!ensureDatabaseInitialized()) {
            return false;
        }

        try (Connection conn = getConnection()) {
            return assignCampaignToUserInternal(conn, campaignId, userId, assignedById);
        } catch (SQLException e) {
            System.err.println("Error assigning campaign: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Internal method to assign a campaign to a user within a transaction
     */
    private static boolean assignCampaignToUserInternal(Connection conn, int campaignId, int userId, int assignedById)
        throws SQLException {

        // Check if assignment already exists
        try (PreparedStatement checkStmt = conn.prepareStatement(
            "SELECT 1 FROM CampaignAssignments WHERE campaign_id = ? AND user_id = ?")) {

            checkStmt.setInt(1, campaignId);
            checkStmt.setInt(2, userId);

            try (ResultSet rs = checkStmt.executeQuery()) {
                // If assignment already exists, return true
                if (rs.next()) {
                    return true;
                }
            }
        }

        // Create new assignment
        try (PreparedStatement insertStmt = conn.prepareStatement(
            "INSERT INTO CampaignAssignments (campaign_id, user_id, assigned_by) VALUES (?, ?, ?)")) {

            insertStmt.setInt(1, campaignId);
            insertStmt.setInt(2, userId);
            insertStmt.setInt(3, assignedById);

            int rowsAffected = insertStmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Remove a campaign assignment from a user
     *
     * @param campaignId The campaign ID
     * @param userId The user ID to remove access from
     * @return true if successful, false otherwise
     */
    public static boolean removeCampaignFromUser(int campaignId, int userId) {
        if (!ensureDatabaseInitialized()) {
            return false;
        }

        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM CampaignAssignments WHERE campaign_id = ? AND user_id = ?")) {

                stmt.setInt(1, campaignId);
                stmt.setInt(2, userId);

                int rowsAffected = stmt.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error removing campaign assignment: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Helper methods for database operations

    private static void createTablesIfNotExist(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Create Campaigns table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS Campaigns (" +
                    "campaign_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "campaign_name VARCHAR(255) NOT NULL, " +
                    "creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "start_date TIMESTAMP, " +
                    "end_date TIMESTAMP, " +
                    "bounce_pages_threshold INT DEFAULT 1, " +
                    "bounce_seconds_threshold INT DEFAULT 4, " +
                    "user_id INT" +
                    ")"
            );

            // Create ImpressionLogs table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS ImpressionLogs (" +
                    "impression_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "campaign_id INT NOT NULL, " +
                    "log_date TIMESTAMP NOT NULL, " +
                    "user_id VARCHAR(255) NOT NULL, " +
                    "gender VARCHAR(50), " +
                    "age VARCHAR(50), " +
                    "income VARCHAR(50), " +
                    "context VARCHAR(50), " +
                    "impression_cost DECIMAL(10,6), " +
                    "FOREIGN KEY (campaign_id) REFERENCES Campaigns(campaign_id) ON DELETE CASCADE" +
                    ")"
            );

            // Create ClickLogs table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS ClickLogs (" +
                    "click_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "campaign_id INT NOT NULL, " +
                    "log_date TIMESTAMP NOT NULL, " +
                    "user_id VARCHAR(255) NOT NULL, " +
                    "click_cost DECIMAL(10,6), " +
                    "FOREIGN KEY (campaign_id) REFERENCES Campaigns(campaign_id) ON DELETE CASCADE" +
                    ")"
            );

            // Create ServerLogs table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS ServerLogs (" +
                    "server_log_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "campaign_id INT NOT NULL, " +
                    "entry_date TIMESTAMP NOT NULL, " +
                    "user_id VARCHAR(255) NOT NULL, " +
                    "exit_date TIMESTAMP, " +
                    "pages_viewed INT, " +
                    "conversion BOOLEAN, " +
                    "FOREIGN KEY (campaign_id) REFERENCES Campaigns(campaign_id) ON DELETE CASCADE" +
                    ")"
            );

            // Create CampaignAssignments table - for assigning campaigns to users
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS CampaignAssignments (" +
                    "assignment_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "campaign_id INT NOT NULL, " +
                    "user_id INT NOT NULL, " +
                    "assigned_by INT NOT NULL, " +
                    "assigned_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (campaign_id) REFERENCES Campaigns(campaign_id) ON DELETE CASCADE, " +
                    "UNIQUE (campaign_id, user_id)" +
                    ")"
            );
            // Caching basic metrics for comparisons
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS CampaignMetrics (" +
                    "metric_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "campaign_id INT NOT NULL, " +
                    "impressions INT DEFAULT 0, " +
                    "clicks INT DEFAULT 0, " +
                    "uniques INT DEFAULT 0, " +
                    "bounces INT DEFAULT 0, " +
                    "conversions INT DEFAULT 0, " +
                    "totalCost DECIMAL(15,6) DEFAULT 0, " +
                    "ctr DECIMAL(15,6) DEFAULT 0, " +
                    "cpc DECIMAL(15,6) DEFAULT 0, " +
                    "cpa DECIMAL(15,6) DEFAULT 0, " +
                    "cpm DECIMAL(15,6) DEFAULT 0, " +
                    "bounceRate DECIMAL(15,6) DEFAULT 0, " +
                    "FOREIGN KEY (campaign_id) REFERENCES Campaigns(campaign_id) ON DELETE CASCADE" +
                    ")"
            );


        }
    }

    // Add this method to CampaignDatabase.java to save precomputed metrics
    private static void savePrecomputedMetrics(Connection conn, int campaignId, CampaignMetrics metrics) throws SQLException {
        // First check if metrics already exist for this campaign
        try (PreparedStatement checkStmt = conn.prepareStatement(
            "SELECT 1 FROM CampaignMetrics WHERE campaign_id = ?")) {

            checkStmt.setInt(1, campaignId);

            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    // Metrics already exist, update them
                    try (PreparedStatement updateStmt = conn.prepareStatement(
                        "UPDATE CampaignMetrics SET " +
                            "impressions = ?, clicks = ?, uniques = ?, bounces = ?, conversions = ?, " +
                            "totalCost = ?, ctr = ?, cpc = ?, cpa = ?, cpm = ?, bounceRate = ? " +
                            "WHERE campaign_id = ?")) {

                        updateStmt.setInt(1, metrics.getNumberOfImpressions());
                        updateStmt.setInt(2, metrics.getNumberOfClicks());
                        updateStmt.setInt(3, metrics.getNumberOfUniques());
                        updateStmt.setInt(4, metrics.getNumberOfBounces());
                        updateStmt.setInt(5, metrics.getNumberOfConversions());
                        updateStmt.setDouble(6, metrics.getTotalCost());
                        updateStmt.setDouble(7, metrics.getCTR());
                        updateStmt.setDouble(8, metrics.getCPC());
                        updateStmt.setDouble(9, metrics.getCPA());
                        updateStmt.setDouble(10, metrics.getCPM());
                        updateStmt.setDouble(11, metrics.getBounceRate());
                        updateStmt.setInt(12, campaignId);

                        updateStmt.executeUpdate();
                    }
                    return;
                }
            }
        }

        // Metrics don't exist, insert them
        try (PreparedStatement insertStmt = conn.prepareStatement(
            "INSERT INTO CampaignMetrics (campaign_id, impressions, clicks, uniques, bounces, conversions, " +
                "totalCost, ctr, cpc, cpa, cpm, bounceRate) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {

            insertStmt.setInt(1, campaignId);
            insertStmt.setInt(2, metrics.getNumberOfImpressions());
            insertStmt.setInt(3, metrics.getNumberOfClicks());
            insertStmt.setInt(4, metrics.getNumberOfUniques());
            insertStmt.setInt(5, metrics.getNumberOfBounces());
            insertStmt.setInt(6, metrics.getNumberOfConversions());
            insertStmt.setDouble(7, metrics.getTotalCost());
            insertStmt.setDouble(8, metrics.getCTR());
            insertStmt.setDouble(9, metrics.getCPC());
            insertStmt.setDouble(10, metrics.getCPA());
            insertStmt.setDouble(11, metrics.getCPM());
            insertStmt.setDouble(12, metrics.getBounceRate());

            insertStmt.executeUpdate();
        }
    }

    // Add this method to retrieve metrics directly from the database
    public static Map<String, Double> getMetricsDirectlyFromDatabase(int campaignId) {
        if (!ensureDatabaseInitialized()) {
            return null;
        }

        Map<String, Double> metrics = new HashMap<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT * FROM CampaignMetrics WHERE campaign_id = ?")) {

            stmt.setInt(1, campaignId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    metrics.put("impressions", (double) rs.getInt("impressions"));
                    metrics.put("clicks", (double) rs.getInt("clicks"));
                    metrics.put("uniques", (double) rs.getInt("uniques"));
                    metrics.put("bounces", (double) rs.getInt("bounces"));
                    metrics.put("conversions", (double) rs.getInt("conversions"));
                    metrics.put("totalCost", rs.getDouble("totalCost"));
                    metrics.put("ctr", rs.getDouble("ctr"));
                    metrics.put("cpc", rs.getDouble("cpc"));
                    metrics.put("cpa", rs.getDouble("cpa"));
                    metrics.put("cpm", rs.getDouble("cpm"));
                    metrics.put("bounceRate", rs.getDouble("bounceRate"));
                }
            }

            return metrics;
        } catch (SQLException e) {
            System.err.println("Error retrieving metrics: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static int insertCampaignRecord(Connection conn, CampaignMetrics campaignMetrics,
                                            String campaignName, int userId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
            "INSERT INTO Campaigns (campaign_name, start_date, end_date, " +
                "bounce_pages_threshold, bounce_seconds_threshold, user_id) " +
                "VALUES (?, ?, ?, ?, ?, ?)",
            Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, campaignName);

            // Set start and end dates
            LocalDateTime startDate = campaignMetrics.getCampaignStartDate();
            LocalDateTime endDate = campaignMetrics.getCampaignEndDate();

            if (startDate != null) {
                stmt.setTimestamp(2, Timestamp.valueOf(startDate));
            } else {
                stmt.setNull(2, Types.TIMESTAMP);
            }

            if (endDate != null) {
                stmt.setTimestamp(3, Timestamp.valueOf(endDate));
            } else {
                stmt.setNull(3, Types.TIMESTAMP);
            }

            stmt.setInt(4, campaignMetrics.getBouncePagesThreshold());
            stmt.setInt(5, campaignMetrics.getBounceSecondsThreshold());
            stmt.setInt(6, userId);

            stmt.executeUpdate();

            // Get the generated campaign ID
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new SQLException("Failed to get generated campaign ID");
                }
            }
        }
    }

    private static void insertImpressionLogs(Connection conn, ImpressionLog[] logs, int campaignId) throws SQLException {
        if (logs == null || logs.length == 0) {
            return;
        }

        try (PreparedStatement stmt = conn.prepareStatement(
            "INSERT INTO ImpressionLogs (campaign_id, log_date, user_id, gender, age, income, context, impression_cost) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {

            for (ImpressionLog log : logs) {
                if (log == null) continue;

                LogDate logDate = log.getDate();
                if (logDate == null || !logDate.getExists()) continue;

                stmt.setInt(1, campaignId);

                // Convert LogDate to LocalDateTime
                LocalDateTime dateTime = LocalDateTime.of(
                    logDate.getYear(), logDate.getMonth(), logDate.getDay(),
                    logDate.getHour(), logDate.getMinute(), logDate.getSecond()
                );
                stmt.setTimestamp(2, Timestamp.valueOf(dateTime));

                stmt.setString(3, log.getId());
                stmt.setString(4, log.getGender());
                stmt.setString(5, log.getAge());
                stmt.setString(6, log.getIncome());
                stmt.setString(7, log.getContext());
                stmt.setFloat(8, log.getImpressionCost());

                stmt.addBatch();
            }

            stmt.executeBatch();
        }
    }

    private static void insertClickLogs(Connection conn, ClickLog[] logs, int campaignId) throws SQLException {
        if (logs == null || logs.length == 0) {
            return;
        }

        try (PreparedStatement stmt = conn.prepareStatement(
            "INSERT INTO ClickLogs (campaign_id, log_date, user_id, click_cost) " +
                "VALUES (?, ?, ?, ?)")) {

            for (ClickLog log : logs) {
                if (log == null) continue;

                LogDate logDate = log.getDate();
                if (logDate == null || !logDate.getExists()) continue;

                stmt.setInt(1, campaignId);

                // Convert LogDate to LocalDateTime
                LocalDateTime dateTime = LocalDateTime.of(
                    logDate.getYear(), logDate.getMonth(), logDate.getDay(),
                    logDate.getHour(), logDate.getMinute(), logDate.getSecond()
                );
                stmt.setTimestamp(2, Timestamp.valueOf(dateTime));

                stmt.setString(3, log.getId());
                stmt.setFloat(4, log.getClickCost());

                stmt.addBatch();
            }

            stmt.executeBatch();
        }
    }

    private static void insertServerLogs(Connection conn, ServerLog[] logs, int campaignId) throws SQLException {
        if (logs == null || logs.length == 0) {
            return;
        }

        try (PreparedStatement stmt = conn.prepareStatement(
            "INSERT INTO ServerLogs (campaign_id, entry_date, user_id, exit_date, pages_viewed, conversion) " +
                "VALUES (?, ?, ?, ?, ?, ?)")) {

            for (ServerLog log : logs) {
                if (log == null) continue;

                LogDate entryDate = log.getEntryDate();
                if (entryDate == null || !entryDate.getExists()) continue;

                stmt.setInt(1, campaignId);

                // Convert entry LogDate to LocalDateTime
                LocalDateTime entryDateTime = LocalDateTime.of(
                    entryDate.getYear(), entryDate.getMonth(), entryDate.getDay(),
                    entryDate.getHour(), entryDate.getMinute(), entryDate.getSecond()
                );
                stmt.setTimestamp(2, Timestamp.valueOf(entryDateTime));

                stmt.setString(3, log.getId());

                // Handle exit date - could be null
                LogDate exitDate = log.getExitDate();
                if (exitDate != null && exitDate.getExists()) {
                    LocalDateTime exitDateTime = LocalDateTime.of(
                        exitDate.getYear(), exitDate.getMonth(), exitDate.getDay(),
                        exitDate.getHour(), exitDate.getMinute(), exitDate.getSecond()
                    );
                    stmt.setTimestamp(4, Timestamp.valueOf(exitDateTime));
                } else {
                    stmt.setNull(4, Types.TIMESTAMP);
                }

                stmt.setInt(5, log.getPagesViewed());
                stmt.setBoolean(6, log.getConversion());

                stmt.addBatch();
            }

            stmt.executeBatch();
        }
    }

    private static ImpressionLog[] loadImpressionLogs(Connection conn, int campaignId) throws SQLException {
        List<ImpressionLog> logs = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(
            "SELECT log_date, user_id, gender, age, income, context, impression_cost " +
                "FROM ImpressionLogs WHERE campaign_id = ?")) {

            stmt.setInt(1, campaignId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // Convert Timestamp to date string in the expected format for LogDate
                    Timestamp timestamp = rs.getTimestamp("log_date");
                    String dateStr = timestamp.toLocalDateTime().format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    );

                    logs.add(new ImpressionLog(
                        dateStr,
                        rs.getString("user_id"),
                        rs.getString("gender"),
                        rs.getString("age"),
                        rs.getString("income"),
                        rs.getString("context"),
                        String.format("%.6f", rs.getFloat("impression_cost"))
                    ));
                }
            }
        }

        return logs.toArray(new ImpressionLog[0]);
    }

    private static ClickLog[] loadClickLogs(Connection conn, int campaignId) throws SQLException {
        List<ClickLog> logs = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(
            "SELECT log_date, user_id, click_cost " +
                "FROM ClickLogs WHERE campaign_id = ?")) {

            stmt.setInt(1, campaignId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // Convert Timestamp to date string in the expected format for LogDate
                    Timestamp timestamp = rs.getTimestamp("log_date");
                    String dateStr = timestamp.toLocalDateTime().format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    );

                    logs.add(new ClickLog(
                        dateStr,
                        rs.getString("user_id"),
                        String.format("%.6f", rs.getFloat("click_cost"))
                    ));
                }
            }
        }

        return logs.toArray(new ClickLog[0]);
    }

    private static ServerLog[] loadServerLogs(Connection conn, int campaignId) throws SQLException {
        List<ServerLog> logs = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(
            "SELECT entry_date, user_id, exit_date, pages_viewed, conversion " +
                "FROM ServerLogs WHERE campaign_id = ?")) {

            stmt.setInt(1, campaignId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // Convert Timestamp to date string in the expected format for LogDate
                    Timestamp entryTimestamp = rs.getTimestamp("entry_date");
                    String entryDateStr = entryTimestamp.toLocalDateTime().format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    );

                    // Handle potentially null exit_date
                    String exitDateStr = "n/a";
                    Timestamp exitTimestamp = rs.getTimestamp("exit_date");
                    if (exitTimestamp != null) {
                        exitDateStr = exitTimestamp.toLocalDateTime().format(
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        );
                    }

                    logs.add(new ServerLog(
                        entryDateStr,
                        rs.getString("user_id"),
                        exitDateStr,
                        String.valueOf(rs.getInt("pages_viewed")),
                        rs.getBoolean("conversion") ? "Yes" : "No"
                    ));
                }
            }
        }

        return logs.toArray(new ServerLog[0]);
    }

    /**
     * Class to hold campaign metadata for listings - with extended fields
     */
    public static class CampaignInfo {
        private final int campaignId;
        private final String campaignName;
        private final LocalDateTime creationDate;
        private final LocalDateTime startDate;
        private final LocalDateTime endDate;
        private final int userId;
        private int bouncePagesThreshold = 1; // Default values
        private int bounceSecondsThreshold = 4;

        public CampaignInfo(int campaignId, String campaignName,
                            LocalDateTime creationDate, LocalDateTime startDate,
                            LocalDateTime endDate, int userId) {
            this.campaignId = campaignId;
            this.campaignName = campaignName;
            this.creationDate = creationDate;
            this.startDate = startDate;
            this.endDate = endDate;
            this.userId = userId;
        }

        // Getters
        public int getCampaignId() { return campaignId; }
        public String getCampaignName() { return campaignName; }
        public LocalDateTime getCreationDate() { return creationDate; }
        public LocalDateTime getStartDate() { return startDate; }
        public LocalDateTime getEndDate() { return endDate; }
        public int getUserId() { return userId; }
        public int getBouncePagesThreshold() { return bouncePagesThreshold; }
        public int getBounceSecondsThreshold() { return bounceSecondsThreshold; }

        // Setters for bounce thresholds
        public void setBouncePagesThreshold(int bouncePagesThreshold) {
            this.bouncePagesThreshold = bouncePagesThreshold;
        }

        public void setBounceSecondsThreshold(int bounceSecondsThreshold) {
            this.bounceSecondsThreshold = bounceSecondsThreshold;
        }

        @Override
        public String toString() {
            return campaignName + " (ID: " + campaignId + ", Created: " +
                (creationDate != null ? creationDate.toLocalDate() : "Unknown") + ")";
        }
    }

}