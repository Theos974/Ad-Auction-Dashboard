package com.example.ad_auction_dashboard.IntegrationTests;

import static org.junit.jupiter.api.Assertions.*;
import com.example.ad_auction_dashboard.logic.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

public class CampaignDatabaseIntegrationTest {

    private UserDatabase.User editorUser;
    private UserDatabase.User viewerUser;
    private Campaign testCampaign;
    private CampaignMetrics testMetrics;
    private int savedCampaignId = -1;

    @BeforeEach
    void setUp() {
        // Create test users
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String editorUsername = "editor_" + uniqueId;
        UserDatabase.addUser(editorUsername, "editor_" + uniqueId + "@example.com",
            "1" + System.currentTimeMillis() % 10000000000L, "password", "editor");
        editorUser = UserDatabase.getUser(editorUsername);

        String viewerUsername = "viewer_" + uniqueId;
        UserDatabase.addUser(viewerUsername, "viewer_" + uniqueId + "@example.com",
            "2" + System.currentTimeMillis() % 10000000000L, "password", "viewer");
        viewerUser = UserDatabase.getUser(viewerUsername);

        // Create test campaign
        ImpressionLog imp = new ImpressionLog("2023-03-01 10:00:00", "1001", "Male", "<25", "Medium", "News", "0.123456");
        ClickLog click = new ClickLog("2023-03-01 10:05:00", "1001", "1.230000");
        ServerLog server = new ServerLog("2023-03-01 10:05:30", "1001", "2023-03-01 10:10:30", "3", "Yes");

        testCampaign = new Campaign(
            new ImpressionLog[]{imp},
            new ClickLog[]{click},
            new ServerLog[]{server}
        );
        testMetrics = new CampaignMetrics(testCampaign);
    }

    @AfterEach
    void tearDown() {
        // Clean up test data
        if (savedCampaignId > 0) {
            CampaignDatabase.deleteCampaign(savedCampaignId);
        }
        if (editorUser != null) UserDatabase.deleteUser(editorUser.getId());
        if (viewerUser != null) UserDatabase.deleteUser(viewerUser.getId());
    }

    @Test
    void testGetAccessibleCampaigns() {
        // Save campaign as editor
        savedCampaignId = CampaignDatabase.saveCampaign(testMetrics, "Accessible Test", editorUser.getId());

        // Test accessible campaigns for editor (creator)
        List<CampaignDatabase.CampaignInfo> editorCampaigns =
            CampaignDatabase.getAccessibleCampaigns(editorUser.getId());

        assertTrue(editorCampaigns.stream()
                .anyMatch(c -> c.getCampaignId() == savedCampaignId),
            "Editor should see own campaign in accessible campaigns");

        // Test accessible campaigns for viewer (not assigned yet)
        List<CampaignDatabase.CampaignInfo> viewerCampaigns =
            CampaignDatabase.getAccessibleCampaigns(viewerUser.getId());

        assertFalse(viewerCampaigns.stream()
                .anyMatch(c -> c.getCampaignId() == savedCampaignId),
            "Viewer should not see campaign in accessible campaigns before assignment");

        // Assign campaign to viewer
        CampaignDatabase.assignCampaignToUser(savedCampaignId, viewerUser.getId(), editorUser.getId());
CampaignDatabase.clearCaches();
        // Test accessible campaigns for viewer after assignment
        viewerCampaigns = CampaignDatabase.getAccessibleCampaigns(viewerUser.getId());

        assertTrue(viewerCampaigns.stream()
                .anyMatch(c -> c.getCampaignId() == savedCampaignId),
            "Viewer should see campaign in accessible campaigns after assignment");
    }

    @Test
    void testCampaignAssignmentAndAccess() {
        // Save campaign as editor
        savedCampaignId = CampaignDatabase.saveCampaign(testMetrics, "Assignment Test", editorUser.getId());

        // Test initial access permissions
        assertTrue(CampaignDatabase.canUserAccessCampaign(editorUser.getId(), savedCampaignId),
            "Editor should have access to own campaign");

        assertFalse(CampaignDatabase.canUserAccessCampaign(viewerUser.getId(), savedCampaignId),
            "Viewer should not have access before assignment");

        // Assign campaign to viewer
        boolean assigned = CampaignDatabase.assignCampaignToUser(
            savedCampaignId, viewerUser.getId(), editorUser.getId());

        assertTrue(assigned, "Assignment should succeed");

        // Verify access after assignment
        assertTrue(CampaignDatabase.canUserAccessCampaign(viewerUser.getId(), savedCampaignId),
            "Viewer should have access after assignment");

        // Revoke access
        boolean removed = CampaignDatabase.removeCampaignFromUser(savedCampaignId, viewerUser.getId());
        assertTrue(removed, "Removal should succeed");

        // Verify access after removal
        assertFalse(CampaignDatabase.canUserAccessCampaign(viewerUser.getId(), savedCampaignId),
            "Viewer should not have access after removal");
    }

    @Test
    void testCampaignDataPersistence() {
        // Save campaign
        savedCampaignId = CampaignDatabase.saveCampaign(testMetrics, "Persistence Test", editorUser.getId());

        // Load campaign
        Campaign loadedCampaign = CampaignDatabase.loadCampaign(savedCampaignId);

        // Verify data integrity
        assertNotNull(loadedCampaign, "Loaded campaign should not be null");
        assertEquals(1, loadedCampaign.getImpressionLogs().length,
            "Should have 1 impression log");
        assertEquals(1, loadedCampaign.getClickLogs().length,
            "Should have 1 click log");
        assertEquals(1, loadedCampaign.getServerLogs().length,
            "Should have 1 server log");

        // Verify specific data values
        ImpressionLog originalImp = testCampaign.getImpressionLogs()[0];
        ImpressionLog loadedImp = loadedCampaign.getImpressionLogs()[0];

        assertEquals(originalImp.getId(), loadedImp.getId(),
            "Impression log ID should match");
        assertEquals(originalImp.getGender(), loadedImp.getGender(),
            "Impression log gender should match");
    }
}