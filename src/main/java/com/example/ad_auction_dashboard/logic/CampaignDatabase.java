package com.example.ad_auction_dashboard.logic;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
     * Save a campaign to the database and assign access to the creator
     *
     * @param campaignMetrics The campaign to save
     * @param campaignName The name of the campaign
     * @param userId The ID of the user saving the campaign
     * @return The ID of the saved campaign, or -1 if an error occurred
     */
    public static int saveCampaign(CampaignMetrics campaignMetrics, String campaignName, int userId) {
        initDatabaseFolder();

        try (Connection conn = getConnection()) {
            // Ensure tables exist
            createTablesIfNotExist(conn);

            // Begin transaction
            conn.setAutoCommit(false);

            try {
                // Insert campaign record
                int campaignId = insertCampaignRecord(conn, campaignMetrics, campaignName, userId);

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
                conn.rollback();
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
        // First, get all admin users - FIX: Using correct case "USERS" instead of "users"
        PreparedStatement stmt = conn.prepareStatement(
            "SELECT id FROM USERS WHERE role = 'admin' AND id != ?"
        );
        stmt.setInt(1, assignedBy); // Exclude the user who created the campaign (they already have access)

        ResultSet rs = stmt.executeQuery();

        // Grant access to each admin
        while (rs.next()) {
            int adminId = rs.getInt("id");
            assignCampaignToUserInternal(conn, campaignId, adminId, assignedBy);
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
        initDatabaseFolder();

        try (Connection conn = getConnection()) {
            // Get all campaigns
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT campaign_id FROM Campaigns"
            );

            ResultSet rs = stmt.executeQuery();

            // Grant access to each campaign
            while (rs.next()) {
                int campaignId = rs.getInt("campaign_id");
                assignCampaignToUserInternal(conn, campaignId, adminId, grantedBy);
            }

            return true;
        } catch (SQLException e) {
            System.err.println("Error granting admin access to campaigns: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Load a campaign from the database
     *
     * @param campaignId The ID of the campaign to load
     * @return A Campaign object containing the loaded data, or null if an error occurred
     */
    public static Campaign loadCampaign(int campaignId) {
        initDatabaseFolder();

        try (Connection conn = getConnection()) {
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

        } catch (SQLException e) {
            System.err.println("Error loading campaign: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get a list of all saved campaigns
     *
     * @return A list of CampaignInfo objects containing campaign metadata
     */
    public static List<CampaignInfo> getAllCampaigns() {
        initDatabaseFolder();

        List<CampaignInfo> campaigns = new ArrayList<>();

        try (Connection conn = getConnection()) {
            // Create tables if they don't exist
            createTablesIfNotExist(conn);

            // Query for all campaigns
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT campaign_id, campaign_name, creation_date, start_date, end_date, user_id FROM Campaigns"
            );

            ResultSet rs = stmt.executeQuery();
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
        initDatabaseFolder();

        List<CampaignInfo> campaigns = new ArrayList<>();

        try (Connection conn = getConnection()) {
            createTablesIfNotExist(conn);

            // Query for user's campaigns
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT campaign_id, campaign_name, creation_date, start_date, end_date, user_id " +
                    "FROM Campaigns WHERE user_id = ?"
            );
            stmt.setInt(1, userId);

            ResultSet rs = stmt.executeQuery();
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

        } catch (SQLException e) {
            System.err.println("Error fetching user campaigns: " + e.getMessage());
            e.printStackTrace();
        }

        return campaigns;
    }


    /**
     * Delete a campaign from the database
     *
     * @param campaignId The ID of the campaign to delete
     * @return true if the campaign was deleted successfully, false otherwise
     */
    public static boolean deleteCampaign(int campaignId) {
        initDatabaseFolder();

        try (Connection conn = getConnection()) {
            // Begin transaction
            conn.setAutoCommit(false);

            try {
                // Delete from all tables
                PreparedStatement deleteServerLogs = conn.prepareStatement(
                    "DELETE FROM ServerLogs WHERE campaign_id = ?"
                );
                deleteServerLogs.setInt(1, campaignId);
                deleteServerLogs.executeUpdate();

                PreparedStatement deleteClickLogs = conn.prepareStatement(
                    "DELETE FROM ClickLogs WHERE campaign_id = ?"
                );
                deleteClickLogs.setInt(1, campaignId);
                deleteClickLogs.executeUpdate();

                PreparedStatement deleteImpressionLogs = conn.prepareStatement(
                    "DELETE FROM ImpressionLogs WHERE campaign_id = ?"
                );
                deleteImpressionLogs.setInt(1, campaignId);
                deleteImpressionLogs.executeUpdate();

                // Delete assignments
                PreparedStatement deleteAssignments = conn.prepareStatement(
                    "DELETE FROM CampaignAssignments WHERE campaign_id = ?"
                );
                deleteAssignments.setInt(1, campaignId);
                deleteAssignments.executeUpdate();

                PreparedStatement deleteCampaign = conn.prepareStatement(
                    "DELETE FROM Campaigns WHERE campaign_id = ?"
                );
                deleteCampaign.setInt(1, campaignId);
                int rowsDeleted = deleteCampaign.executeUpdate();

                conn.commit();

                return rowsDeleted > 0;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Error deleting campaign: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Helper methods for database operations

    private static void createTablesIfNotExist(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();

        // Create Campaigns table - REMOVED FOREIGN KEY CONSTRAINT
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
    }

    private static int insertCampaignRecord(Connection conn, CampaignMetrics campaignMetrics,
                                            String campaignName, int userId) throws SQLException {

        PreparedStatement stmt = conn.prepareStatement(
            "INSERT INTO Campaigns (campaign_name, start_date, end_date, " +
                "bounce_pages_threshold, bounce_seconds_threshold, user_id) " +
                "VALUES (?, ?, ?, ?, ?, ?)",
            Statement.RETURN_GENERATED_KEYS
        );

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
        ResultSet rs = stmt.getGeneratedKeys();

        if (rs.next()) {
            return rs.getInt(1);
        } else {
            throw new SQLException("Failed to get generated campaign ID");
        }
    }

    private static void insertImpressionLogs(Connection conn, ImpressionLog[] logs, int campaignId) throws SQLException {
        if (logs == null || logs.length == 0) {
            return;
        }

        PreparedStatement stmt = conn.prepareStatement(
            "INSERT INTO ImpressionLogs (campaign_id, log_date, user_id, gender, age, income, context, impression_cost) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
        );

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

    private static void insertClickLogs(Connection conn, ClickLog[] logs, int campaignId) throws SQLException {
        if (logs == null || logs.length == 0) {
            return;
        }

        PreparedStatement stmt = conn.prepareStatement(
            "INSERT INTO ClickLogs (campaign_id, log_date, user_id, click_cost) " +
                "VALUES (?, ?, ?, ?)"
        );

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

    private static void insertServerLogs(Connection conn, ServerLog[] logs, int campaignId) throws SQLException {
        if (logs == null || logs.length == 0) {
            return;
        }

        PreparedStatement stmt = conn.prepareStatement(
            "INSERT INTO ServerLogs (campaign_id, entry_date, user_id, exit_date, pages_viewed, conversion) " +
                "VALUES (?, ?, ?, ?, ?, ?)"
        );

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

    private static ImpressionLog[] loadImpressionLogs(Connection conn, int campaignId) throws SQLException {
        List<ImpressionLog> logs = new ArrayList<>();

        PreparedStatement stmt = conn.prepareStatement(
            "SELECT log_date, user_id, gender, age, income, context, impression_cost " +
                "FROM ImpressionLogs WHERE campaign_id = ?"
        );
        stmt.setInt(1, campaignId);

        ResultSet rs = stmt.executeQuery();
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

        return logs.toArray(new ImpressionLog[0]);
    }

    private static ClickLog[] loadClickLogs(Connection conn, int campaignId) throws SQLException {
        List<ClickLog> logs = new ArrayList<>();

        PreparedStatement stmt = conn.prepareStatement(
            "SELECT log_date, user_id, click_cost " +
                "FROM ClickLogs WHERE campaign_id = ?"
        );
        stmt.setInt(1, campaignId);

        ResultSet rs = stmt.executeQuery();
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

        return logs.toArray(new ClickLog[0]);
    }

    private static ServerLog[] loadServerLogs(Connection conn, int campaignId) throws SQLException {
        List<ServerLog> logs = new ArrayList<>();

        PreparedStatement stmt = conn.prepareStatement(
            "SELECT entry_date, user_id, exit_date, pages_viewed, conversion " +
                "FROM ServerLogs WHERE campaign_id = ?"
        );
        stmt.setInt(1, campaignId);

        ResultSet rs = stmt.executeQuery();
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

        return logs.toArray(new ServerLog[0]);
    }

    /**
     * Class to hold campaign metadata for listings
     */
    public static class CampaignInfo {
        private final int campaignId;
        private final String campaignName;
        private final LocalDateTime creationDate;
        private final LocalDateTime startDate;
        private final LocalDateTime endDate;
        private final int userId;

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

        @Override
        public String toString() {
            return campaignName + " (ID: " + campaignId + ", Created: " +
                (creationDate != null ? creationDate.toLocalDate() : "Unknown") + ")";
        }
    }

    /**
     * Get metrics for a specific time period and filters directly from the database
     * rather than loading all data
     */
    public static Map<String, Object> getFilteredMetrics(
        int campaignId,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String genderFilter,
        String contextFilter) {

        Map<String, Object> metrics = new HashMap<>();

        try (Connection conn = getConnection()) {
            // Query for impressions with filters
            StringBuilder impressionsQuery = new StringBuilder(
                "SELECT COUNT(*) as impression_count, SUM(impression_cost) as impression_cost " +
                    "FROM ImpressionLogs WHERE campaign_id = ? AND log_date BETWEEN ? AND ?"
            );

            // Add filters if provided
            if (genderFilter != null && !genderFilter.isEmpty() && !genderFilter.equals("All")) {
                impressionsQuery.append(" AND gender = ?");
            }
            if (contextFilter != null && !contextFilter.isEmpty() && !contextFilter.equals("All")) {
                impressionsQuery.append(" AND context = ?");
            }

            PreparedStatement stmt = conn.prepareStatement(impressionsQuery.toString());
            int paramIndex = 1;
            stmt.setInt(paramIndex++, campaignId);
            stmt.setTimestamp(paramIndex++, Timestamp.valueOf(startDate));
            stmt.setTimestamp(paramIndex++, Timestamp.valueOf(endDate));

            if (genderFilter != null && !genderFilter.isEmpty() && !genderFilter.equals("All")) {
                stmt.setString(paramIndex++, genderFilter);
            }
            if (contextFilter != null && !contextFilter.isEmpty() && !contextFilter.equals("All")) {
                stmt.setString(paramIndex++, contextFilter);
            }

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                metrics.put("impressions", rs.getInt("impression_count"));
                metrics.put("impression_cost", rs.getDouble("impression_cost"));
            }

            // Similar approach for clicks, conversions, etc.
            // ...

            // Get the number of unique users
            String uniquesQuery =
                "SELECT COUNT(DISTINCT user_id) as unique_count " +
                    "FROM ClickLogs WHERE campaign_id = ? AND log_date BETWEEN ? AND ?";

            stmt = conn.prepareStatement(uniquesQuery);
            stmt.setInt(1, campaignId);
            stmt.setTimestamp(2, Timestamp.valueOf(startDate));
            stmt.setTimestamp(3, Timestamp.valueOf(endDate));

            rs = stmt.executeQuery();
            if (rs.next()) {
                metrics.put("uniques", rs.getInt("unique_count"));
            }

            // Calculate bounce rate directly in SQL
            String bounceQuery =
                "SELECT COUNT(*) as bounce_count FROM ServerLogs " +
                    "WHERE campaign_id = ? AND entry_date BETWEEN ? AND ? " +
                    "AND (pages_viewed <= ? OR (TIMESTAMPDIFF(SECOND, entry_date, exit_date) <= ?))";

            stmt = conn.prepareStatement(bounceQuery);
            stmt.setInt(1, campaignId);
            stmt.setTimestamp(2, Timestamp.valueOf(startDate));
            stmt.setTimestamp(3, Timestamp.valueOf(endDate));
            stmt.setInt(4, getBouncePageThreshold(conn, campaignId));
            stmt.setInt(5, getBounceTimeThreshold(conn, campaignId));

            rs = stmt.executeQuery();
            if (rs.next()) {
                metrics.put("bounces", rs.getInt("bounce_count"));
            }

        } catch (SQLException e) {
            System.err.println("Error getting filtered metrics: " + e.getMessage());
            e.printStackTrace();
        }

        return metrics;
    }

    /**
     * Get time-based metrics for charts directly from the database
     */
    public static Map<String, Map<String, Object>> getTimeSeriesMetrics(
        int campaignId,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String granularity) {

        Map<String, Map<String, Object>> timeSeriesData = new HashMap<>();

        try (Connection conn = getConnection()) {
            String timeFormat;
            String groupBy;

            // Set SQL time format based on granularity
            switch (granularity) {
                case "Hourly":
                    timeFormat = "FORMAT(log_date, 'yyyy-MM-dd HH:00:00')";
                    groupBy = "HOUR";
                    break;
                case "Daily":
                    timeFormat = "FORMAT(log_date, 'yyyy-MM-dd')";
                    groupBy = "DAY";
                    break;
                case "Weekly":
                    // H2 specific format for week grouping
                    timeFormat = "FORMATDATETIME(log_date, 'yyyy-ww')";
                    groupBy = "WEEK";
                    break;
                default:
                    timeFormat = "FORMAT(log_date, 'yyyy-MM-dd')";
                    groupBy = "DAY";
            }

            // Query impressions grouped by time
            String query =
                "SELECT " + timeFormat + " as time_bucket, " +
                    "COUNT(*) as count, SUM(impression_cost) as cost " +
                    "FROM ImpressionLogs " +
                    "WHERE campaign_id = ? AND log_date BETWEEN ? AND ? " +
                    "GROUP BY " + groupBy + "(log_date) " +
                    "ORDER BY time_bucket";

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, campaignId);
            stmt.setTimestamp(2, Timestamp.valueOf(startDate));
            stmt.setTimestamp(3, Timestamp.valueOf(endDate));

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String timeBucket = rs.getString("time_bucket");
                Map<String, Object> bucketData = new HashMap<>();
                bucketData.put("impressions", rs.getInt("count"));
                bucketData.put("impression_cost", rs.getDouble("cost"));

                timeSeriesData.put(timeBucket, bucketData);
            }

            // Similar queries for clicks, conversions, etc.
            // ...
        } catch (SQLException e) {
            System.err.println("Error getting time series metrics: " + e.getMessage());
            e.printStackTrace();
        }

        return timeSeriesData;
    }

    /**
     * Get histogram data directly from the database
     */
    public static Map<String, Integer> getClickCostHistogram(
        int campaignId,
        LocalDateTime startDate,
        LocalDateTime endDate,
        int numBins) {

        Map<String, Integer> histogramData = new LinkedHashMap<>();

        try (Connection conn = getConnection()) {
            // First get min and max click costs
            String minMaxQuery =
                "SELECT MIN(click_cost) as min_cost, MAX(click_cost) as max_cost " +
                    "FROM ClickLogs WHERE campaign_id = ? AND log_date BETWEEN ? AND ?";

            PreparedStatement stmt = conn.prepareStatement(minMaxQuery);
            stmt.setInt(1, campaignId);
            stmt.setTimestamp(2, Timestamp.valueOf(startDate));
            stmt.setTimestamp(3, Timestamp.valueOf(endDate));

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                double minCost = rs.getDouble("min_cost");
                double maxCost = rs.getDouble("max_cost");

                // Add buffer to max
                maxCost += 0.001;

                // Calculate bin width
                double binWidth = (maxCost - minCost) / numBins;

                // Now get the histogram data using SQL CASE statements
                StringBuilder histQuery = new StringBuilder(
                    "SELECT ");

                for (int i = 0; i < numBins; i++) {
                    double lowerBound = minCost + (i * binWidth);
                    double upperBound = lowerBound + binWidth;

                    histQuery.append("SUM(CASE WHEN click_cost >= ")
                        .append(lowerBound)
                        .append(" AND click_cost < ")
                        .append(upperBound)
                        .append(" THEN 1 ELSE 0 END) as bin")
                        .append(i);

                    if (i < numBins - 1) {
                        histQuery.append(", ");
                    }

                    // Format bin label
                    String binLabel;
                    if (lowerBound < 0.01 && upperBound < 0.01) {
                        binLabel = String.format("$%.4f-$%.4f", lowerBound, upperBound);
                    } else {
                        binLabel = String.format("$%.2f-$%.2f", lowerBound, upperBound);
                    }

                    // Initialize the bin in the result map
                    histogramData.put(binLabel, 0);
                }

                histQuery.append(" FROM ClickLogs WHERE campaign_id = ? AND log_date BETWEEN ? AND ?");

                stmt = conn.prepareStatement(histQuery.toString());
                stmt.setInt(1, campaignId);
                stmt.setTimestamp(2, Timestamp.valueOf(startDate));
                stmt.setTimestamp(3, Timestamp.valueOf(endDate));

                rs = stmt.executeQuery();
                if (rs.next()) {
                    int binIndex = 0;
                    for (String binLabel : histogramData.keySet()) {
                        histogramData.put(binLabel, rs.getInt("bin" + binIndex));
                        binIndex++;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error generating histogram: " + e.getMessage());
            e.printStackTrace();
        }

        return histogramData;
    }

    // Helper method to get bounce page threshold
    private static int getBouncePageThreshold(Connection conn, int campaignId) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
            "SELECT bounce_pages_threshold FROM Campaigns WHERE campaign_id = ?"
        );
        stmt.setInt(1, campaignId);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return rs.getInt("bounce_pages_threshold");
        }
        return 1; // Default
    }

    // Helper method to get bounce time threshold
    private static int getBounceTimeThreshold(Connection conn, int campaignId) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
            "SELECT bounce_seconds_threshold FROM Campaigns WHERE campaign_id = ?"
        );
        stmt.setInt(1, campaignId);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return rs.getInt("bounce_seconds_threshold");
        }
        return 4; // Default
    }

    /**
     * Check if a user has access to a campaign
     *
     * @param userId The user ID to check
     * @param campaignId The campaign ID to check
     * @return true if the user has access, false otherwise
     */
    public static boolean hasUserAccess(int userId, int campaignId) {
        initDatabaseFolder();

        try (Connection conn = getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT 1 FROM CampaignAssignments WHERE user_id = ? AND campaign_id = ?"
            );
            stmt.setInt(1, userId);
            stmt.setInt(2, campaignId);

            ResultSet rs = stmt.executeQuery();
            return rs.next(); // If any row exists, user has access
        } catch (SQLException e) {
            System.err.println("Error checking user access: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Internal method to assign a campaign to a user within a transaction
     * without checking if the assignment already exists
     */
    private static boolean assignCampaignToUserInternal(Connection conn, int campaignId, int userId, int assignedById)
        throws SQLException {
        // Check if assignment already exists
        PreparedStatement checkStmt = conn.prepareStatement(
            "SELECT 1 FROM CampaignAssignments WHERE campaign_id = ? AND user_id = ?"
        );
        checkStmt.setInt(1, campaignId);
        checkStmt.setInt(2, userId);
        ResultSet rs = checkStmt.executeQuery();

        // If assignment already exists, return true
        if (rs.next()) {
            return true;
        }

        // Create new assignment
        PreparedStatement insertStmt = conn.prepareStatement(
            "INSERT INTO CampaignAssignments (campaign_id, user_id, assigned_by) VALUES (?, ?, ?)"
        );
        insertStmt.setInt(1, campaignId);
        insertStmt.setInt(2, userId);
        insertStmt.setInt(3, assignedById);

        int rowsAffected = insertStmt.executeUpdate();
        return rowsAffected > 0;
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
        initDatabaseFolder();

        try (Connection conn = getConnection()) {
            return assignCampaignToUserInternal(conn, campaignId, userId, assignedById);
        } catch (SQLException e) {
            System.err.println("Error assigning campaign: " + e.getMessage());
            e.printStackTrace();
            return false;
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
        initDatabaseFolder();

        try (Connection conn = getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM CampaignAssignments WHERE campaign_id = ? AND user_id = ?"
            );
            stmt.setInt(1, campaignId);
            stmt.setInt(2, userId);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error removing campaign assignment: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get all users who have access to a specific campaign
     *
     * @param campaignId The campaign ID
     * @return List of user IDs with access to the campaign
     */
    public static List<Integer> getUsersWithAccess(int campaignId) {
        initDatabaseFolder();
        List<Integer> userIds = new ArrayList<>();

        try (Connection conn = getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT user_id FROM CampaignAssignments WHERE campaign_id = ?"
            );
            stmt.setInt(1, campaignId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                userIds.add(rs.getInt("user_id"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting users with access: " + e.getMessage());
            e.printStackTrace();
        }

        return userIds;
    }

    /**
     * Get all campaigns that a user has access to
     *
     * @param userId The user ID
     * @return List of campaign IDs the user has access to
     */
    public static List<Integer> getCampaignsForUser(int userId) {
        initDatabaseFolder();
        List<Integer> campaignIds = new ArrayList<>();

        try (Connection conn = getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT campaign_id FROM CampaignAssignments WHERE user_id = ?"
            );
            stmt.setInt(1, userId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                campaignIds.add(rs.getInt("campaign_id"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting campaigns for user: " + e.getMessage());
            e.printStackTrace();
        }

        return campaignIds;
    }

    /**
     * Check if a user can access a specific campaign
     * This considers both direct creation (user_id in Campaigns table)
     * and assignments (CampaignAssignments table)
     *
     * @param userId The user ID
     * @param campaignId The campaign ID
     * @return true if the user can access the campaign, false otherwise
     */
    public static boolean canUserAccessCampaign(int userId, int campaignId) {
        initDatabaseFolder();

        try (Connection conn = getConnection()) {
            // First check if user is the creator of the campaign
            PreparedStatement creatorStmt = conn.prepareStatement(
                "SELECT 1 FROM Campaigns WHERE campaign_id = ? AND user_id = ?"
            );
            creatorStmt.setInt(1, campaignId);
            creatorStmt.setInt(2, userId);

            ResultSet creatorRs = creatorStmt.executeQuery();
            if (creatorRs.next()) {
                return true; // User is the creator
            }

            // Check if user has been assigned access
            return hasUserAccess(userId, campaignId);
        } catch (SQLException e) {
            System.err.println("Error checking campaign access: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Load accessible campaigns for a user
     * Includes campaigns created by the user and campaigns assigned to the user
     *
     * @param userId The user ID
     * @return List of CampaignInfo objects the user can access
     */
    public static List<CampaignInfo> getAccessibleCampaigns(int userId) {
        initDatabaseFolder();
        List<CampaignInfo> campaigns = new ArrayList<>();

        try (Connection conn = getConnection()) {
            // Check if the user is an admin
            boolean isAdmin = false;
            PreparedStatement adminCheckStmt = conn.prepareStatement(
                "SELECT role FROM USERS WHERE id = ?"
            );
            adminCheckStmt.setInt(1, userId);
            ResultSet adminRs = adminCheckStmt.executeQuery();
            if (adminRs.next() && "admin".equals(adminRs.getString("role"))) {
                isAdmin = true;
            }

            PreparedStatement stmt;

            if (isAdmin) {
                // Admins can see all campaigns
                stmt = conn.prepareStatement(
                    "SELECT DISTINCT c.campaign_id, c.campaign_name, c.creation_date, " +
                        "c.start_date, c.end_date, c.user_id FROM Campaigns c"
                );
            } else {
                // Regular users can only see campaigns they created or have been assigned to
                stmt = conn.prepareStatement(
                    "SELECT DISTINCT c.campaign_id, c.campaign_name, c.creation_date, " +
                        "c.start_date, c.end_date, c.user_id FROM Campaigns c " +
                        "LEFT JOIN CampaignAssignments a ON c.campaign_id = a.campaign_id " +
                        "WHERE c.user_id = ? OR a.user_id = ?"
                );
                stmt.setInt(1, userId);
                stmt.setInt(2, userId);
            }

            ResultSet rs = stmt.executeQuery();
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
        } catch (SQLException e) {
            System.err.println("Error getting accessible campaigns: " + e.getMessage());
            e.printStackTrace();
        }

        return campaigns;
    }
    /**
     * Get a specific metric directly from the database for a campaign with filters applied.
     * This provides a more efficient way to get a single metric without loading the entire campaign.
     *
     * @param campaignId The ID of the campaign
     * @param metricName The name of the metric (impressions, clicks, etc.)
     * @param startDate Start date for filtering
     * @param endDate End date for filtering
     * @param genderFilter Optional gender filter (null means no filter)
     * @param ageFilter Optional age filter (null means no filter)
     * @param incomeFilter Optional income filter (null means no filter)
     * @param contextFilter Optional context filter (null means no filter)
     * @return The metric value as a double
     */
    public static double getMetricDirectFromDB(
        int campaignId,
        String metricName,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String genderFilter,
        String ageFilter,
        String incomeFilter,
        String contextFilter) {

        initDatabaseFolder();

        try (Connection conn = getConnection()) {
            // Define base queries for different metrics
            String sql = "";

            switch (metricName.toLowerCase()) {
                case "impressions":
                    sql = "SELECT COUNT(*) as value FROM ImpressionLogs WHERE campaign_id = ? AND log_date BETWEEN ? AND ?";
                    break;

                case "clicks":
                    sql = "SELECT COUNT(*) as value FROM ClickLogs WHERE campaign_id = ? AND log_date BETWEEN ? AND ?";
                    break;

                case "bounces":
                    sql = "SELECT COUNT(*) as value FROM ServerLogs " +
                        "WHERE campaign_id = ? AND entry_date BETWEEN ? AND ? " +
                        "AND (pages_viewed <= ? OR (TIMESTAMPDIFF(SECOND, entry_date, exit_date) <= ?))";
                    break;

                case "conversions":
                    sql = "SELECT COUNT(*) as value FROM ServerLogs " +
                        "WHERE campaign_id = ? AND entry_date BETWEEN ? AND ? AND conversion = true";
                    break;

                case "uniques":
                    sql = "SELECT COUNT(DISTINCT user_id) as value FROM ClickLogs " +
                        "WHERE campaign_id = ? AND log_date BETWEEN ? AND ?";
                    break;

                case "total_cost":
                    sql = "SELECT " +
                        "(SELECT COALESCE(SUM(impression_cost), 0) FROM ImpressionLogs " +
                        "WHERE campaign_id = ? AND log_date BETWEEN ? AND ?) + " +
                        "(SELECT COALESCE(SUM(click_cost), 0) FROM ClickLogs " +
                        "WHERE campaign_id = ? AND log_date BETWEEN ? AND ?) as value";
                    break;

                default:
                    // For derived metrics, we'll calculate them after getting the base metrics
                    return getMetricFromBasicMetrics(
                        campaignId, metricName, startDate, endDate,
                        genderFilter, ageFilter, incomeFilter, contextFilter);
            }

            // Add audience filters if provided
            if (genderFilter != null || ageFilter != null || incomeFilter != null || contextFilter != null) {
                // For metrics that need to filter through impression data
                if (!metricName.equalsIgnoreCase("bounces") && !metricName.equalsIgnoreCase("conversions")) {
                    // We need to add a join to ImpressionLogs or consider user IDs from impressions
                    sql = addAudienceFiltersToQuery(sql, metricName);
                }
            }

            PreparedStatement stmt = conn.prepareStatement(sql);

            // Set parameters based on the query
            int paramIndex = 1;

            // Basic date and campaign ID parameters for all queries
            stmt.setInt(paramIndex++, campaignId);
            stmt.setTimestamp(paramIndex++, Timestamp.valueOf(startDate));
            stmt.setTimestamp(paramIndex++, Timestamp.valueOf(endDate));

            // For total_cost, we need to set parameters twice
            if (metricName.equalsIgnoreCase("total_cost")) {
                stmt.setInt(paramIndex++, campaignId);
                stmt.setTimestamp(paramIndex++, Timestamp.valueOf(startDate));
                stmt.setTimestamp(paramIndex++, Timestamp.valueOf(endDate));
            }

            // For bounces, add threshold parameters
            if (metricName.equalsIgnoreCase("bounces")) {
                stmt.setInt(paramIndex++, getBouncePageThreshold(conn, campaignId));
                stmt.setInt(paramIndex++, getBounceTimeThreshold(conn, campaignId));
            }

            // Add audience filter parameters if needed
            if (genderFilter != null || ageFilter != null || incomeFilter != null || contextFilter != null) {
                if (genderFilter != null) {
                    stmt.setString(paramIndex++, genderFilter);
                }
                if (ageFilter != null) {
                    stmt.setString(paramIndex++, ageFilter);
                }
                if (incomeFilter != null) {
                    stmt.setString(paramIndex++, incomeFilter);
                }
                if (contextFilter != null) {
                    stmt.setString(paramIndex++, contextFilter);
                }
            }

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getDouble("value");
            }

            return 0.0;
        } catch (SQLException e) {
            System.err.println("Error fetching metric from database: " + e.getMessage());
            e.printStackTrace();
            return 0.0;
        }
    }

    /**
     * Helper method to modify SQL queries to include audience filters
     */
    private static String addAudienceFiltersToQuery(String sql, String metricName) {
        // Extract main parts of the query
        String selectPart = sql.substring(0, sql.indexOf("FROM"));
        String fromWherePart = sql.substring(sql.indexOf("FROM"));

        // For impressions, we can directly filter
        if (metricName.equalsIgnoreCase("impressions")) {
            StringBuilder newSql = new StringBuilder(sql);
            boolean hasWhere = sql.contains("WHERE");

            // Add filter conditions
            if (hasWhere) {
                newSql.append(" AND ");
            } else {
                newSql.append(" WHERE ");
            }

            newSql.append("(gender = ? OR ? IS NULL) AND ")
                .append("(age = ? OR ? IS NULL) AND ")
                .append("(income = ? OR ? IS NULL) AND ")
                .append("(context = ? OR ? IS NULL)");

            return newSql.toString();
        }
        // For clicks, uniques, etc. we need to join with impressions
        else if (metricName.equalsIgnoreCase("clicks") || metricName.equalsIgnoreCase("uniques")) {
            return selectPart +
                " FROM ClickLogs c JOIN ImpressionLogs i ON c.user_id = i.user_id " +
                " WHERE c.campaign_id = ? AND c.log_date BETWEEN ? AND ? " +
                " AND i.campaign_id = c.campaign_id " +
                " AND (i.gender = ? OR ? IS NULL) " +
                " AND (i.age = ? OR ? IS NULL) " +
                " AND (i.income = ? OR ? IS NULL) " +
                " AND (i.context = ? OR ? IS NULL)";
        }
        // For total_cost, it's more complex - we already have a compound query
        else if (metricName.equalsIgnoreCase("total_cost")) {
            // This would require more complex logic to filter both impression costs and click costs
            // For simplicity, we might want to handle this separately
            return sql;
        }

        return sql;
    }

    /**
     * Calculate derived metrics from basic metrics
     */
    private static double getMetricFromBasicMetrics(
        int campaignId,
        String metricName,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String genderFilter,
        String ageFilter,
        String incomeFilter,
        String contextFilter) {

        // Get base metrics needed for calculation
        double impressions = getMetricDirectFromDB(
            campaignId, "impressions", startDate, endDate,
            genderFilter, ageFilter, incomeFilter, contextFilter);

        double clicks = getMetricDirectFromDB(
            campaignId, "clicks", startDate, endDate,
            genderFilter, ageFilter, incomeFilter, contextFilter);

        double conversions = getMetricDirectFromDB(
            campaignId, "conversions", startDate, endDate,
            genderFilter, ageFilter, incomeFilter, contextFilter);

        double bounces = getMetricDirectFromDB(
            campaignId, "bounces", startDate, endDate,
            genderFilter, ageFilter, incomeFilter, contextFilter);

        double totalCost = getMetricDirectFromDB(
            campaignId, "total_cost", startDate, endDate,
            genderFilter, ageFilter, incomeFilter, contextFilter);

        // Calculate the requested metric
        switch (metricName.toLowerCase()) {
            case "ctr":
                return impressions > 0 ? clicks / impressions : 0;

            case "cpc":
                return clicks > 0 ? totalCost / clicks : 0;

            case "cpa":
                return conversions > 0 ? totalCost / conversions : 0;

            case "cpm":
                return impressions > 0 ? (totalCost / impressions) * 1000 : 0;

            case "bounce_rate":
                return clicks > 0 ? bounces / clicks : 0;

            default:
                return 0;
        }
    }

}