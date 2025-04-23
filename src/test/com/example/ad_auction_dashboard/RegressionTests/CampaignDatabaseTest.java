package com.example.ad_auction_dashboard.RegressionTests;

import com.example.ad_auction_dashboard.logic.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for campaign database operations
 * Verifies saving, loading, and management of campaigns in the database
 */
public class CampaignDatabaseTest {

    private Campaign testCampaign;
    private CampaignMetrics testMetrics;
    private String testCampaignName;
    private int testUserId;
    private int savedCampaignId = -1;

    @BeforeEach
    void setUp() {
        // Create a unique campaign name for this test run
        testCampaignName = "Test Campaign " + UUID.randomUUID().toString().substring(0, 8);

        // Set up a simple test campaign
        ImpressionLog imp1 = new ImpressionLog("2023-03-01 10:00:00", "1001", "Male", "<25", "Medium", "News", "0.123456");
        ImpressionLog imp2 = new ImpressionLog("2023-03-01 10:30:00", "1002", "Female", "25-34", "High", "Shopping", "0.234567");
        ImpressionLog[] imps = {imp1, imp2};

        ClickLog c1 = new ClickLog("2023-03-01 10:05:00", "1001", "1.230000");
        ClickLog c2 = new ClickLog("2023-03-01 10:35:00", "1002", "1.450000");
        ClickLog[] cls = {c1, c2};

        ServerLog s1 = new ServerLog("2023-03-01 10:05:30", "1001", "2023-03-01 10:10:30", "3", "Yes");
        ServerLog s2 = new ServerLog("2023-03-01 10:35:30", "1002", "2023-03-01 10:40:30", "5", "No");
        ServerLog[] srvs = {s1, s2};

        testCampaign = new Campaign(imps, cls, srvs);
        testMetrics = new CampaignMetrics(testCampaign);

        // Create a test user if needed (or use an existing admin user)
        testUserId = ensureTestUser();
    }

    @AfterEach
    void tearDown() {
        // Clean up saved campaign if necessary
        if (savedCampaignId > 0) {
            CampaignDatabase.deleteCampaign(savedCampaignId);
            savedCampaignId = -1;
        }
    }

    /**
     * Helper method to ensure a test user exists and return its ID
     */
    private int ensureTestUser() {
        // Try to use the admin user
        UserDatabase.User adminUser = UserDatabase.getUser("admin");
        if (adminUser != null) {
            return adminUser.getId();
        }

        // If admin doesn't exist, create a test user
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String username = "testuser_" + uniqueId;
        String email = "test_" + uniqueId + "@example.com";
        String phone = "1" + System.currentTimeMillis() % 10000000000L;

        UserDatabase.addUser(username, email, phone, "password", "admin");
        UserDatabase.User user = UserDatabase.getUser(username);

        return user.getId();
    }

    @Test
    void testSaveAndLoadCampaign() {
        // Save the campaign to the database
        savedCampaignId = CampaignDatabase.saveCampaign(testMetrics, testCampaignName, testUserId);

        // Verify campaign was saved successfully
        assertTrue(savedCampaignId > 0, "Campaign should be saved with a valid ID");

        // Load the campaign from the database
        Campaign loadedCampaign = CampaignDatabase.loadCampaign(savedCampaignId);

        // Verify campaign was loaded successfully
        assertNotNull(loadedCampaign, "Loaded campaign should not be null");

        // Verify campaign data was preserved
        ImpressionLog[] originalImps = testCampaign.getImpressionLogs();
        ImpressionLog[] loadedImps = loadedCampaign.getImpressionLogs();
        assertEquals(originalImps.length, loadedImps.length, "Loaded campaign should have the same number of impression logs");

        ClickLog[] originalClicks = testCampaign.getClickLogs();
        ClickLog[] loadedClicks = loadedCampaign.getClickLogs();
        assertEquals(originalClicks.length, loadedClicks.length, "Loaded campaign should have the same number of click logs");

        ServerLog[] originalServer = testCampaign.getServerLogs();
        ServerLog[] loadedServer = loadedCampaign.getServerLogs();
        assertEquals(originalServer.length, loadedServer.length, "Loaded campaign should have the same number of server logs");
    }

