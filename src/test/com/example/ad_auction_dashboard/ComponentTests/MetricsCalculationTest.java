package com.example.ad_auction_dashboard.ComponentTests;

import static org.junit.jupiter.api.Assertions.*;
import com.example.ad_auction_dashboard.logic.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class MetricsCalculationTest {

    @Test
    @DisplayName("Test Conversion Calculation")
    public void testCalculateConversions() {
        // Create server logs with different conversion values
        ServerLog s1 = new ServerLog("2025-03-16 05:28:00", "1", "2025-03-16 05:29:00", "3", "Yes");
        ServerLog s2 = new ServerLog("2025-03-16 05:30:00", "2", "2025-03-16 05:31:00", "2", "No");
        ServerLog[] srvs = {s1, s2};

        Campaign campaign = new Campaign(new ImpressionLog[0], new ClickLog[0], srvs);
        CampaignMetrics metrics = new CampaignMetrics(campaign);

        // Only s1 qualifies as a conversion
        assertEquals(1, metrics.getNumberOfConversions(), "Should count 1 conversion");
    }

    @Test
    @DisplayName("Test Total Cost Calculation")
    public void testCalculateTotalCost() {
        ImpressionLog imp1 = new ImpressionLog("2025-03-16 05:28:06", "1", "Male", "25-34", "High", "Blog", "0.001632");
        ImpressionLog imp2 = new ImpressionLog("2025-03-16 05:30:06", "2", "Female", "35-44", "Medium", "News", "0.002000");
        ImpressionLog[] imps = {imp1, imp2};

        ClickLog c1 = new ClickLog("2025-03-16 05:29:00", "1", "0.500000");
        ClickLog c2 = new ClickLog("2025-03-16 05:29:30", "2", "0.750000");
        ClickLog[] cls = {c1, c2};

        Campaign campaign = new Campaign(imps, cls, new ServerLog[0]);
        CampaignMetrics metrics = new CampaignMetrics(campaign);

        double expectedCost = 0.001632 + 0.002000 + 0.5 + 0.75;
        assertEquals(expectedCost, metrics.getTotalCost(), 0.0001, "Total cost should be sum of impression and click costs");
    }

    @Test
    @DisplayName("Test CTR Calculation")
    public void testCalculateCTR() {
        ImpressionLog[] imps = {
            new ImpressionLog("2025-03-16 05:28:06", "1", "Male", "25-34", "High", "Blog", "0.001632"),
            new ImpressionLog("2025-03-16 05:30:06", "2", "Female", "35-44", "Medium", "News", "0.002000")
        };

        ClickLog[] cls = {
            new ClickLog("2025-03-16 05:29:00", "1", "0.500000")
        };

        Campaign campaign = new Campaign(imps, cls, new ServerLog[0]);
        CampaignMetrics metrics = new CampaignMetrics(campaign);

        // CTR = clicks/impressions = 1/2 = 0.5
        assertEquals(0.5, metrics.getCTR(), 0.0001, "CTR should be clicks/impressions");
    }

    @Test
    @DisplayName("Test Bounce Rate Calculation")
    public void testCalculateBounceRate() {
        ClickLog[] cls = {
            new ClickLog("2025-03-16 05:29:00", "1", "0.500000"),
            new ClickLog("2025-03-16 05:30:00", "2", "0.750000")
        };

        ServerLog[] srvs = {
            new ServerLog("2025-03-16 05:29:30", "1", "2025-03-16 05:29:33", "1", "No"), // Bounce (short time)
            new ServerLog("2025-03-16 05:30:30", "2", "2025-03-16 05:35:00", "4", "Yes") // Not a bounce
        };

        Campaign campaign = new Campaign(new ImpressionLog[0], cls, srvs);
        CampaignMetrics metrics = new CampaignMetrics(campaign);

        // Bounce rate = bounces/clicks = 1/2 = 0.5
        assertEquals(0.5, metrics.getBounceRate(), 0.0001, "Bounce rate should be bounces/clicks");
    }

    @Test
    @DisplayName("Test CPA Calculation")
    public void testCPACalculation() {
        ImpressionLog[] imps = {
            new ImpressionLog("2025-03-16 05:28:06", "1", "Male", "25-34", "High", "Blog", "0.001632")
        };

        ClickLog[] cls = {
            new ClickLog("2025-03-16 05:29:00", "1", "0.500000")
        };

        ServerLog[] srvs = {
            new ServerLog("2025-03-16 05:29:30", "1", "2025-03-16 05:35:00", "5", "Yes") // Conversion
        };

        Campaign campaign = new Campaign(imps, cls, srvs);
        CampaignMetrics metrics = new CampaignMetrics(campaign);

        double expectedCost = 0.001632 + 0.5;
        // CPA = totalCost/conversions = expectedCost/1
        assertEquals(expectedCost, metrics.getCPA(), 0.0001, "CPA should be totalCost/conversions");
    }
}