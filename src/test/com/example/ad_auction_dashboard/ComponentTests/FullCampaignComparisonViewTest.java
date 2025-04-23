package com.example.ad_auction_dashboard.ComponentTests;

import static org.junit.jupiter.api.Assertions.*;

import com.example.ad_auction_dashboard.logic.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FullCampaignComparisonViewTest {

    private CampaignMetrics testMetrics;
    private int savedCampaignId = -1;
    private String testCampaignName;
    private int testUserId;

    @BeforeEach
    void setUp() {
        CampaignDatabase.ensureDatabaseInitialized();

        // Create a unique campaign name
        testCampaignName = "Test Campaign " + UUID.randomUUID().toString().substring(0, 8);

        // Set up a test campaign with known metrics
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

        // Save the campaign and track its metrics
        testMetrics = new CampaignMetrics(campaign);
        savedCampaignId = CampaignDatabase.saveCampaign(testMetrics, testCampaignName, testUserId);

        // Make sure the metrics were pre-computed and saved
        assertNotNull(CampaignDatabase.getMetricsDirectlyFromDatabase(savedCampaignId),
            "Pre-computed metrics should be stored in database");
    }

    @Test
    void testMetricsRetrieval() {
        // Test that metrics can be retrieved in the format expected by the comparison view
        Map<String, Double> metrics = CampaignDatabase.getMetricsDirectlyFromDatabase(savedCampaignId);

        assertNotNull(metrics, "Should retrieve metrics");

        // Check all the metrics we expect to compare
        assertTrue(metrics.containsKey("impressions"));
        assertTrue(metrics.containsKey("clicks"));
        assertTrue(metrics.containsKey("uniques"));
        assertTrue(metrics.containsKey("bounces"));
        assertTrue(metrics.containsKey("conversions"));
        assertTrue(metrics.containsKey("totalCost"));
        assertTrue(metrics.containsKey("ctr"));
        assertTrue(metrics.containsKey("cpc"));
        assertTrue(metrics.containsKey("cpa"));
        assertTrue(metrics.containsKey("cpm"));
        assertTrue(metrics.containsKey("bounceRate"));
    }

    @Test
    void testCampaignInfoRetrieval() {
        // Test getting the campaign info needed for the comparison view
        CampaignDatabase.CampaignInfo info = CampaignDatabase.getCampaignById(savedCampaignId);

        assertNotNull(info, "Should retrieve campaign info");
        assertEquals(testCampaignName, info.getCampaignName(), "Campaign name should match");

        // Check date range info
        LocalDateTime startDate = info.getStartDate();
        LocalDateTime endDate = info.getEndDate();
        assertNotNull(startDate, "Start date should be set");
        assertNotNull(endDate, "End date should be set");

        // These should match the dates in our test data
        assertEquals(2023, startDate.getYear());
        assertEquals(3, startDate.getMonthValue());
        assertEquals(1, startDate.getDayOfMonth());
    }

    @Test
    void cleanUp() {
        // Clean up saved campaign
        if (savedCampaignId > 0) {
            CampaignDatabase.deleteCampaign(savedCampaignId);
        }
    }
}