    @Test
    void testGetAllCampaigns() {
        // Save the campaign to the database
        savedCampaignId = CampaignDatabase.saveCampaign(testMetrics, testCampaignName, testUserId);

        // Get all campaigns
        List<CampaignDatabase.CampaignInfo> campaigns = CampaignDatabase.getAllCampaigns();

        // Verify campaigns list is not empty
        assertFalse(campaigns.isEmpty(), "Campaigns list should not be empty");

        // Verify our test campaign is in the list
        boolean foundTestCampaign = campaigns.stream()
            .anyMatch(c -> c.getCampaignId() == savedCampaignId);
        assertTrue(foundTestCampaign, "Test campaign should be in the list of all campaigns");
    }

    @Test
    void testGetUserCampaigns() {
        // Save the campaign to the database
        savedCampaignId = CampaignDatabase.saveCampaign(testMetrics, testCampaignName, testUserId);

        // Get campaigns for this user
        List<CampaignDatabase.CampaignInfo> userCampaigns = CampaignDatabase.getUserCampaigns(testUserId);

        // Verify user campaigns list is not empty
        assertFalse(userCampaigns.isEmpty(), "User campaigns list should not be empty");

        // Verify our test campaign is in the list
        boolean foundTestCampaign = userCampaigns.stream()
            .anyMatch(c -> c.getCampaignId() == savedCampaignId);
        assertTrue(foundTestCampaign, "Test campaign should be in the list of user campaigns");
    }

    @Test
    void testDeleteCampaign() {
        // Save the campaign to the database
        savedCampaignId = CampaignDatabase.saveCampaign(testMetrics, testCampaignName, testUserId);

        // Verify campaign exists
        Campaign loadedCampaign = CampaignDatabase.loadCampaign(savedCampaignId);
        assertNotNull(loadedCampaign, "Campaign should exist after saving");

        // Delete the campaign
        boolean deleted = CampaignDatabase.deleteCampaign(savedCampaignId);
        assertTrue(deleted, "Campaign deletion should return true");

        // Try to load the deleted campaign
        Campaign deletedCampaign = CampaignDatabase.loadCampaign(savedCampaignId);
        assertNull(deletedCampaign, "Campaign should not exist after deletion");

        // Reset savedCampaignId since we've deleted it
        savedCampaignId = -1;
    }

    @Test
    void testCampaignAssignment() {
        // Save the campaign to the database
        savedCampaignId = CampaignDatabase.saveCampaign(testMetrics, testCampaignName, testUserId);

        // Create a new user to assign the campaign to
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String username = "assignee_" + uniqueId;
        UserDatabase.addUser(username, uniqueId + "@example.com", "9" + System.currentTimeMillis() % 10000000000L, "password", "viewer");
        UserDatabase.User assignee = UserDatabase.getUser(username);

        // Assign campaign to the new user
        boolean assigned = CampaignDatabase.assignCampaignToUser(savedCampaignId, assignee.getId(), testUserId);
        assertTrue(assigned, "Campaign should be assigned successfully");

        // Verify assignment using canUserAccessCampaign
        boolean hasAccess = CampaignDatabase.canUserAccessCampaign(assignee.getId(), savedCampaignId);
        assertTrue(hasAccess, "User should have access to the campaign after assignment");

        // Get users with access
        List<Integer> usersWithAccess = CampaignDatabase.getUsersWithAccess(savedCampaignId);
        assertTrue(usersWithAccess.contains(assignee.getId()), "User ID should be in the list of users with access");

        // Get campaigns for user - checking accessible campaigns properly
        List<CampaignDatabase.CampaignInfo> accessibleCampaigns = CampaignDatabase.getAccessibleCampaigns(assignee.getId());
        boolean campaignFound = accessibleCampaigns.stream()
            .anyMatch(info -> info.getCampaignId() == savedCampaignId);
        assertTrue(campaignFound, "Campaign should be in the list of user's accessible campaigns");

        // Remove assignment
        boolean removed = CampaignDatabase.removeCampaignFromUser(savedCampaignId, assignee.getId());
        assertTrue(removed, "Campaign assignment should be removed successfully");

        // Verify removal
        hasAccess = CampaignDatabase.canUserAccessCampaign(assignee.getId(), savedCampaignId);
        assertFalse(hasAccess, "User should not have access to the campaign after removal");

        // Clean up test user
        UserDatabase.deleteUser(assignee.getId());
    }

