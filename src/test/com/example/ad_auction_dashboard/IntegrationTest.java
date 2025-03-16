package com.example.ad_auction_dashboard;

import com.example.ad_auction_dashboard.logic.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the Ad Auction Dashboard
 * Tests interactions between different components of the system
 */
public class IntegrationTest {

    // Test files
    private File testZipFile;

    // Test users
    private UserDatabase.User editorUser;
    private UserDatabase.User viewerUser;

    // Test campaign data
    private Campaign testCampaign;
    private CampaignMetrics testMetrics;
    private int savedCampaignId = -1;

    @BeforeEach
    void setUp() throws Exception {
        // Create unique IDs for test
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);

        // Create test zip file with campaign data
        createTestZipFile(uniqueId);

        // Create test users
        String editorUsername = "editor_" + uniqueId;
        UserDatabase.addUser(editorUsername, "editor_" + uniqueId + "@example.com",
            "1" + System.currentTimeMillis() % 10000000000L, "password", "editor");
        editorUser = UserDatabase.getUser(editorUsername);

        String viewerUsername = "viewer_" + uniqueId;
        UserDatabase.addUser(viewerUsername, "viewer_" + uniqueId + "@example.com",
            "2" + System.currentTimeMillis() % 10000000000L, "password", "viewer");
        viewerUser = UserDatabase.getUser(viewerUsername);
    }

    @AfterEach
    void tearDown() {
        // Delete test zip file
        if (testZipFile != null && testZipFile.exists()) {
            testZipFile.delete();
        }

        // Delete saved campaign
        if (savedCampaignId > 0) {
            CampaignDatabase.deleteCampaign(savedCampaignId);
        }

        // Delete test users
        if (editorUser != null) UserDatabase.deleteUser(editorUser.getId());
        if (viewerUser != null) UserDatabase.deleteUser(viewerUser.getId());
    }

    /**
     * Creates a test ZIP file with campaign data
     */
    private void createTestZipFile(String uniqueId) throws Exception {
        // Create test file
        testZipFile = File.createTempFile("test_campaign_" + uniqueId, ".zip");

        // Create ZIP with campaign data
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(testZipFile))) {
            // Add impression log
            ZipEntry impressionEntry = new ZipEntry("impression_log.csv");
            zipOut.putNextEntry(impressionEntry);
            String impressionData = "Date,ID,Gender,Age,Income,Context,Impression Cost\n" +
                "2023-03-01 10:00:00,1001,Male,<25,Medium,News,0.123456\n" +
                "2023-03-01 10:30:00,1002,Female,25-34,High,Shopping,0.234567\n" +
                "2023-03-01 11:00:00,1003,Male,35-44,Low,Blog,0.345678";
            zipOut.write(impressionData.getBytes());
            zipOut.closeEntry();

            // Add click log
            ZipEntry clickEntry = new ZipEntry("click_log.csv");
            zipOut.putNextEntry(clickEntry);
            String clickData = "Date,ID,Click Cost\n" +
                "2023-03-01 10:05:00,1001,1.230000\n" +
                "2023-03-01 10:35:00,1002,1.450000\n" +
                "2023-03-01 11:05:00,1003,1.670000";
            zipOut.write(clickData.getBytes());
            zipOut.closeEntry();

            // Add server log
            ZipEntry serverEntry = new ZipEntry("server_log.csv");
            zipOut.putNextEntry(serverEntry);
            String serverData = "Entry Date,ID,Exit Date,Pages Viewed,Conversion\n" +
                "2023-03-01 10:05:30,1001,2023-03-01 10:10:30,3,Yes\n" +
                "2023-03-01 10:35:30,1002,2023-03-01 10:40:30,5,No\n" +
                "2023-03-01 11:05:30,1003,2023-03-01 11:15:30,7,Yes";
            zipOut.write(serverData.getBytes());
            zipOut.closeEntry();
        }
    }

    @Test
    void testEndToEndCampaignWorkflow() {
        // 1. Load campaign from zip file (role: editor)
        FileHandler fileHandler = new FileHandler();
        testCampaign = fileHandler.openZip(testZipFile.getAbsolutePath());

        // Verify campaign was loaded correctly
        assertNotNull(testCampaign, "Campaign should be loaded from ZIP file");
        assertEquals(3, testCampaign.getImpressionLogs().length, "Should have 3 impression logs");
        assertEquals(3, testCampaign.getClickLogs().length, "Should have 3 click logs");
        assertEquals(3, testCampaign.getServerLogs().length, "Should have 3 server logs");

        // 2. Create campaign metrics
        testMetrics = new CampaignMetrics(testCampaign);

        // Verify metrics are calculated correctly
        assertEquals(3, testMetrics.getNumberOfImpressions(), "Should have 3 impressions");
        assertEquals(3, testMetrics.getNumberOfClicks(), "Should have 3 clicks");
        assertEquals(3, testMetrics.getNumberOfUniques(), "Should have 3 unique users");
        assertEquals(2, testMetrics.getNumberOfConversions(), "Should have 2 conversions");

        // 3. Save campaign to database
        String campaignName = "Integration Test Campaign";
        savedCampaignId = CampaignDatabase.saveCampaign(testMetrics, campaignName, editorUser.getId());

        assertTrue(savedCampaignId > 0, "Campaign should be saved with valid ID");

        // 4. Assign campaign to viewer
        boolean assigned = CampaignDatabase.assignCampaignToUser(savedCampaignId, viewerUser.getId(), editorUser.getId());
        assertTrue(assigned, "Campaign should be assigned to viewer");

        // 5. Load campaign as viewer
        Campaign loadedCampaign = CampaignDatabase.loadCampaign(savedCampaignId);
        assertNotNull(loadedCampaign, "Viewer should be able to load assigned campaign");

        // 6. Create filtered metrics for loaded campaign
        CampaignMetrics loadedMetrics = new CampaignMetrics(loadedCampaign);
        TimeFilteredMetrics filteredMetrics = new TimeFilteredMetrics(
            loadedMetrics.getImpressionLogs(),
            loadedMetrics.getServerLogs(),
            loadedMetrics.getClickLogs(),
            loadedMetrics.getBouncePagesThreshold(),
            loadedMetrics.getBounceSecondsThreshold()
        );

        // 7. Apply filters and compute metrics
        filteredMetrics.setGenderFilter("Male");

        // Get date range for the campaign
        LocalDateTime startDate = loadedMetrics.getCampaignStartDate();
        LocalDateTime endDate = loadedMetrics.getCampaignEndDate();
        assertNotNull(startDate, "Campaign start date should not be null");
        assertNotNull(endDate, "Campaign end date should not be null");

        // Compute metrics with filter
        filteredMetrics.computeForTimeFrame(startDate, endDate, "Daily");

        // Verify filtered metrics
        assertEquals(2, filteredMetrics.getNumberOfImpressions(), "Should have 2 male impressions");
        assertEquals(2, filteredMetrics.getNumberOfClicks(), "Should have 2 male clicks");
        assertEquals(2, filteredMetrics.getNumberOfUniques(), "Should have 2 male uniques");
        assertEquals(2, filteredMetrics.getNumberOfConversions(), "Should have 2 male conversions");

        // 8. Test changing bounce criteria
        loadedMetrics.setBounceCriteria(2, 300); // Pages ≤ 2, Time ≤ 300 seconds

        // Verify bounce metrics updated
        int bounces = loadedMetrics.getNumberOfBounces();
        assertEquals(2, bounces, "Should have 2 bounce with updated criteria");
    }

    @Test
    void testCampaignAccessControl() {
        // 1. Load and save campaign as editor
        FileHandler fileHandler = new FileHandler();
        testCampaign = fileHandler.openZip(testZipFile.getAbsolutePath());
        testMetrics = new CampaignMetrics(testCampaign);

        String campaignName = "Access Control Test Campaign";
        savedCampaignId = CampaignDatabase.saveCampaign(testMetrics, campaignName, editorUser.getId());

        // 2. Verify initial access state
        boolean editorAccess = CampaignDatabase.canUserAccessCampaign(editorUser.getId(), savedCampaignId);
        boolean viewerAccess = CampaignDatabase.canUserAccessCampaign(viewerUser.getId(), savedCampaignId);

        assertTrue(editorAccess, "Editor (owner) should have access to campaign");
        assertFalse(viewerAccess, "Viewer should not have access to campaign initially");

        // 3. Get accessible campaigns for each user
        List<CampaignDatabase.CampaignInfo> editorCampaigns = CampaignDatabase.getAccessibleCampaigns(editorUser.getId());
        List<CampaignDatabase.CampaignInfo> viewerCampaigns = CampaignDatabase.getAccessibleCampaigns(viewerUser.getId());

        boolean editorSeesCampaign = editorCampaigns.stream()
            .anyMatch(c -> c.getCampaignId() == savedCampaignId);
        boolean viewerSeesCampaign = viewerCampaigns.stream()
            .anyMatch(c -> c.getCampaignId() == savedCampaignId);

        assertTrue(editorSeesCampaign, "Editor should see campaign in accessible campaigns");
        assertFalse(viewerSeesCampaign, "Viewer should not see campaign in accessible campaigns");

        // 4. Grant access to viewer
        boolean granted = CampaignDatabase.assignCampaignToUser(savedCampaignId, viewerUser.getId(), editorUser.getId());
        assertTrue(granted, "Access should be granted successfully");

        // 5. Verify access after assignment
        viewerAccess = CampaignDatabase.canUserAccessCampaign(viewerUser.getId(), savedCampaignId);
        assertTrue(viewerAccess, "Viewer should have access after assignment");

        // 6. Get updated accessible campaigns
        viewerCampaigns = CampaignDatabase.getAccessibleCampaigns(viewerUser.getId());
        viewerSeesCampaign = viewerCampaigns.stream()
            .anyMatch(c -> c.getCampaignId() == savedCampaignId);
        assertTrue(viewerSeesCampaign, "Viewer should now see campaign in accessible campaigns");

        // 7. Verify viewer can load campaign
        Campaign viewerLoadedCampaign = CampaignDatabase.loadCampaign(savedCampaignId);
        assertNotNull(viewerLoadedCampaign, "Viewer should be able to load campaign");
    }

    @Test
    void testFilteringAndMetricsConsistency() {
        // 1. Load campaign from zip file
        FileHandler fileHandler = new FileHandler();
        testCampaign = fileHandler.openZip(testZipFile.getAbsolutePath());
        testMetrics = new CampaignMetrics(testCampaign);

        // 2. Create TimeFilteredMetrics with no filters (should match regular metrics)
        TimeFilteredMetrics unfilteredMetrics = new TimeFilteredMetrics(
            testMetrics.getImpressionLogs(),
            testMetrics.getServerLogs(),
            testMetrics.getClickLogs(),
            testMetrics.getBouncePagesThreshold(),
            testMetrics.getBounceSecondsThreshold()
        );

        // Compute with no filters for entire date range
        LocalDateTime startDate = testMetrics.getCampaignStartDate();
        LocalDateTime endDate = testMetrics.getCampaignEndDate();
        unfilteredMetrics.computeForTimeFrame(startDate, endDate, "Daily");

        // Verify unfiltered metrics match regular metrics
        assertEquals(testMetrics.getNumberOfImpressions(), unfilteredMetrics.getNumberOfImpressions(),
            "Unfiltered impression count should match regular metrics");
        assertEquals(testMetrics.getNumberOfClicks(), unfilteredMetrics.getNumberOfClicks(),
            "Unfiltered click count should match regular metrics");
        assertEquals(testMetrics.getNumberOfConversions(), unfilteredMetrics.getNumberOfConversions(),
            "Unfiltered conversion count should match regular metrics");

        // 3. Apply different combined filters
        TimeFilteredMetrics filteredMetrics = new TimeFilteredMetrics(
            testMetrics.getImpressionLogs(),
            testMetrics.getServerLogs(),
            testMetrics.getClickLogs(),
            testMetrics.getBouncePagesThreshold(),
            testMetrics.getBounceSecondsThreshold()
        );

        // Apply gender + context filter
        filteredMetrics.setGenderFilter("Male");
        filteredMetrics.setContextFilter("News");
        filteredMetrics.computeForTimeFrame(startDate, endDate, "Daily");

        // Verify filtered metrics
        assertEquals(1, filteredMetrics.getNumberOfImpressions(), "Should have 1 impression for Male + News");
        assertEquals(1, filteredMetrics.getNumberOfClicks(), "Should have 1 click for Male + News");
        assertEquals(1, filteredMetrics.getNumberOfConversions(), "Should have 1 conversion for Male + News");

        // 4. Apply different time granularities
        filteredMetrics.setGenderFilter("Female");
        filteredMetrics.setContextFilter(null);

        // Try with hourly granularity
        filteredMetrics.computeForTimeFrame(startDate, endDate, "Hourly");
        int hourlyImpressions = filteredMetrics.getNumberOfImpressions();

        // Try with daily granularity
        filteredMetrics.computeForTimeFrame(startDate, endDate, "Daily");
        int dailyImpressions = filteredMetrics.getNumberOfImpressions();

        // Results should be consistent across granularities
        assertEquals(hourlyImpressions, dailyImpressions,
            "Impression count should be consistent between hourly and daily granularity");

        // 5. Test time-specific filtering
        LocalDateTime narrowStartDate = LocalDateTime.of(2023, 3, 1, 10, 0, 0);
        LocalDateTime narrowEndDate = LocalDateTime.of(2023, 3, 1, 10, 59, 59);

        // Reset filters and apply time filter
        filteredMetrics.setGenderFilter(null);
        filteredMetrics.computeForTimeFrame(narrowStartDate, narrowEndDate, "Hourly");

        // Should only include impressions from 10:00-10:59
        assertEquals(2, filteredMetrics.getNumberOfImpressions(),
            "Should have 2 impressions in the 10:00 hour");
    }
}