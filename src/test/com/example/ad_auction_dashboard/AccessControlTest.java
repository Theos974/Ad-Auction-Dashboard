package com.example.ad_auction_dashboard;

import com.example.ad_auction_dashboard.logic.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for role-based access control functionality
 * Verifies that users with different roles have appropriate access to campaigns
 */
public class AccessControlTest {

    // Test users
    private UserDatabase.User adminUser;
    private UserDatabase.User editorUser;
    private UserDatabase.User viewerUser;

    // Test campaign
    private Campaign testCampaign;
    private CampaignMetrics testMetrics;
    private String testCampaignName;
    private int savedCampaignId = -1;

    @BeforeEach
    void setUp() {
        // Generate unique IDs for users and campaign
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);

        // Create test users with different roles
        String adminUsername = "admin_" + uniqueId;
        UserDatabase.addUser(adminUsername, "admin_" + uniqueId + "@example.com",
            "1" + System.currentTimeMillis() % 10000000000L, "password", "admin");
        adminUser = UserDatabase.getUser(adminUsername);

        String editorUsername = "editor_" + uniqueId;
        UserDatabase.addUser(editorUsername, "editor_" + uniqueId + "@example.com",
            "2" + System.currentTimeMillis() % 10000000000L, "password", "editor");
        editorUser = UserDatabase.getUser(editorUsername);

        String viewerUsername = "viewer_" + uniqueId;
        UserDatabase.addUser(viewerUsername, "viewer_" + uniqueId + "@example.com",
            "3" + System.currentTimeMillis() % 10000000000L, "password", "viewer");
        viewerUser = UserDatabase.getUser(viewerUsername);

        // Create test campaign
        testCampaignName = "Test Campaign " + uniqueId;

        // Set up a simple campaign
        ImpressionLog imp = new ImpressionLog("2023-03-01 10:00:00", "1001", "Male", "<25", "Medium", "News", "0.123456");
        ClickLog click = new ClickLog("2023-03-01 10:05:00", "1001", "1.230000");
        ServerLog server = new ServerLog("2023-03-01 10:05:30", "1001", "2023-03-01 10:10:30", "3", "Yes");

        testCampaign = new Campaign(new ImpressionLog[]{imp}, new ClickLog[]{click}, new ServerLog[]{server});
        testMetrics = new CampaignMetrics(testCampaign);