    @Test
    void testCampaignDateRange() {
        // Save the campaign to the database
        savedCampaignId = CampaignDatabase.saveCampaign(testMetrics, testCampaignName, testUserId);

        // Get saved campaign info
        List<CampaignDatabase.CampaignInfo> campaigns = CampaignDatabase.getAllCampaigns();
        CampaignDatabase.CampaignInfo savedCampaign = campaigns.stream()
            .filter(c -> c.getCampaignId() == savedCampaignId)
            .findFirst()
            .orElse(null);

        assertNotNull(savedCampaign, "Saved campaign should be found in database");

        // Verify the campaign date range was saved correctly
        assertNotNull(savedCampaign.getStartDate(), "Campaign start date should not be null");
        assertNotNull(savedCampaign.getEndDate(), "Campaign end date should not be null");

        // Get the original date range from the metrics
        LocalDateTime originalStart = testMetrics.getCampaignStartDate();
        LocalDateTime originalEnd = testMetrics.getCampaignEndDate();

        // Verify dates are the same
        assertEquals(originalStart.toLocalDate(), savedCampaign.getStartDate().toLocalDate(),
            "Saved campaign start date should match original");
        assertEquals(originalEnd.toLocalDate(), savedCampaign.getEndDate().toLocalDate(),
            "Saved campaign end date should match original");
    }

    @Test
    void testGetAccessibleCampaigns() {
        // Save the campaign to the database
        savedCampaignId = CampaignDatabase.saveCampaign(testMetrics, testCampaignName, testUserId);

        // Create a few test users with different roles
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);

        // Create an editor user
        String editorUsername = "editor_" + uniqueId;
        UserDatabase.addUser(editorUsername, "editor_" + uniqueId + "@example.com",
            "8" + System.currentTimeMillis() % 10000000000L, "password", "editor");
        UserDatabase.User editorUser = UserDatabase.getUser(editorUsername);

        // Create a viewer user
        String viewerUsername = "viewer_" + uniqueId;
        UserDatabase.addUser(viewerUsername, "viewer_" + uniqueId + "@example.com",
            "7" + System.currentTimeMillis() % 10000000000L, "password", "viewer");
        UserDatabase.User viewerUser = UserDatabase.getUser(viewerUsername);

        // Assign campaign to viewer
        CampaignDatabase.assignCampaignToUser(savedCampaignId, viewerUser.getId(), testUserId);

        // Test accessible campaigns for owner (testUserId)
        List<CampaignDatabase.CampaignInfo> ownerCampaigns = CampaignDatabase.getAccessibleCampaigns(testUserId);
        boolean ownerHasAccess = ownerCampaigns.stream()
            .anyMatch(c -> c.getCampaignId() == savedCampaignId);
        assertTrue(ownerHasAccess, "Owner should have access to their own campaign");

        // Test accessible campaigns for viewer (assigned)
        List<CampaignDatabase.CampaignInfo> viewerCampaigns = CampaignDatabase.getAccessibleCampaigns(viewerUser.getId());
        boolean viewerHasAccess = viewerCampaigns.stream()
            .anyMatch(c -> c.getCampaignId() == savedCampaignId);
        assertTrue(viewerHasAccess, "Viewer should have access to assigned campaign");

        // Test accessible campaigns for editor (not assigned)
        List<CampaignDatabase.CampaignInfo> editorCampaigns = CampaignDatabase.getAccessibleCampaigns(editorUser.getId());
        boolean editorHasAccess = editorCampaigns.stream()
            .anyMatch(c -> c.getCampaignId() == savedCampaignId);
        assertFalse(editorHasAccess, "Editor should not have access to unassigned campaign");

        // Clean up test users
        UserDatabase.deleteUser(editorUser.getId());
        UserDatabase.deleteUser(viewerUser.getId());
    }
}