package com.example.ad_auction_dashboard.BoundaryAndPartitionTests;

import static org.junit.jupiter.api.Assertions.*;
import com.example.ad_auction_dashboard.logic.*;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for boundary and extreme value conditions in metrics calculations
 */
public class MetricsExtremeValueTest {

    @Test
    @DisplayName("Test extremely large values")
    public void testExtremelyLargeValues() {
        // Create test data with very large values
        ImpressionLog imp = new ImpressionLog("2025-03-16 05:28:06", "1", "Male", "25-34", "High", "Blog", "1000000.000000");
        ImpressionLog[] imps = {imp};

        ClickLog c = new ClickLog("2025-03-16 05:28:30", "1", "2000000.000000");
        ClickLog[] cls = {c};

        ServerLog s = new ServerLog("2025-03-16 05:28:40", "1", "2025-03-16 05:30:00", "999999", "Yes");
        ServerLog[] srvs = {s};

        Campaign campaign = new Campaign(imps, cls, srvs);
        CampaignMetrics metrics = new CampaignMetrics(campaign);

        // Verify metrics with large values
        assertEquals(1, metrics.getNumberOfImpressions(), "Should count 1 impression");
        assertEquals(1, metrics.getNumberOfClicks(), "Should count 1 click");
        assertEquals(1, metrics.getNumberOfUniques(), "Should count 1 unique");
        assertEquals(1, metrics.getNumberOfConversions(), "Should count 1 conversion");

        // Expect large cost value (sum of impression and click costs)
        double expectedCost = 1000000.000000 + 2000000.000000;
        assertEquals(expectedCost, metrics.getTotalCost(), 1e-6, "Total cost should be sum of large values");

        // Derived metrics should handle large values
        assertEquals(1.0, metrics.getCTR(), 1e-6, "CTR calculation should handle large values");
        assertEquals(expectedCost, metrics.getCPA(), 1e-6, "CPA calculation should handle large values");
        assertEquals(expectedCost, metrics.getCPC(), 1e-6, "CPC calculation should handle large values");
        assertEquals(expectedCost * 1000, metrics.getCPM(), 1e-6, "CPM calculation should handle large values");
    }

    @Test
    @DisplayName("Test with Empty Arrays")
    public void testEmptyArrays() {
        Campaign campaign = new Campaign(new ImpressionLog[0], new ClickLog[0], new ServerLog[0]);
        CampaignMetrics metrics = new CampaignMetrics(campaign);

        assertEquals(0, metrics.getNumberOfImpressions());
        assertEquals(0, metrics.getNumberOfClicks());
        assertEquals(0, metrics.getNumberOfUniques());
        assertEquals(0, metrics.getTotalCost(), 1e-6);
        assertEquals(0, metrics.getCTR(), 1e-6);
        assertEquals(0, metrics.getBounceRate(), 1e-6);
        assertEquals(0, metrics.getCPA(), 1e-6);
        assertEquals(0, metrics.getCPC(), 1e-6);
    }

    @Test
    @DisplayName("Test with Null Arrays")
    public void testNullArrays() {
        Campaign campaign = new Campaign(null, null, null);
        CampaignMetrics metrics = assertDoesNotThrow(() -> new CampaignMetrics(campaign));

        // Expect metrics to be 0 for all fields.
        assertEquals(0, metrics.getNumberOfImpressions());
        assertEquals(0, metrics.getNumberOfClicks());
        assertEquals(0, metrics.getNumberOfUniques());
        assertEquals(0, metrics.getTotalCost(), 1e-6);
        assertEquals(0, metrics.getCTR(), 1e-6);
        assertEquals(0, metrics.getBounceRate(), 1e-6);
        assertEquals(0, metrics.getCPA(), 1e-6);
        assertEquals(0, metrics.getCPC(), 1e-6);
    }

    @Test
    @DisplayName("Test Bounce Calculation Boundary")
    public void testBounceBoundary() {
        // Create a server log where the time difference is exactly at the threshold boundary
        ServerLog s = new ServerLog("2025-03-16 05:28:00", "1", "2025-03-16 05:28:04", "2", "No");
        ServerLog[] srvs = {s};
        // Create dummy click log for denominator
        ClickLog c = new ClickLog("2025-03-16 05:28:01", "1", "0.50");
        ClickLog[] cls = {c};
        ImpressionLog[] imps = new ImpressionLog[0];

        Campaign campaign = new Campaign(imps, cls, srvs);
        CampaignMetrics metrics = new CampaignMetrics(campaign);

        assertEquals(1, metrics.getNumberOfBounces());
        assertEquals(1.0, metrics.getBounceRate(), 1e-6);
    }

