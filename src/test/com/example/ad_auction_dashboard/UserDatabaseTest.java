package com.example.ad_auction_dashboard;

import com.example.ad_auction_dashboard.logic.UserDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UserDatabase class to validate user management functionality
 */
public class UserDatabaseTest {

    // Test user credentials
    private String testUsername;
    private String testEmail;
    private String testPhone;
    private final String testPassword = "testPassword";
    private final String testRole = "viewer";

    // Track user IDs to clean up
    private List<Integer> userIdsToCleanup = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // Generate a unique ID for this test run
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);

        // Create unique credentials for each test to avoid conflicts
        testUsername = "testuser_" + uniqueId;
        testEmail = "test_" + uniqueId + "@example.com";
        testPhone = "1" + System.currentTimeMillis() % 10000000000L; // Generate a unique 10-digit phone number
    }

    @AfterEach
    void tearDown() {
        // Clean up all tracked user IDs
        for (Integer id : userIdsToCleanup) {
            try {
                UserDatabase.deleteUser(id);
            } catch (Exception e) {
                System.err.println("Error cleaning up user: " + e.getMessage());
            }
        }
        userIdsToCleanup.clear();
    }

    // Helper to track created users for cleanup
    private void trackUser(String username) {
        UserDatabase.User user = UserDatabase.getUser(username);
        if (user != null) {
            userIdsToCleanup.add(user.getId());
        }
    }

    @Test
    void testAddAndGetUser() {
        // Add a new user
        UserDatabase.addUser(testUsername, testEmail, testPhone, testPassword, testRole);
        trackUser(testUsername);

        // Retrieve the user
        UserDatabase.User user = UserDatabase.getUser(testUsername);

        // Verify user was added correctly
        assertNotNull(user, "User should not be null after adding");
        assertEquals(testUsername, user.getUsername(), "Username should match");
        assertEquals(testEmail, user.getEmail(), "Email should match");
        assertEquals(testPhone, user.getPhone(), "Phone should match");
        assertEquals(testRole, user.getRole(), "Role should match");
    }

    @Test
    void testAuthenticateUser() {
        // Add a new user
        UserDatabase.addUser(testUsername, testEmail, testPhone, testPassword, testRole);
        trackUser(testUsername);

        // Test valid authentication
        boolean validAuth = UserDatabase.authenticateUser(testUsername, testPassword);
        assertTrue(validAuth, "Authentication should succeed with correct credentials");

        // Test invalid authentication with wrong password
        boolean invalidAuth = UserDatabase.authenticateUser(testUsername, "wrongPassword");
        assertFalse(invalidAuth, "Authentication should fail with incorrect password");

        // Test invalid authentication with non-existent user
        boolean nonExistentAuth = UserDatabase.authenticateUser("nonexistentuser", testPassword);
        assertFalse(nonExistentAuth, "Authentication should fail with non-existent user");
    }

    @Test
    void testChangeUserRole() {
        // Add a new user
        UserDatabase.addUser(testUsername, testEmail, testPhone, testPassword, testRole);
        trackUser(testUsername);

        // Verify initial role
        UserDatabase.User user = UserDatabase.getUser(testUsername);
        assertEquals(testRole, user.getRole(), "Initial role should be 'viewer'");

        // Change role to editor
        boolean changeResult = UserDatabase.changeUserRole(testUsername, "editor");
        assertTrue(changeResult, "Role change should succeed");

        // Verify updated role
        user = UserDatabase.getUser(testUsername);
        assertEquals("editor", user.getRole(), "Updated role should be 'editor'");
    }

    @Test
    void testGetUserRole() {
        // Add a new user
        UserDatabase.addUser(testUsername, testEmail, testPhone, testPassword, testRole);
        trackUser(testUsername);

        // Get user role
        String role = UserDatabase.getUserRole(testUsername);

        // Verify role matches
        assertEquals(testRole, role, "Retrieved role should match the assigned role");

        // Test non-existent user
        String nonExistentRole = UserDatabase.getUserRole("nonexistentuser");
        assertNull(nonExistentRole, "Role should be null for non-existent user");
    }

    @Test
    void testGetAllUsers() {
        // Add a new user
        UserDatabase.addUser(testUsername, testEmail, testPhone, testPassword, testRole);
        trackUser(testUsername);

        // Get all users
        List<UserDatabase.User> users = UserDatabase.getAllUsers();

        // Verify we have at least one user (the admin user should always exist)
        assertFalse(users.isEmpty(), "User list should not be empty");

        // Verify our test user is in the list
        boolean foundTestUser = users.stream()
            .anyMatch(user -> user.getUsername().equals(testUsername));
        assertTrue(foundTestUser, "Test user should be in the list of all users");
    }

    @Test
    void testRoleBasedPermissions() {
        // Add a viewer user
        String viewerUsername = testUsername + "_viewer";
        String viewerEmail = "viewer_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String viewerPhone = "1" + (System.currentTimeMillis() % 10000000000L + 1);
        UserDatabase.addUser(viewerUsername, viewerEmail, viewerPhone, testPassword, "viewer");
        trackUser(viewerUsername);
        UserDatabase.User viewerUser = UserDatabase.getUser(viewerUsername);

        // Add an editor user
        String editorUsername = testUsername + "_editor";
        String editorEmail = "editor_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String editorPhone = "1" + (System.currentTimeMillis() % 10000000000L + 2);
        UserDatabase.addUser(editorUsername, editorEmail, editorPhone, testPassword, "editor");
        trackUser(editorUsername);
        UserDatabase.User editorUser = UserDatabase.getUser(editorUsername);

        // Add an admin user
        String adminUsername = testUsername + "_admin";
        String adminEmail = "admin_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String adminPhone = "1" + (System.currentTimeMillis() % 10000000000L + 3);
        UserDatabase.addUser(adminUsername, adminEmail, adminPhone, testPassword, "admin");
        trackUser(adminUsername);
        UserDatabase.User adminUser = UserDatabase.getUser(adminUsername);

        // Verify permissions
        assertNotNull(viewerUser, "Viewer user should be created successfully");
        assertNotNull(editorUser, "Editor user should be created successfully");
        assertNotNull(adminUser, "Admin user should be created successfully");

        // Viewer permissions
        assertTrue(viewerUser.isViewer(), "Viewer should have viewer permissions");
        assertFalse(viewerUser.isEditor(), "Viewer should not have editor permissions");
        assertFalse(viewerUser.isAdmin(), "Viewer should not have admin permissions");

        // Editor permissions
        assertTrue(editorUser.isViewer(), "Editor should have viewer permissions");
        assertTrue(editorUser.isEditor(), "Editor should have editor permissions");
        assertFalse(editorUser.isAdmin(), "Editor should not have admin permissions");

        // Admin permissions
        assertTrue(adminUser.isViewer(), "Admin should have viewer permissions");
        assertTrue(adminUser.isEditor(), "Admin should have editor permissions");
        assertTrue(adminUser.isAdmin(), "Admin should have admin permissions");
    }

    @Test
    void testDeleteUser() {
        // Add a new user
        UserDatabase.addUser(testUsername, testEmail, testPhone, testPassword, testRole);
        UserDatabase.User user = UserDatabase.getUser(testUsername);
        assertNotNull(user, "User should exist after adding");

        // Delete the user
        UserDatabase.deleteUser(user.getId());

        // Verify user no longer exists
        UserDatabase.User deletedUser = UserDatabase.getUser(testUsername);
        assertNull(deletedUser, "User should not exist after deletion");
    }

    @Test
    void testUpdateUser() {
        // Add a new user
        UserDatabase.addUser(testUsername, testEmail, testPhone, testPassword, testRole);
        trackUser(testUsername);
        UserDatabase.User user = UserDatabase.getUser(testUsername);
        assertNotNull(user, "User should not be null after adding");

        // Updated information
        String updatedEmail = "updated@example.com";
        String updatedPhone = "9876543210";
        String updatedPassword = "updatedPassword";
        String updatedRole = "editor";

        // Update the user
        UserDatabase.updateUser(user.getId(), testUsername, updatedEmail, updatedPhone, updatedPassword, updatedRole);

        // Get updated user
        UserDatabase.User updatedUser = UserDatabase.getUser(testUsername);
        assertNotNull(updatedUser, "User should still exist after update");

        // Verify updated information
        assertEquals(updatedEmail, updatedUser.getEmail(), "Email should be updated");
        assertEquals(updatedPhone, updatedUser.getPhone(), "Phone should be updated");
        assertEquals(updatedRole, updatedUser.getRole(), "Role should be updated");

        // Verify authentication with new password works
        boolean authResult = UserDatabase.authenticateUser(testUsername, updatedPassword);
        assertTrue(authResult, "Authentication should work with updated password");
    }

    @Test
    void testInitialDatabaseState() {
        // Verify admin user exists
        UserDatabase.User adminUser = UserDatabase.getUser("admin");
        assertNotNull(adminUser, "Admin user should exist in the database");
        assertEquals("admin", adminUser.getRole(), "Admin user should have admin role");
    }
}