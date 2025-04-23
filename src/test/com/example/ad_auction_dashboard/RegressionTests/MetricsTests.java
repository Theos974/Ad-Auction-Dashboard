package com.example.ad_auction_dashboard.RegressionTests;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.ad_auction_dashboard.logic.Campaign;
import com.example.ad_auction_dashboard.logic.CampaignMetrics;
import com.example.ad_auction_dashboard.logic.ClickLog;
import com.example.ad_auction_dashboard.logic.ImpressionLog;
import com.example.ad_auction_dashboard.logic.ServerLog;
import com.example.ad_auction_dashboard.logic.TimeFilteredMetrics;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


public class MetricsTests {


    private Campaign createCampaign(ImpressionLog[] imps, ClickLog[] cls, ServerLog[] srvs) {
        Campaign campaign = new Campaign(imps,cls,srvs);
        return campaign;
    }

    @Test
    public void testCalculateImpressions() {
        ImpressionLog imp1 = new ImpressionLog("2025-03-16 05:28:06", "1", "Male", "25-34", "High", "Blog", "0.001632");
        ImpressionLog imp2 = new ImpressionLog("2025-03-16 05:30:06", "2", "Female", "35-44", "Medium", "News", "0.002000");
        ImpressionLog[] imps = {imp1, imp2};

        // Create empty arrays for other logs
        ClickLog[] cls = new ClickLog[0];
        ServerLog[] srvs = new ServerLog[0];

        Campaign campaign = new Campaign(imps, cls, srvs);
        CampaignMetrics metrics = new CampaignMetrics(campaign);

        assertEquals(2, metrics.getNumberOfImpressions());
    }

    @Test
    public void testCalculateClicksAndUniques() {
        ClickLog c1 = new ClickLog("2025-03-16 05:29:00", "1", "0.50");
        ClickLog c2 = new ClickLog("2025-03-16 05:29:30", "2", "0.75");
        ClickLog c3 = new ClickLog("2025-03-16 05:30:00", "1", "0.60");
        ClickLog[] cls = {c1, c2, c3};

        ImpressionLog[] imps = new ImpressionLog[0];
        ServerLog[] srvs = new ServerLog[0];

        Campaign campaign = new Campaign(imps, cls, srvs);
        CampaignMetrics metrics = new CampaignMetrics(campaign);

        assertEquals(3, metrics.getNumberOfClicks());
        // Unique IDs: "1" and "2" => 2 unique clicks
        assertEquals(2, metrics.getNumberOfUniques());
    }

    @Test
    public void testCalculateConversions() {
        // Create two server logs. Only one has conversion = true.
        ServerLog s1 = new ServerLog("2025-03-16 05:28:00", "1", "2025-03-16 05:29:00", "3", "Yes");
        ServerLog s2 = new ServerLog("2025-03-16 05:30:00", "2", "2025-03-16 05:31:00", "2", "No");
        ServerLog[] srvs = {s1, s2};

        ImpressionLog[] imps = new ImpressionLog[0];
        ClickLog[] cls = new ClickLog[0];

        Campaign campaign = new Campaign(imps, cls, srvs);
        CampaignMetrics metrics = new CampaignMetrics(campaign);

        // Only s1 qualifies as a conversion
        assertEquals(1, metrics.getNumberOfConversions());
    }

    @Test
    public void testCalculateTotalCost() {
        ImpressionLog imp1 = new ImpressionLog("2025-03-16 05:28:06", "1", "Male", "25-34", "High", "Blog", "0.001632");
        ImpressionLog imp2 = new ImpressionLog("2025-03-16 05:30:06", "2", "Female", "35-44", "Medium", "News", "0.002000");
        ImpressionLog[] imps = {imp1, imp2};

        ClickLog c1 = new ClickLog("2025-03-16 05:29:00", "1", "0.50");
        ClickLog c2 = new ClickLog("2025-03-16 05:29:30", "2", "0.75");
        ClickLog[] cls = {c1, c2};

        ServerLog[] srvs = new ServerLog[0];

        Campaign campaign = new Campaign(imps, cls, srvs);
        CampaignMetrics metrics = new CampaignMetrics(campaign);

        double expectedCost = 0.001632 + 0.002000 + 0.50 + 0.75;
        assertEquals(expectedCost, metrics.getTotalCost(), 1e-6);
    }