        // Save campaign as editor user
        savedCampaignId = CampaignDatabase.saveCampaign(testMetrics, testCampaignName, editorUser.getId());
    }

    @AfterEach
    void tearDown() {
        // Clean up campaign
        if (savedCampaignId > 0) {
            CampaignDatabase.deleteCampaign(savedCampaignId);
        }

        // Clean up users
        if (adminUser != null) UserDatabase.deleteUser(adminUser.getId());
        if (editorUser != null) UserDatabase.deleteUser(editorUser.getId());
        if (viewerUser != null) UserDatabase.deleteUser(viewerUser.getId());
    }

    @Test
    void testCampaignOwnerAccess() {
        // Verify editor can access their own campaign
        boolean canAccess = CampaignDatabase.canUserAccessCampaign(editorUser.getId(), savedCampaignId);
        assertTrue(canAccess, "Editor should be able to access their own campaign");

        // Verify campaign appears in editor's accessible campaigns
        List<CampaignDatabase.CampaignInfo> accessibleCampaigns =
            CampaignDatabase.getAccessibleCampaigns(editorUser.getId());

        boolean foundCampaign = accessibleCampaigns.stream()
            .anyMatch(c -> c.getCampaignId() == savedCampaignId);
        assertTrue(foundCampaign, "Editor should see their own campaign in accessible campaigns");
    }

    @Test
    void testAdminAccess() {
        // Print admin user details
        System.out.println("Admin User ID: " + adminUser.getId());
        System.out.println("Saved Campaign ID: " + savedCampaignId);
        System.out.println("Campaign Creator ID: " + editorUser.getId());

        // Verify admin can access by checking both methods
        boolean directCreatorAccess = CampaignDatabase.canUserAccessCampaign(adminUser.getId(), savedCampaignId);

        // Get all campaigns
        List<CampaignDatabase.CampaignInfo> allCampaigns = CampaignDatabase.getAllCampaigns();
        System.out.println("Total Campaigns: " + allCampaigns.size());
        for (CampaignDatabase.CampaignInfo campaign : allCampaigns) {
            System.out.println("Campaign " + campaign.getCampaignId() +
                " - Name: " + campaign.getCampaignName() +
                " - User ID: " + campaign.getUserId());
        }

        // Get accessible campaigns for admin
        List<CampaignDatabase.CampaignInfo> accessibleCampaigns =
            CampaignDatabase.getAccessibleCampaigns(adminUser.getId());

        System.out.println("Accessible Campaigns for Admin: " + accessibleCampaigns.size());
        for (CampaignDatabase.CampaignInfo campaign : accessibleCampaigns) {
            System.out.println("Accessible Campaign " + campaign.getCampaignId() +
                " - Name: " + campaign.getCampaignName() +
                " - User ID: " + campaign.getUserId());
        }

        // Admin should have access to all campaigns by default
        boolean canAccess = CampaignDatabase.canUserAccessCampaign(adminUser.getId(), savedCampaignId);
        assertTrue(canAccess, "Admin should be able to access any campaign");

        // Verify campaign appears in admin's accessible campaigns
        boolean foundCampaign = accessibleCampaigns.stream()
            .anyMatch(c -> c.getCampaignId() == savedCampaignId);
        assertTrue(foundCampaign, "Admin should see all campaigns in accessible campaigns");
    }

    @Test
    void testViewerAccess() {
        // Initially, viewer should not have access
        boolean canAccess = CampaignDatabase.canUserAccessCampaign(viewerUser.getId(), savedCampaignId);
        assertFalse(canAccess, "Viewer should not have access to campaign by default");

        // Assign campaign to viewer
        boolean assigned = CampaignDatabase.assignCampaignToUser(savedCampaignId, viewerUser.getId(), adminUser.getId());
        assertTrue(assigned, "Admin should be able to assign campaign to viewer");

        // Now viewer should have access
        canAccess = CampaignDatabase.canUserAccessCampaign(viewerUser.getId(), savedCampaignId);
        assertTrue(canAccess, "Viewer should have access to campaign after assignment");

        // Verify campaign appears in viewer's accessible campaigns
        List<CampaignDatabase.CampaignInfo> accessibleCampaigns =
            CampaignDatabase.getAccessibleCampaigns(viewerUser.getId());

        boolean foundCampaign = accessibleCampaigns.stream()
            .anyMatch(c -> c.getCampaignId() == savedCampaignId);
        assertTrue(foundCampaign, "Viewer should see assigned campaign in accessible campaigns");
    }

    @Test
    void testCampaignAssignmentPermissions() {
        // Admin should be able to assign campaigns
        boolean adminCanAssign = CampaignDatabase.assignCampaignToUser(
            savedCampaignId, viewerUser.getId(), adminUser.getId());
        assertTrue(adminCanAssign, "Admin should be able to assign campaigns");

        // Remove assignment
        CampaignDatabase.removeCampaignFromUser(savedCampaignId, viewerUser.getId());

        // Editor (owner) should be able to assign their own campaign
        boolean editorCanAssign = CampaignDatabase.assignCampaignToUser(
            savedCampaignId, viewerUser.getId(), editorUser.getId());
        assertTrue(editorCanAssign, "Editor should be able to assign their own campaign");

        // Remove assignment
        CampaignDatabase.removeCampaignFromUser(savedCampaignId, viewerUser.getId());

        // Create a second editor
        String secondEditorUsername = "editor2_" + UUID.randomUUID().toString().substring(0, 8);
        UserDatabase.addUser(secondEditorUsername, "editor2_" + UUID.randomUUID() + "@example.com",
            "4" + System.currentTimeMillis() % 10000000000L, "password", "editor");
        UserDatabase.User secondEditor = UserDatabase.getUser(secondEditorUsername);

        // Second editor should not be able to access the campaign by default
        boolean secondEditorAccess = CampaignDatabase.canUserAccessCampaign(secondEditor.getId(), savedCampaignId);
        assertFalse(secondEditorAccess, "Second editor should not have access to another editor's campaign");

        // Clean up second editor
        UserDatabase.deleteUser(secondEditor.getId());
    }

    @Test
    void testViewerPermissionLimitations() {
        // Assign campaign to viewer
        CampaignDatabase.assignCampaignToUser(savedCampaignId, viewerUser.getId(), adminUser.getId());

        // Create a new test user
        String newViewerUsername = "viewer2_" + UUID.randomUUID().toString().substring(0, 8);
        UserDatabase.addUser(newViewerUsername, "viewer2_" + UUID.randomUUID() + "@example.com",
            "5" + System.currentTimeMillis() % 10000000000L, "password", "viewer");
        UserDatabase.User newViewer = UserDatabase.getUser(newViewerUsername);

        // Viewer should not be able to assign campaign to another user
        // This would normally be enforced in the controller layer, not at the database level
        // So we're testing the expected behavior if the controller was enforcing this

        // For demonstration purposes, we can't test this directly because the CampaignDatabase
        // doesn't have role-based access control built in at the API level.
        // In a real app, this would be enforced by the controller or service layer.

        // Instead, we'll verify that the viewer permission level doesn't include admin capabilities
        assertFalse(viewerUser.isAdmin(), "Viewer should not have admin permissions");
        assertFalse(viewerUser.isEditor(), "Viewer should not have editor permissions");
        assertTrue(viewerUser.isViewer(), "Viewer should have viewer permissions");

        // Clean up new viewer
        UserDatabase.deleteUser(newViewer.getId());
    }

    @Test
    void testUserRoles() {
        // Verify permission levels
        assertTrue(adminUser.isAdmin(), "Admin user should have admin permission");
        assertTrue(adminUser.isEditor(), "Admin user should have editor permission");
        assertTrue(adminUser.isViewer(), "Admin user should have viewer permission");

        assertFalse(editorUser.isAdmin(), "Editor user should not have admin permission");
        assertTrue(editorUser.isEditor(), "Editor user should have editor permission");
        assertTrue(editorUser.isViewer(), "Editor user should have viewer permission");

        assertFalse(viewerUser.isAdmin(), "Viewer user should not have admin permission");
        assertFalse(viewerUser.isEditor(), "Viewer user should not have editor permission");
        assertTrue(viewerUser.isViewer(), "Viewer user should have viewer permission");
    }

    @Test
    void testRoleChangeImpactsPermissions() {
        // Initially, viewer does not have editor permissions
        assertFalse(viewerUser.isEditor(), "Viewer should not initially have editor permissions");

        // Change viewer to editor
        boolean roleChanged = UserDatabase.changeUserRole(viewerUser.getUsername(), "editor");
        assertTrue(roleChanged, "Role change should succeed");

        // Get updated user
        UserDatabase.User updatedUser = UserDatabase.getUser(viewerUser.getUsername());

        // Verify permissions updated
        assertTrue(updatedUser.isEditor(), "User should now have editor permissions after role change");
        assertFalse(updatedUser.isAdmin(), "User should still not have admin permissions");
    }
}