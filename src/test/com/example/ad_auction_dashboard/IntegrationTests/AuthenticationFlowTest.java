package com.example.ad_auction_dashboard.IntegrationTests;

import static org.junit.jupiter.api.Assertions.*;
import com.example.ad_auction_dashboard.logic.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.UUID;

public class AuthenticationFlowTest {

    private String testUsername;
    private String testEmail;
    private String testPhone;
    private final String testPassword = "testPassword123";
    private int testUserId = -1;

    @BeforeEach
    void setUp() {
        // Generate unique credentials
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        testUsername = "testauth_" + uniqueId;
        testEmail = "testauth_" + uniqueId + "@example.com";
        testPhone = "1" + System.currentTimeMillis() % 10000000000L;

        // Create test user
        UserDatabase.addUser(testUsername, testEmail, testPhone, testPassword, "viewer");

        // Get user ID for cleanup
        UserDatabase.User user = UserDatabase.getUser(testUsername);
        if (user != null) {
            testUserId = user.getId();
        }

        // Ensure session is cleared
        UserSession.getInstance().logout();
    }

    @Test
    void testSuccessfulAuthentication() {
        // Verify initial state
        assertNull(UserSession.getInstance().getUser(),
            "User session should start empty");

        // Perform authentication
        boolean authenticated = UserDatabase.authenticateUser(testUsername, testPassword);
        assertTrue(authenticated, "Authentication should succeed with correct credentials");

        // Create session
        UserDatabase.User user = UserDatabase.getUser(testUsername);
        UserSession.getInstance().setUser(user);

        // Verify session state
        assertNotNull(UserSession.getInstance().getUser(),
            "User should be set in session after authentication");
        assertEquals(testUsername, UserSession.getInstance().getUser().getUsername(),
            "Username in session should match authenticated user");
        assertEquals("viewer", UserSession.getInstance().getRole(),
            "Role in session should match user's role");
    }

    @Test
    void testFailedAuthentication() {
        // Test with wrong password
        boolean authenticated = UserDatabase.authenticateUser(testUsername, "wrongPassword");
        assertFalse(authenticated, "Authentication should fail with incorrect password");

        // Test with non-existent user
        authenticated = UserDatabase.authenticateUser("nonExistentUser", testPassword);
        assertFalse(authenticated, "Authentication should fail with non-existent username");
    }

    @Test
    void testLogout() {
        // First authenticate and set up session
        UserDatabase.User user = UserDatabase.getUser(testUsername);
        UserSession.getInstance().setUser(user);

        // Verify user is set
        assertNotNull(UserSession.getInstance().getUser(),
            "User should be set in session");

        // Perform logout
        UserSession.getInstance().logout();

        // Verify session is cleared
        assertNull(UserSession.getInstance().getUser(),
            "User session should be cleared after logout");
        assertNull(UserSession.getInstance().getRole(),
            "Role should be null after logout");
    }

    @Test
    void testRoleBasedPermissions() {
        // Set up session with viewer role
        UserDatabase.User viewerUser = UserDatabase.getUser(testUsername);
        UserSession.getInstance().setUser(viewerUser);

        // Test permissions
        assertTrue(UserSession.getInstance().isViewer(),
            "User should have viewer permissions");
        assertFalse(UserSession.getInstance().isEditor(),
            "Viewer should not have editor permissions");
        assertFalse(UserSession.getInstance().isAdmin(),
            "Viewer should not have admin permissions");

        // Create temp admin user
        String adminUsername = "tempadmin_" + UUID.randomUUID().toString().substring(0, 8);
        UserDatabase.addUser(adminUsername, "admin_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com",
            "9" + System.currentTimeMillis() % 10000000000L, "adminpass", "admin");

        // Set up session with admin role
        UserDatabase.User adminUser = UserDatabase.getUser(adminUsername);
        UserSession.getInstance().setUser(adminUser);

        // Test admin permissions
        assertTrue(UserSession.getInstance().isViewer(),
            "Admin should have viewer permissions");
        assertTrue(UserSession.getInstance().isEditor(),
            "Admin should have editor permissions");
        assertTrue(UserSession.getInstance().isAdmin(),
            "Admin should have admin permissions");

        // Clean up admin user
        UserDatabase.deleteUser(adminUser.getId());
    }

    @AfterEach
    void tearDown() {
        // Clean up test user
        if (testUserId > 0) {
            UserDatabase.deleteUser(testUserId);
        }

        // Clear session
        UserSession.getInstance().logout();
    }
}