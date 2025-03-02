package com.example.ad_auction_dashboard;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.ad_auction_dashboard.logic.Campaign;
import com.example.ad_auction_dashboard.logic.CampaignMetrics;
import com.example.ad_auction_dashboard.logic.ClickLog;
import com.example.ad_auction_dashboard.logic.ImpressionLog;
import com.example.ad_auction_dashboard.logic.ServerLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


public class MetricsTests {

    // Helper constructor for Campaign if you don't have one that accepts arrays.
    // You may use setters if needed.
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

        // CPM = (totalCost/impressions)*100, in this case = (expectedTotalCost / 2)*100
        assertEquals((expectedTotalCost / 2) * 100, metrics.getCPM(), 1e-6);
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
        // If your Campaign constructor or setters don't handle nulls,
        // you might wrap this in assertDoesNotThrow, or modify Campaign to convert nulls to empty arrays.
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

        // Depending on your specification (<= 4 counts as bounce),
        // this test checks the boundary condition.
        // If <= 4 is a bounce, expected bounce rate = 1.0.
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
}