    @Test
    public void testCalculateCTR() {
        ImpressionLog imp1 = new ImpressionLog("2025-03-16 05:28:06", "1", "Male", "25-34", "High", "Blog", "0.001632");
        ImpressionLog imp2 = new ImpressionLog("2025-03-16 05:30:06", "2", "Female", "35-44", "Medium", "News", "0.002000");
        ImpressionLog[] imps = {imp1, imp2};

        ClickLog c1 = new ClickLog("2025-03-16 05:29:00", "1", "0.50");
        ClickLog c2 = new ClickLog("2025-03-16 05:29:30", "2", "0.75");
        ClickLog[] cls = {c1, c2};

        ServerLog[] srvs = new ServerLog[0];

        Campaign campaign = new Campaign(imps, cls, srvs);
        CampaignMetrics metrics = new CampaignMetrics(campaign);

        double expectedCTR = (double) 2 / 2; // 1.0
        assertEquals(expectedCTR, metrics.getCTR(), 1e-6);
    }

    @Test
    public void testCalculateBounceRate() {
        // Create server logs with specific entry and exit times.
        // s1: Bounce because diff < 4 seconds.
        // s2: Not a bounce.
        ServerLog s1 = new ServerLog("2025-03-16 05:28:00", "1", "2025-03-16 05:28:03", "2", "No");
        ServerLog s2 = new ServerLog("2025-03-16 05:30:00", "2", "2025-03-16 05:30:10", "2", "Yes");
        ServerLog[] srvs = {s1, s2};

        // Create dummy ClickLogs to define the denominator for bounce rate.
        ClickLog c1 = new ClickLog("2025-03-16 05:28:01", "1", "0.50");
        ClickLog c2 = new ClickLog("2025-03-16 05:30:01", "2", "0.75");
        ClickLog[] cls = {c1, c2};

        ImpressionLog[] imps = new ImpressionLog[0];

        Campaign campaign = new Campaign(imps, cls, srvs);
        CampaignMetrics metrics = new CampaignMetrics(campaign);

        // Expect s1 to be counted as bounce because diffSeconds = 3 (<4).
        // Thus, numberOfBounces should be 1; numberOfClicks is 2.
        // Bounce Rate should be 0.5.
        assertEquals(0.5, metrics.getBounceRate(), 1e-6);
    }

    @Test
    public void testDerivedMetricsCalculations() {
        // Combine all logs to test derived metrics like CPC, CPA, CPM.

        // Impression logs (2 entries)
        ImpressionLog imp1 = new ImpressionLog("2025-03-16 05:28:06", "1", "Male", "25-34", "High", "Blog", "0.001632");
        ImpressionLog imp2 = new ImpressionLog("2025-03-16 05:30:06", "2", "Female", "35-44", "Medium", "News", "0.002000");
        ImpressionLog[] imps = {imp1, imp2};

        // Click logs (2 entries)
        ClickLog c1 = new ClickLog("2025-03-16 05:29:00", "1", "0.50");
        ClickLog c2 = new ClickLog("2025-03-16 05:29:30", "2", "0.75");
        ClickLog[] cls = {c1, c2};

        // Server logs (1 conversion, 1 bounce from earlier test)
        ServerLog s1 = new ServerLog("2025-03-16 05:28:00", "1", "2025-03-16 05:28:03", "2", "No"); // bounce
        ServerLog s2 = new ServerLog("2025-03-16 05:30:00", "2", "2025-03-16 05:31:00", "3", "Yes"); // conversion
        ServerLog[] srvs = {s1, s2};

        Campaign campaign = new Campaign(imps, cls, srvs);
        CampaignMetrics metrics = new CampaignMetrics(campaign);

        // Total cost: impression costs + click costs.
        double expectedTotalCost = 0.001632 + 0.002000 + 0.50 + 0.75;
        assertEquals(expectedTotalCost, metrics.getTotalCost(), 1e-6);

        // CTR = clicks/impressions = 2/2 = 1.0
        assertEquals(1.0, metrics.getCTR(), 1e-6);

        // CPC = totalCost / clicks = expectedTotalCost / 2
        assertEquals(expectedTotalCost / 2, metrics.getCPC(), 1e-6);

        // CPA = totalCost / conversions = expectedTotalCost / 1 (since s2 is conversion)
        assertEquals(expectedTotalCost, metrics.getCPA(), 1e-6);

        // CPM = (totalCost/impressions)*1000, in this case = (expectedTotalCost / 2)*100
        assertEquals((expectedTotalCost / 2) * 1000, metrics.getCPM(), 1e-6);
    }