    @Test
    @DisplayName("Test campaign date range detection with extreme dates")
    public void testCampaignDateRangeExtreme() {
        // Create logs with extremely separated dates
        ImpressionLog imp1 = new ImpressionLog("2020-01-01 00:00:00", "1", "Male", "25-34", "High", "Blog", "0.001632");
        ImpressionLog imp2 = new ImpressionLog("2030-12-31 23:59:59", "2", "Female", "35-44", "Medium", "News", "0.002000");
        ImpressionLog[] imps = {imp1, imp2};

        ClickLog c1 = new ClickLog("2025-03-10 05:28:30", "1", "0.50"); // Date in the middle
        ClickLog[] cls = {c1};

        ServerLog s = new ServerLog("2025-03-18 05:28:40", "1", "2025-03-18 05:30:00", "3", "Yes");
        ServerLog[] srvs = {s};

        Campaign campaign = new Campaign(imps, cls, srvs);
        CampaignMetrics metrics = new CampaignMetrics(campaign);

        // Verify start date is earliest date across all logs
        LocalDateTime expectedStart = LocalDateTime.of(2020, 1, 1, 0, 0, 0);
        assertEquals(expectedStart, metrics.getCampaignStartDate(),
            "Campaign start date should be from earliest log (impression log imp1)");

        // Verify end date is latest date across all logs
        LocalDateTime expectedEnd = LocalDateTime.of(2030, 12, 31, 23, 59, 59);
        assertEquals(expectedEnd, metrics.getCampaignEndDate(),
            "Campaign end date should be from latest log (impression log imp2)");
    }

    @Test
    @DisplayName("Test setting valid bounce criteria at boundary values")
    public void testValidBounceCriteriaBoundary() {
        // Use a server log where bounce depends on threshold
        ServerLog s = new ServerLog("2025-03-16 05:28:00", "1", "2025-03-16 05:28:05", "3", "No");
        ServerLog[] srvs = {s};
        ClickLog c = new ClickLog("2025-03-16 05:28:01", "1", "0.50");
        ClickLog[] cls = {c};
        ImpressionLog[] imps = new ImpressionLog[0];
        Campaign campaign = new Campaign(imps, cls, srvs);
        CampaignMetrics metrics = new CampaignMetrics(campaign);

        // Initially, with default criteria (pages <= 1, seconds <= 4),
        // the log should not count as a bounce.
        assertEquals(0, metrics.getNumberOfBounces(), "Initially, bounce count should be 0.");

        // Now, change the bounce criteria to exactly match the server log values
        metrics.setBounceCriteria(3, 5);  // Exactly match the pages viewed and seconds

        // Since there is only one log that matches exactly the boundary, we expect it to be a bounce
        assertEquals(1, metrics.getNumberOfBounces(), "After setting exact threshold, bounce count should be 1.");
        assertEquals(1.0, metrics.getBounceRate(), 1e-6, "Bounce rate should be 1.0 after updating threshold.");
    }

    @Test
    @DisplayName("Test invalid bounce criteria boundaries")
    public void testInvalidBounceCriteriaBoundary() {
        Campaign campaign = new Campaign(new ImpressionLog[0], new ClickLog[0], new ServerLog[0]);
        CampaignMetrics metrics = new CampaignMetrics(campaign);

        // Zero values (edge of valid range)
        assertDoesNotThrow(() -> {
            metrics.setBounceCriteria(0, 0);
        }, "Setting zero thresholds should be valid");

        // Negative pages threshold should throw exception.
        assertThrows(IllegalArgumentException.class, () -> {
            metrics.setBounceCriteria(-1, 4);
        }, "Setting negative pages threshold should throw IllegalArgumentException.");

        // Negative seconds threshold should throw exception.
        assertThrows(IllegalArgumentException.class, () -> {
            metrics.setBounceCriteria(1, -4);
        }, "Setting negative seconds threshold should throw IllegalArgumentException.");

        // Extremely large value (should be valid)
        assertDoesNotThrow(() -> {
            metrics.setBounceCriteria(Integer.MAX_VALUE, Integer.MAX_VALUE);
        }, "Setting extremely large thresholds should be valid");
    }
}