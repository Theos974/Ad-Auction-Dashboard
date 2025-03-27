package com.example.ad_auction_dashboard.ComponentTests;


import static org.junit.jupiter.api.Assertions.*;
import com.example.ad_auction_dashboard.logic.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class MetricsComponentTest {

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

}