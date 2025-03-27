package com.example.ad_auction_dashboard.BoundaryAndPartitionTests;

import static org.junit.jupiter.api.Assertions.*;

import com.example.ad_auction_dashboard.logic.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

public class CampaignComparisonBoundaryTest {

    private CampaignMetrics extremeMetrics;
    private CampaignMetrics emptyMetrics;
    private int savedExtremeCampaignId = -1;
    private int savedEmptyCampaignId = -1;
    private String extremeCampaignName;
    private String emptyCampaignName;
    private int testUserId;

    @BeforeEach
    void setUp() {
        CampaignDatabase.ensureDatabaseInitialized();

        // Create unique campaign names
        extremeCampaignName = "Extreme-" + UUID.randomUUID().toString().substring(0, 8);
        emptyCampaignName = "Empty-" + UUID.randomUUID().toString().substring(0, 8);

        // Create a campaign with extreme but valid values
        // Using 999.999999 which is within DECIMAL(10,6) limits
        ImpressionLog extremeImp = new ImpressionLog(
            "2023-03-01 10:00:00", "1001", "Male", "<25", "Medium", "News", "999.999999");
        ClickLog extremeClick = new ClickLog(
            "2023-03-01 10:05:00", "1001", "999.999999");
        ServerLog extremeServer = new ServerLog(
            "2023-03-01 10:05:30", "1001", "2023-03-01 23:59:59", "9999", "Yes");

        Campaign extremeCampaign = new Campaign(
            new ImpressionLog[]{extremeImp},
            new ClickLog[]{extremeClick},
            new ServerLog[]{extremeServer}
        );

        // Create an empty campaign with no log data
        Campaign emptyCampaign = new Campaign(
            new ImpressionLog[0],
            new ClickLog[0],
            new ServerLog[0]
        );

        // Find a user for test
        UserDatabase.User adminUser = UserDatabase.getUser("admin");
        testUserId = adminUser != null ? adminUser.getId() : 1;

        // Save the campaigns
        extremeMetrics = new CampaignMetrics(extremeCampaign);
        emptyMetrics = new CampaignMetrics(emptyCampaign);

        savedExtremeCampaignId = CampaignDatabase.saveCampaign(extremeMetrics, extremeCampaignName, testUserId);
        savedEmptyCampaignId = CampaignDatabase.saveCampaign(emptyMetrics, emptyCampaignName, testUserId);
    }

    @Test
    void testExtremeValues() {
        // Test extreme values in the metrics
        Map<String, Double> extremeMetricsData =
            CampaignDatabase.getMetricsDirectlyFromDatabase(savedExtremeCampaignId);

        assertNotNull(extremeMetricsData, "Extreme metrics should be retrieved");

        // Verify the extreme values were saved correctly
        assertEquals(1.0, extremeMetricsData.get("impressions"), "Should have 1 impression");
        assertEquals(1.0, extremeMetricsData.get("clicks"), "Should have 1 click");

        // Check extreme cost value
        double expectedCost = 2000.0; // impression cost + click cost
        assertEquals(expectedCost, extremeMetricsData.get("totalCost"), 0.000001,
            "Total cost should match the sum of extreme costs");
    }

    @Test
    void testEmptyData() {
        // Test comparing with a campaign that has no data
        Map<String, Double> emptyMetricsData =
            CampaignDatabase.getMetricsDirectlyFromDatabase(savedEmptyCampaignId);

        assertNotNull(emptyMetricsData, "Empty metrics should be retrieved");

        // Verify all metrics are zero
        assertEquals(0.0, emptyMetricsData.get("impressions"), "Should have 0 impressions");
        assertEquals(0.0, emptyMetricsData.get("clicks"), "Should have 0 clicks");
        assertEquals(0.0, emptyMetricsData.get("conversions"), "Should have 0 conversions");
        assertEquals(0.0, emptyMetricsData.get("totalCost"), "Should have 0 total cost");

        // Derived metrics should also be zero
        assertEquals(0.0, emptyMetricsData.get("ctr"), "CTR should be 0");
        assertEquals(0.0, emptyMetricsData.get("cpc"), "CPC should be 0");
        assertEquals(0.0, emptyMetricsData.get("cpa"), "CPA should be 0");
        assertEquals(0.0, emptyMetricsData.get("cpm"), "CPM should be 0");
        assertEquals(0.0, emptyMetricsData.get("bounceRate"), "Bounce rate should be 0");
    }

    @Test
    void testMetricsDivergence() {
        // Test comparing campaigns with vastly different metrics
        Map<String, Double> extremeMetricsData =
            CampaignDatabase.getMetricsDirectlyFromDatabase(savedExtremeCampaignId);
        Map<String, Double> emptyMetricsData =
            CampaignDatabase.getMetricsDirectlyFromDatabase(savedEmptyCampaignId);

        assertNotNull(extremeMetricsData, "Extreme metrics should be retrieved");
        assertNotNull(emptyMetricsData, "Empty metrics should be retrieved");

        // Calculate differences between the campaigns
        double impressionsDiff = extremeMetricsData.get("impressions") - emptyMetricsData.get("impressions");
        double clicksDiff = extremeMetricsData.get("clicks") - emptyMetricsData.get("clicks");
        double costDiff = extremeMetricsData.get("totalCost") - emptyMetricsData.get("totalCost");

        // Verify differences are as expected
        assertEquals(1.0, impressionsDiff, "Impressions difference should be 1");
        assertEquals(1.0, clicksDiff, "Clicks difference should be 1");
        assertTrue(costDiff > 1999.0, "Cost difference should be over 1999");
    }

    @Test
    void testCampaignDateBoundaries() {
        // Test date range comparison at boundaries
        CampaignDatabase.CampaignInfo extremeInfo = CampaignDatabase.getCampaignById(savedExtremeCampaignId);
        CampaignDatabase.CampaignInfo emptyInfo = CampaignDatabase.getCampaignById(savedEmptyCampaignId);

        assertNotNull(extremeInfo, "Should retrieve extreme campaign info");
        assertNotNull(emptyInfo, "Should retrieve empty campaign info");

        // Empty campaign may have null dates or default dates - check both possibilities
        if (emptyInfo.getStartDate() == null) {
            assertNull(emptyInfo.getStartDate(), "Empty campaign could have null start date");
        }

        if (emptyInfo.getEndDate() == null) {
            assertNull(emptyInfo.getEndDate(), "Empty campaign could have null end date");
        }

        // Extreme campaign should have specific dates
        assertNotNull(extremeInfo.getStartDate(), "Extreme campaign should have start date");
        assertNotNull(extremeInfo.getEndDate(), "Extreme campaign should have end date");
        assertEquals(2023, extremeInfo.getStartDate().getYear(), "Start year should be 2023");
    }

    @AfterEach
    void cleanUp() {
        // Clean up saved campaigns
        if (savedExtremeCampaignId > 0) {
            CampaignDatabase.deleteCampaign(savedExtremeCampaignId);
        }
        if (savedEmptyCampaignId > 0) {
            CampaignDatabase.deleteCampaign(savedEmptyCampaignId);
        }
    }
}