    @Test
    @DisplayName("Test with Valid Data")
    public void testCalculateImpressions2() {
        ImpressionLog imp1 = new ImpressionLog("2025-03-16 05:28:06", "1", "Male", "25-34", "High", "Blog", "0.001632");
        ImpressionLog imp2 = new ImpressionLog("2025-03-16 05:30:06", "2", "Female", "35-44", "Medium", "News", "0.002000");
        ImpressionLog[] imps = {imp1, imp2};

        ClickLog[] cls = new ClickLog[0];
        ServerLog[] srvs = new ServerLog[0];

        Campaign campaign = new Campaign(imps, cls, srvs);
        CampaignMetrics metrics = new CampaignMetrics(campaign);

        assertEquals(2, metrics.getNumberOfImpressions());
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
        // CPA and CPC return 0 because there are no conversions/clicks.
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
        // Create a server log where the time difference is exactly 4 seconds.
        ServerLog s = new ServerLog("2025-03-16 05:28:00", "1", "2025-03-16 05:28:04", "2", "No");
        ServerLog[] srvs = {s};
        // Create dummy click log for denominator.
        ClickLog c = new ClickLog("2025-03-16 05:28:01", "1", "0.50");
        ClickLog[] cls = {c};
        ImpressionLog[] imps = new ImpressionLog[0];

        Campaign campaign = new Campaign(imps, cls, srvs);
        CampaignMetrics metrics = new CampaignMetrics(campaign);


        assertEquals(1, metrics.getNumberOfBounces());
        assertEquals(1.0, metrics.getBounceRate(), 1e-6);
    }
    @Test
    @DisplayName("Test setting valid bounce criteria and recomputation")
    public void testValidBounceCriteria() {
        // Use a simple server log where bounce depends on threshold.
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

        // Now, change the bounce criteria so that visits up to 5 seconds qualify as a bounce.
        metrics.setBounceCriteria(1, 5);

        // Since there is only one log, we expect the bounce count to be 1.
        assertEquals(1, metrics.getNumberOfBounces(), "After increasing threshold, bounce count should be 1.");
        // And bounce rate should be 1.0 since there is 1 bounce and 1 click.
        assertEquals(1.0, metrics.getBounceRate(), 1e-6, "Bounce rate should be 1.0 after updating threshold.");

    }

