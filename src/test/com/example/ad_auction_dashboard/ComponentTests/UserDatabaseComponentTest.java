package com.example.ad_auction_dashboard.ComponentTests;


import static org.junit.jupiter.api.Assertions.*;
import com.example.ad_auction_dashboard.logic.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserDatabaseComponentTest {

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

}