package com.example.ad_auction_dashboard.ComponentTests;

import static org.junit.jupiter.api.Assertions.*;

import com.example.ad_auction_dashboard.logic.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CampaignComparisonComponentTest {

    private CampaignMetrics mockMetrics;
    private Campaign testCampaign;
    private int savedCampaignId1 = -1;
    private int savedCampaignId2 = -1;
    private String testCampaignName1;
    private String testCampaignName2;
    private int testUserId;

    @BeforeEach
    void setUp() {
        CampaignDatabase.ensureDatabaseInitialized();

        // Create unique campaign names
        testCampaignName1 = "Test Campaign 1-" + UUID.randomUUID().toString().substring(0, 8);
        testCampaignName2 = "Test Campaign 2-" + UUID.randomUUID().toString().substring(0, 8);

        // Set up test campaigns with slightly different metrics
        ImpressionLog imp1 = new ImpressionLog("2023-03-01 10:00:00", "1001", "Male", "<25", "Medium", "News", "0.123456");
        ImpressionLog imp2 = new ImpressionLog("2023-03-01 11:00:00", "1002", "Female", "35-44", "High", "Shopping", "0.234567");

        ClickLog click1 = new ClickLog("2023-03-01 10:05:00", "1001", "1.230000");
        ClickLog click2 = new ClickLog("2023-03-01 11:05:00", "1002", "1.450000");

        ServerLog server1 = new ServerLog("2023-03-01 10:05:30", "1001", "2023-03-01 10:10:30", "3", "Yes");
        ServerLog server2 = new ServerLog("2023-03-01 11:05:30", "1002", "2023-03-01 11:10:30", "4", "No");

        // Create first campaign with first set of logs
        Campaign campaign1 = new Campaign(
            new ImpressionLog[]{imp1},
            new ClickLog[]{click1},
            new ServerLog[]{server1}
        );

        // Create second campaign with second set of logs
        Campaign campaign2 = new Campaign(
            new ImpressionLog[]{imp2},
            new ClickLog[]{click2},
            new ServerLog[]{server2}
        );

        // Find a user for test (admin user should always exist)
        UserDatabase.User adminUser = UserDatabase.getUser("admin");
        testUserId = adminUser != null ? adminUser.getId() : 1;

        // Save the first campaign
        CampaignMetrics metrics1 = new CampaignMetrics(campaign1);
        savedCampaignId1 = CampaignDatabase.saveCampaign(metrics1, testCampaignName1, testUserId);

        // Save the second campaign
        CampaignMetrics metrics2 = new CampaignMetrics(campaign2);
        savedCampaignId2 = CampaignDatabase.saveCampaign(metrics2, testCampaignName2, testUserId);

        // Use the first campaign's metrics for our tests
        mockMetrics = metrics1;
    }

    @Test
    void testGetMetricsDirectlyFromDatabase() {
        // Verify metrics are stored and can be retrieved directly
        Map<String, Double> metrics = CampaignDatabase.getMetricsDirectlyFromDatabase(savedCampaignId1);

        assertNotNull(metrics, "Metrics should not be null");
        assertTrue(metrics.containsKey("impressions"), "Metrics should contain impression count");
        assertTrue(metrics.containsKey("clicks"), "Metrics should contain click count");
        assertTrue(metrics.containsKey("totalCost"), "Metrics should contain total cost");
        assertTrue(metrics.containsKey("ctr"), "Metrics should contain CTR");

        // Verify values match expected values from our test data
        assertEquals(1.0, metrics.get("impressions"), "Should have 1 impression");
        assertEquals(1.0, metrics.get("clicks"), "Should have 1 click");
    }

    @Test
    void testGetCampaignById() {
        // Test retrieving detailed campaign info by ID
        CampaignDatabase.CampaignInfo campaignInfo = CampaignDatabase.getCampaignById(savedCampaignId1);

        assertNotNull(campaignInfo, "Campaign info should not be null");
        assertEquals(testCampaignName1, campaignInfo.getCampaignName(), "Campaign name should match");
        assertNotNull(campaignInfo.getStartDate(), "Start date should not be null");
        assertNotNull(campaignInfo.getEndDate(), "End date should not be null");

        // Verify bounce thresholds
        assertEquals(mockMetrics.getBouncePagesThreshold(), campaignInfo.getBouncePagesThreshold(),
            "Bounce pages threshold should match");
        assertEquals(mockMetrics.getBounceSecondsThreshold(), campaignInfo.getBounceSecondsThreshold(),
            "Bounce seconds threshold should match");
    }

    @Test
    void testAccessibleCampaigns() {
        // Test that the admin user can access both campaigns
        assertTrue(CampaignDatabase.canUserAccessCampaign(testUserId, savedCampaignId1),
            "Admin should have access to campaign 1");
        assertTrue(CampaignDatabase.canUserAccessCampaign(testUserId, savedCampaignId2),
            "Admin should have access to campaign 2");
    }

    @Test
    void cleanUp() {
        // Clean up saved campaigns
        if (savedCampaignId1 > 0) {
            CampaignDatabase.deleteCampaign(savedCampaignId1);
        }
        if (savedCampaignId2 > 0) {
            CampaignDatabase.deleteCampaign(savedCampaignId2);
        }
    }
}