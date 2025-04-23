package com.example.ad_auction_dashboard.RegressionTests;

import static org.junit.jupiter.api.Assertions.*;

import com.example.ad_auction_dashboard.logic.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

public class CampaignComparisonDatabaseRegressionTest {

    private int testCampaignId = -1;
    private String testCampaignName;
    private int testUserId;
    private CampaignMetrics testMetrics;

    @BeforeEach
    void setUp() {
        CampaignDatabase.ensureDatabaseInitialized();

        // Generate a unique campaign name
        testCampaignName = "DB-Test-" + UUID.randomUUID().toString().substring(0, 8);

        // Create a test campaign with known values
        ImpressionLog imp = new ImpressionLog("2023-03-01 10:00:00", "1001", "Male", "<25", "Medium", "News", "0.123456");
        ClickLog click = new ClickLog("2023-03-01 10:05:00", "1001", "1.230000");
        ServerLog server = new ServerLog("2023-03-01 10:05:30", "1001", "2023-03-01 10:10:30", "3", "Yes");

        Campaign campaign = new Campaign(
            new ImpressionLog[]{imp},
            new ClickLog[]{click},
            new ServerLog[]{server}
        );

        // Find a user for test
        UserDatabase.User adminUser = UserDatabase.getUser("admin");
        testUserId = adminUser != null ? adminUser.getId() : 1;

        // Create metrics and save campaign
        testMetrics = new CampaignMetrics(campaign);
        testCampaignId = CampaignDatabase.saveCampaign(testMetrics, testCampaignName, testUserId);
    }

    @Test
    void testSaveAndRetrievePrecomputedMetrics() {
        // Verify the metrics were saved and can be retrieved
        Map<String, Double> metrics = CampaignDatabase.getMetricsDirectlyFromDatabase(testCampaignId);

        assertNotNull(metrics, "Metrics should be retrievable");
        assertEquals(1.0, metrics.get("impressions"), "Should have 1 impression");
        assertEquals(1.0, metrics.get("clicks"), "Should have 1 click");
        assertEquals(1.0, metrics.get("conversions"), "Should have 1 conversion");

        // Check that the total cost is correct
        double expectedCost = 0.123456 + 1.23; // impression cost + click cost
        assertEquals(expectedCost, metrics.get("totalCost"), 0.000001,
            "Total cost should match expected value");

        // Check derived metrics
        assertEquals(1.0, metrics.get("ctr"), "CTR should be 1.0");
        assertEquals(expectedCost, metrics.get("cpc"), 0.000001, "CPC should match total cost");
        assertEquals(expectedCost, metrics.get("cpa"), 0.000001, "CPA should match total cost");
        assertEquals(expectedCost * 1000, metrics.get("cpm"), 0.001, "CPM should be total cost * 1000");
    }

    @Test
    void testCacheClearingForComparison() {
        // Test cache clearing functionality which is important for comparisons

        // First, let's access the campaign to populate the cache
        CampaignDatabase.CampaignInfo firstAccess = CampaignDatabase.getCampaignById(testCampaignId);
        assertNotNull(firstAccess, "First access should return campaign info");

        // Now clear the caches
        CampaignDatabase.clearCaches();

        // Access again - should still work after cache clearing
        CampaignDatabase.CampaignInfo secondAccess = CampaignDatabase.getCampaignById(testCampaignId);
        assertNotNull(secondAccess, "Second access after cache clearing should still work");

        // Data should be the same
        assertEquals(firstAccess.getCampaignName(), secondAccess.getCampaignName(),
            "Campaign name should be the same after cache clearing");

        // Verify metrics still accessible after cache clearing
        Map<String, Double> metrics = CampaignDatabase.getMetricsDirectlyFromDatabase(testCampaignId);
        assertNotNull(metrics, "Metrics should still be accessible after cache clearing");
    }

    @Test
    void testDeleteCampaignWithPrecomputedMetrics() {
        // Test deleting a campaign with precomputed metrics

        // First verify the metrics exist
        Map<String, Double> metricsBeforeDelete =
            CampaignDatabase.getMetricsDirectlyFromDatabase(testCampaignId);
        assertNotNull(metricsBeforeDelete, "Metrics should exist before deletion");

        // Delete the campaign
        boolean deleted = CampaignDatabase.deleteCampaign(testCampaignId);
        assertTrue(deleted, "Campaign should be deleted successfully");

        // After deletion, metrics should no longer be accessible
        Map<String, Double> metricsAfterDelete =
            CampaignDatabase.getMetricsDirectlyFromDatabase(testCampaignId);
        assertTrue(metricsAfterDelete == null || metricsAfterDelete.isEmpty(),
            "Metrics should be null or empty after campaign deletion");
        // Reset test campaign ID since we've deleted it
        testCampaignId = -1;
    }

    @Test
    void cleanUp() {
        // Clean up saved campaign if not already deleted
        if (testCampaignId > 0) {
            CampaignDatabase.deleteCampaign(testCampaignId);
            testCampaignId = -1;
        }
    }
}