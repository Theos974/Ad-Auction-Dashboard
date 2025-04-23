package com.example.ad_auction_dashboard.IntegrationTests;

import static org.junit.jupiter.api.Assertions.*;

import com.example.ad_auction_dashboard.logic.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CampaignComparisonIntegrationTest {

    private CampaignMetrics testMetrics1;
    private CampaignMetrics testMetrics2;
    private int savedCampaignId1 = -1;
    private int savedCampaignId2 = -1;
    private String testCampaignName1;
    private String testCampaignName2;
    private int testUserId;

    @BeforeEach
    void setUp() {
        CampaignDatabase.ensureDatabaseInitialized();
        UserSession.getInstance().logout();

        // Create unique campaign names
        testCampaignName1 = "Test Campaign 1-" + UUID.randomUUID().toString().substring(0, 8);
        testCampaignName2 = "Test Campaign 2-" + UUID.randomUUID().toString().substring(0, 8);

        // Set up first test campaign with specific metrics
        ImpressionLog imp1 = new ImpressionLog("2023-03-01 10:00:00", "1001", "Male", "<25", "Medium", "News", "0.123456");
        ClickLog click1 = new ClickLog("2023-03-01 10:05:00", "1001", "1.230000");
        ServerLog server1 = new ServerLog("2023-03-01 10:05:30", "1001", "2023-03-01 10:10:30", "3", "Yes");

        // Set up second test campaign with different metrics
        ImpressionLog imp2 = new ImpressionLog("2023-03-01 11:00:00", "1002", "Female", "35-44", "High", "Shopping", "0.234567");
        ClickLog click2 = new ClickLog("2023-03-01 11:05:00", "1002", "1.450000");
        ServerLog server2 = new ServerLog("2023-03-01 11:05:30", "1002", "2023-03-01 11:07:30", "1", "No");

        Campaign campaign1 = new Campaign(
            new ImpressionLog[]{imp1},
            new ClickLog[]{click1},
            new ServerLog[]{server1}
        );

        Campaign campaign2 = new Campaign(
            new ImpressionLog[]{imp2},
            new ClickLog[]{click2},
            new ServerLog[]{server2}
        );

        // Find a user for test
        UserDatabase.User adminUser = UserDatabase.getUser("admin");
        testUserId = adminUser != null ? adminUser.getId() : 1;

        // Mock user session
        UserSession.getInstance().setUser(adminUser);

        // Save both campaigns to the database
        testMetrics1 = new CampaignMetrics(campaign1);
        testMetrics2 = new CampaignMetrics(campaign2);

        savedCampaignId1 = CampaignDatabase.saveCampaign(testMetrics1, testCampaignName1, testUserId);
        savedCampaignId2 = CampaignDatabase.saveCampaign(testMetrics2, testCampaignName2, testUserId);
    }

    @Test
    void testCampaignListLoading() {
        // Test that both campaigns are accessible
        List<CampaignDatabase.CampaignInfo> accessibleCampaigns =
            CampaignDatabase.getAccessibleCampaigns(testUserId);

        assertNotNull(accessibleCampaigns, "Accessible campaigns list should not be null");
        assertTrue(accessibleCampaigns.size() >= 2, "Should have at least our 2 test campaigns");

        // Verify our test campaigns are in the list
        boolean foundCampaign1 = false;
        boolean foundCampaign2 = false;

        for (CampaignDatabase.CampaignInfo campaign : accessibleCampaigns) {
            if (campaign.getCampaignId() == savedCampaignId1) {
                foundCampaign1 = true;
            }
            if (campaign.getCampaignId() == savedCampaignId2) {
                foundCampaign2 = true;
            }
        }

        assertTrue(foundCampaign1, "Should find campaign 1 in accessible campaigns");
        assertTrue(foundCampaign2, "Should find campaign 2 in accessible campaigns");
    }

    @Test
    void testMetricsComparison() {
        // Test comparing the metrics between campaigns
        Map<String, Double> metrics1 = CampaignDatabase.getMetricsDirectlyFromDatabase(savedCampaignId1);
        Map<String, Double> metrics2 = CampaignDatabase.getMetricsDirectlyFromDatabase(savedCampaignId2);

        assertNotNull(metrics1, "Metrics for campaign 1 should not be null");
        assertNotNull(metrics2, "Metrics for campaign 2 should not be null");

        // Compare specific metrics
        assertEquals(1.0, metrics1.get("impressions"), "Campaign 1 should have 1 impression");
        assertEquals(1.0, metrics2.get("impressions"), "Campaign 2 should have 1 impression");

        // Campaign 1 has a conversion, campaign 2 doesn't
        assertEquals(1.0, metrics1.get("conversions"), "Campaign 1 should have 1 conversion");
        assertEquals(0.0, metrics2.get("conversions"), "Campaign 2 should have 0 conversions");

        // Campaign 2 has a bounce, campaign 1 doesn't (based on the log setup)
        assertEquals(1.0, metrics2.get("bounces"), "Campaign 2 should have 1 bounce");
    }

    @Test
    void testGetCampaignDetailsForComparison() {
        // Test getting detailed campaign info for comparison
        CampaignDatabase.CampaignInfo info1 = CampaignDatabase.getCampaignById(savedCampaignId1);
        CampaignDatabase.CampaignInfo info2 = CampaignDatabase.getCampaignById(savedCampaignId2);

        assertNotNull(info1, "Campaign 1 info should not be null");
        assertNotNull(info2, "Campaign 2 info should not be null");

        // The campaigns should have the same date range
        assertEquals(info1.getStartDate().toLocalDate(), info2.getStartDate().toLocalDate(),
            "Both campaigns should have the same start date");

        // They should have different bounce thresholds in this test (default values)
        assertEquals(1, info1.getBouncePagesThreshold(), "Campaign 1 bounce pages threshold should be 1");
        assertEquals(1, info2.getBouncePagesThreshold(), "Campaign 2 bounce pages threshold should be 1");
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