package com.example.ad_auction_dashboard.logic;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * This class manages campaign database operations.
 * It handles saving campaigns to a local database and loading them back.
 */
public class CampaignDatabase {

    // Database folder path
    private static final String DB_FOLDER = System.getProperty("user.home") + File.separator + "user_databases";

    // Database URL template for H2
    private static final String DB_URL_PREFIX = "jdbc:h2:file:";
    private static final String DB_NAME = "campaigns";

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
        String dbPath = DB_FOLDER + File.separator + DB_NAME;
        String dbUrl = DB_URL_PREFIX + dbPath;

        try {
            // Load H2 driver - H2 is included with JavaFX so should be available
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("H2 JDBC driver not found: " + e.getMessage());
            throw new SQLException("H2 JDBC driver not found", e);
        }

        return DriverManager.getConnection(dbUrl, "sa", "");
    }

    /**
     * Save a campaign to the database
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
}