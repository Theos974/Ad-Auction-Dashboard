package com.example.ad_auction_dashboard.RegressionTests;

import static org.junit.jupiter.api.Assertions.*;
import com.example.ad_auction_dashboard.logic.*;
import org.junit.jupiter.api.Test;

public class CoreFunctionalityRegressionTest {

    @Test
    public void testDerivedMetricsCalculations() {
        // Combined test data
        ImpressionLog imp1 = new ImpressionLog("2025-03-16 05:28:06", "1", "Male", "25-34", "High", "Blog", "0.001632");
        ImpressionLog imp2 = new ImpressionLog("2025-03-16 05:30:06", "2", "Female", "35-44", "Medium", "News", "0.002000");
        ImpressionLog[] imps = {imp1, imp2};

        ClickLog c1 = new ClickLog("2025-03-16 05:29:00", "1", "0.50");
        ClickLog c2 = new ClickLog("2025-03-16 05:29:30", "2", "0.75");
        ClickLog[] cls = {c1, c2};

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

        // CPA = totalCost / conversions = expectedTotalCost / 1
        assertEquals(expectedTotalCost, metrics.getCPA(), 1e-6);

        // CPM = (totalCost/impressions)*1000, in this case = (expectedTotalCost / 2)*100
        assertEquals((expectedTotalCost / 2) * 1000, metrics.getCPM(), 1e-6);
    }

    @Test
    public void testCampaignSaveAndLoad() {
        // Create a simple campaign
        ImpressionLog imp = new ImpressionLog("2025-03-16 05:28:06", "1", "Male", "25-34", "High", "Blog", "0.001632");
        ClickLog click = new ClickLog("2025-03-16 05:29:00", "1", "0.50");
        ServerLog server = new ServerLog("2025-03-16 05:28:00", "1", "2025-03-16 05:28:03", "2", "No");

        Campaign campaign = new Campaign(
            new ImpressionLog[]{imp},
            new ClickLog[]{click},
            new ServerLog[]{server}
        );

        CampaignMetrics metrics = new CampaignMetrics(campaign);

        // Get a test user ID (admin user should always exist)
        UserDatabase.User admin = UserDatabase.getUser("admin");
        if (admin == null) {
            // If no admin, create one
            UserDatabase.addUser("test_admin", "test@example.com", "1234567890", "password", "admin");
            admin = UserDatabase.getUser("test_admin");
        }

        int userId = admin.getId();

        // Save the campaign
        int campaignId = CampaignDatabase.saveCampaign(metrics, "Regression Test Campaign", userId);

        // Verify campaign was saved
        assertTrue(campaignId > 0, "Campaign should be saved with a valid ID");

        // Load the campaign
        Campaign loadedCampaign = CampaignDatabase.loadCampaign(campaignId);

        // Verify campaign was loaded
        assertNotNull(loadedCampaign, "Campaign should be loaded from database");
        assertEquals(1, loadedCampaign.getImpressionLogs().length, "Should have 1 impression log");
        assertEquals(1, loadedCampaign.getClickLogs().length, "Should have 1 click log");
        assertEquals(1, loadedCampaign.getServerLogs().length, "Should have 1 server log");

        // Clean up
        CampaignDatabase.deleteCampaign(campaignId);
        if (admin.getUsername().equals("test_admin")) {
            UserDatabase.deleteUser(admin.getId());
        }
    }

    @Test
    public void testUserAccess() {
        // Create test users
        String uniqueId = java.util.UUID.randomUUID().toString().substring(0, 8);
        String editorUsername = "editor_" + uniqueId;
        String viewerUsername = "viewer_" + uniqueId;

        UserDatabase.addUser(editorUsername, "editor_" + uniqueId + "@example.com",
            "1" + System.currentTimeMillis() % 10000000000L, "password", "editor");
        UserDatabase.User editorUser = UserDatabase.getUser(editorUsername);

        UserDatabase.addUser(viewerUsername, "viewer_" + uniqueId + "@example.com",
            "2" + System.currentTimeMillis() % 10000000000L, "password", "viewer");
        UserDatabase.User viewerUser = UserDatabase.getUser(viewerUsername);

        // Create a simple campaign
        ImpressionLog imp = new ImpressionLog("2025-03-16 05:28:06", "1", "Male", "25-34", "High", "Blog", "0.001632");
        Campaign campaign = new Campaign(
            new ImpressionLog[]{imp},
            new ClickLog[0],
            new ServerLog[0]
        );

        CampaignMetrics metrics = new CampaignMetrics(campaign);

        // Save as editor
        int campaignId = CampaignDatabase.saveCampaign(metrics, "Access Test Campaign", editorUser.getId());

        // Verify editor has access
        boolean editorAccess = CampaignDatabase.canUserAccessCampaign(editorUser.getId(), campaignId);
        assertTrue(editorAccess, "Editor (owner) should have access to campaign");

        // Verify viewer doesn't have access
        boolean viewerAccess = CampaignDatabase.canUserAccessCampaign(viewerUser.getId(), campaignId);
        assertFalse(viewerAccess, "Viewer should not have access to campaign initially");

        // Grant access to viewer
        boolean granted = CampaignDatabase.assignCampaignToUser(campaignId, viewerUser.getId(), editorUser.getId());
        assertTrue(granted, "Access should be granted successfully");

        // Verify viewer now has access
        viewerAccess = CampaignDatabase.canUserAccessCampaign(viewerUser.getId(), campaignId);
        assertTrue(viewerAccess, "Viewer should have access after assignment");

        // Clean up
        CampaignDatabase.deleteCampaign(campaignId);
        UserDatabase.deleteUser(editorUser.getId());
        UserDatabase.deleteUser(viewerUser.getId());
    }

}