    @Test
    @DisplayName("Test invalid bounce criteria")
    public void testInvalidBounceCriteria() {
        Campaign campaign = createCampaign(new ImpressionLog[0], new ClickLog[0], new ServerLog[0]);
        CampaignMetrics metrics = new CampaignMetrics(campaign);
        // Negative pages threshold should throw exception.
        assertThrows(IllegalArgumentException.class, () -> {
            metrics.setBounceCriteria(-1, 4);
        }, "Setting negative pages threshold should throw IllegalArgumentException.");
        // Negative seconds threshold should throw exception.
        assertThrows(IllegalArgumentException.class, () -> {
            metrics.setBounceCriteria(1, -4);
        }, "Setting negative seconds threshold should throw IllegalArgumentException.");
    }
    @Test
    @DisplayName("Test filtered metrics computation")
    public void testFilteredMetricsComputation() {
        // Create test data with different demographics
        ImpressionLog imp1 = new ImpressionLog("2025-03-16 05:28:06", "1", "Male", "25-34", "High", "Blog", "0.001632");
        ImpressionLog imp2 = new ImpressionLog("2025-03-16 05:30:06", "2", "Female", "35-44", "Medium", "News", "0.002000");
        ImpressionLog[] imps = {imp1, imp2};

        ClickLog c1 = new ClickLog("2025-03-16 05:28:30", "1", "0.50");
        ClickLog c2 = new ClickLog("2025-03-16 05:30:30", "2", "0.75");
        ClickLog[] cls = {c1, c2};

        ServerLog s1 = new ServerLog("2025-03-16 05:28:40", "1", "2025-03-16 05:30:00", "2", "No");
        ServerLog s2 = new ServerLog("2025-03-16 05:30:40", "2", "2025-03-16 05:32:00", "5", "Yes");
        ServerLog[] srvs = {s1, s2};

        Campaign campaign = new Campaign(imps, cls, srvs);
        CampaignMetrics metrics = new CampaignMetrics(campaign);

        // Create filtered metrics for males only
        TimeFilteredMetrics filteredMetrics = new TimeFilteredMetrics(
            imps, srvs, cls,
            metrics.getBouncePagesThreshold(),
            metrics.getBounceSecondsThreshold()
        );

        // Apply gender filter
        filteredMetrics.setGenderFilter("Male");

        // Apply time filter (full range of data)
        LocalDateTime start = LocalDateTime.of(2025, 3, 16, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 3, 16, 23, 59, 59);
        filteredMetrics.computeForTimeFrame(start, end, "Daily");

        // Verify filtered metrics
        assertEquals(1, filteredMetrics.getNumberOfImpressions(), "Should have 1 impression for Male");
        assertEquals(1, filteredMetrics.getNumberOfClicks(), "Should have 1 click for Male");
        assertEquals(1, filteredMetrics.getNumberOfUniques(), "Should have 1 unique for Male");
        assertEquals(0, filteredMetrics.getNumberOfConversions(), "Should have 0 conversions for Male");

        // Apply different filter (context)
        filteredMetrics.setGenderFilter(null);
        filteredMetrics.setContextFilter("News");
        filteredMetrics.computeForTimeFrame(start, end, "Daily");

        // Verify differently filtered metrics
        assertEquals(1, filteredMetrics.getNumberOfImpressions(), "Should have 1 impression for News context");
        assertEquals(1, filteredMetrics.getNumberOfClicks(), "Should have 1 click for News context");
        assertEquals(1, filteredMetrics.getNumberOfUniques(), "Should have 1 unique for News context");
        assertEquals(1, filteredMetrics.getNumberOfConversions(), "Should have 1 conversion for News context");
    }

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
    @DisplayName("Test campaign date range detection")
    public void testCampaignDateRangeDetection() {
        // Create logs with different dates
        ImpressionLog imp1 = new ImpressionLog("2025-03-15 05:28:06", "1", "Male", "25-34", "High", "Blog", "0.001632");
        ImpressionLog imp2 = new ImpressionLog("2025-03-20 05:30:06", "2", "Female", "35-44", "Medium", "News", "0.002000");
        ImpressionLog[] imps = {imp1, imp2};

        ClickLog c1 = new ClickLog("2025-03-10 05:28:30", "1", "0.50"); // Earlier than impressions
        ClickLog c2 = new ClickLog("2025-03-25 05:30:30", "2", "0.75"); // Later than impressions
        ClickLog[] cls = {c1, c2};

        ServerLog s = new ServerLog("2025-03-18 05:28:40", "1", "2025-03-18 05:30:00", "3", "Yes");
        ServerLog[] srvs = {s};

        Campaign campaign = new Campaign(imps, cls, srvs);
        CampaignMetrics metrics = new CampaignMetrics(campaign);

        // Verify start date is earliest date across all logs
        LocalDateTime expectedStart = LocalDateTime.of(2025, 3, 10, 5, 28, 30);
        assertEquals(expectedStart, metrics.getCampaignStartDate(),
            "Campaign start date should be from earliest log (click log c1)");

        // Verify end date is latest date across all logs
        LocalDateTime expectedEnd = LocalDateTime.of(2025, 3, 25, 5, 30, 30);
        assertEquals(expectedEnd, metrics.getCampaignEndDate(),
            "Campaign end date should be from latest log (click log c2)");
    }
}
