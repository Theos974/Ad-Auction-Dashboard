package com.example.ad_auction_dashboard.ComponentTests;

import static org.junit.jupiter.api.Assertions.*;
import com.example.ad_auction_dashboard.logic.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class CampaignDatabaseComponentTest {

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
        ImpressionLog imp = new ImpressionLog("2023-03-01 10:00:00", "1001", "Male", "<25", "Medium", "News", "0.123456");
        ClickLog click = new ClickLog("2023-03-01 10:05:00", "1001", "1.230000");
        ServerLog server = new ServerLog("2023-03-01 10:05:30", "1001", "2023-03-01 10:10:30", "3", "Yes");

        testCampaign = new Campaign(
            new ImpressionLog[]{imp},
            new ClickLog[]{click},
            new ServerLog[]{server}
        );
        testMetrics = new CampaignMetrics(testCampaign);

        // Find a user for test (admin user should always exist)
        UserDatabase.User adminUser = UserDatabase.getUser("admin");
        testUserId = adminUser != null ? adminUser.getId() : 1;
    }

    @AfterEach
    void tearDown() {
        // Clean up saved campaign if necessary
        if (savedCampaignId > 0) {
            CampaignDatabase.deleteCampaign(savedCampaignId);
            savedCampaignId = -1;
        }
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
}