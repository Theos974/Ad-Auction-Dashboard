package com.example.ad_auction_dashboard.ComponentTests;

import static org.junit.jupiter.api.Assertions.*;
import com.example.ad_auction_dashboard.logic.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Tests for the integration between various input components
 * This class focuses on how different components interact when processing input data
 */
public class InputComponentTest {

    private FileHandler fileHandler;

    @BeforeEach
    void setup() {
        fileHandler = new FileHandler();
    }

    @Test
    @DisplayName("Test end-to-end CSV processing to Campaign creation")
    void testCSVProcessingToCampaign(@TempDir Path tempDir) throws IOException {
        // Create test CSV files
        File impressionCsv = createImpressionCsv(tempDir);
        File clickCsv = createClickCsv(tempDir);
        File serverCsv = createServerCsv(tempDir);

        // Process CSV files into log arrays
        ImpressionLog[] impressionLogs = (ImpressionLog[]) fileHandler.openIndividualCSV(impressionCsv.getAbsolutePath());
        ClickLog[] clickLogs = (ClickLog[]) fileHandler.openIndividualCSV(clickCsv.getAbsolutePath());
        ServerLog[] serverLogs = (ServerLog[]) fileHandler.openIndividualCSV(serverCsv.getAbsolutePath());

        // Create a campaign from the logs
        Campaign campaign = new Campaign(impressionLogs, clickLogs, serverLogs);

        // Verify the campaign was created correctly
        assertNotNull(campaign, "Campaign should be created successfully");
        assertEquals(2, campaign.getImpressionLogs().length, "Campaign should contain 2 impression logs");
        assertEquals(2, campaign.getClickLogs().length, "Campaign should contain 2 click logs");
        assertEquals(2, campaign.getServerLogs().length, "Campaign should contain 2 server logs");

        // Verify the data was processed correctly
        assertEquals("1001", campaign.getImpressionLogs()[0].getId(), "First impression ID should match");
        assertEquals(0.123456f, campaign.getImpressionLogs()[0].getImpressionCost(), 0.000001f, "First impression cost should match");
        assertEquals("1001", campaign.getClickLogs()[0].getId(), "First click ID should match");
        assertEquals(1.23f, campaign.getClickLogs()[0].getClickCost(), 0.000001f, "First click cost should match");
    }

    @Test
    @DisplayName("Test ZIP file processing to metrics calculations")
    void testZipToMetricsCalculation(@TempDir Path tempDir) throws IOException {
        // Create a test ZIP file with all three CSV files
        File zipFile = createTestZipFile(tempDir);

        // Process ZIP file into a campaign
        Campaign campaign = fileHandler.openZip(zipFile.getAbsolutePath());
        assertNotNull(campaign, "Campaign should be created from ZIP file");

        // Create metrics from the campaign
        CampaignMetrics metrics = new CampaignMetrics(campaign);
        assertNotNull(metrics, "Metrics should be created from campaign");

        // Verify the metrics calculations
        assertEquals(2, metrics.getNumberOfImpressions(), "Should count 2 impressions");
        assertEquals(2, metrics.getNumberOfClicks(), "Should count 2 clicks");
        assertEquals(2, metrics.getNumberOfUniques(), "Should count 2 unique users");
        assertEquals(1, metrics.getNumberOfConversions(), "Should count 1 conversion");

        // Verify derived metrics
        double expectedCost = 0.123456 + 0.234567 + 1.23 + 0.75;
        assertEquals(expectedCost, metrics.getTotalCost(), 0.0001, "Total cost should be calculated correctly");
        assertEquals(1.0, metrics.getCTR(), 0.0001, "CTR should be calculated correctly");
    }

    @Test
    @DisplayName("Test input validation with metrics impact")
    void testInputValidationWithMetricsImpact() {
        // Create logs with both valid and invalid data
        ImpressionLog[] impressions = new ImpressionLog[] {
            new ImpressionLog("2023-03-01 10:00:00", "1001", "Male", "<25", "Medium", "News", "0.123456"),
            new ImpressionLog("2023-03-01 11:00:00", "1002", "Unknown", "25-34", "High", "Shopping", "0.234567"),
            new ImpressionLog("2023-03-01 12:00:00", "1003", "Female", "35-44", "Low", "Blog", "-0.100000")
        };

        ClickLog[] clicks = new ClickLog[] {
            // Valid click
            new ClickLog("2023-03-01 10:05:00", "1001", "1.230000"),
            new ClickLog("2023-03-01 11:05:00", "1002", "invalid")
        };

        ServerLog[] servers = new ServerLog[] {
            new ServerLog("2023-03-01 10:05:30", "1001", "2023-03-01 10:10:30", "5", "Yes"),
            new ServerLog("2023-03-01 11:05:30", "1002", "2023-03-01 11:10:30", "3", "No")
        };

        // Create campaign and metrics
        Campaign campaign = new Campaign(impressions, clicks, servers);
        CampaignMetrics metrics = new CampaignMetrics(campaign);

        assertEquals(3, metrics.getNumberOfImpressions(), "All 3 impressions should be counted");
        assertEquals(2, metrics.getNumberOfClicks(), "Both clicks should be counted");
        assertEquals(1, metrics.getNumberOfConversions(), "Only valid conversion should be counted");


        double expectedCost = (0.123456 + 0.234567 + 1.23) - 2.0; // = -0.411977
        assertEquals(expectedCost, metrics.getTotalCost(), 0.0001,
            "Invalid costs are set to -1, so they lower the total by 1 each");

        TimeFilteredMetrics filteredMetrics = new TimeFilteredMetrics(
            impressions, servers, clicks,
            metrics.getBouncePagesThreshold(),
            metrics.getBounceSecondsThreshold()
        );
        filteredMetrics.setGenderFilter("Male");
        filteredMetrics.computeForTimeFrame(
            LocalDateTime.of(2023, 1, 1, 0, 0),
            LocalDateTime.of(2023, 12, 31, 23, 59, 59),
            "Daily"
        );
        assertEquals(1, filteredMetrics.getNumberOfImpressions(),
            "Only impressions with valid Male gender should be counted");
    }


    @Test
    @DisplayName("Test interaction between FileHandler and input validation")
    void testFileHandlerInputValidation() {
        // Test the proper parsing and validation of data by the FileHandler
        String[] validCsvLines = {
            "Date,ID,Gender,Age,Income,Context,Impression Cost",
            "2023-03-01 10:00:00,1001,Male,<25,Medium,News,0.123456"
        };

        String[][] splitData = fileHandler.splitImpressions(validCsvLines);
        assertNotNull(splitData, "Valid CSV should be parsed successfully");
        assertEquals(1, splitData.length, "Should contain 1 data row");
        assertEquals(7, splitData[0].length, "Should contain 7 fields");

        // Test with invalid data
        String[] invalidCsvLines = {
            "Date,ID,Gender,Age,Income,Context,Impression Cost",
            "2023-03-01 10:00:00,1001,Male,<25,Medium,News,0.123456,ExtraField"
        };

        String[][] invalidSplitData = fileHandler.splitImpressions(invalidCsvLines);
        assertNull(invalidSplitData, "Invalid CSV should return null");
    }

    // Helper methods to create test files

    private File createImpressionCsv(Path tempDir) throws IOException {
        File file = tempDir.resolve("impression_log.csv").toFile();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("Date,ID,Gender,Age,Income,Context,Impression Cost\n" +
                "2023-03-01 10:00:00,1001,Male,<25,Medium,News,0.123456\n" +
                "2023-03-01 11:00:00,1002,Female,25-34,High,Shopping,0.234567");
        }
        return file;
    }

    private File createClickCsv(Path tempDir) throws IOException {
        File file = tempDir.resolve("click_log.csv").toFile();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("Date,ID,Click Cost\n" +
                "2023-03-01 10:05:00,1001,1.230000\n" +
                "2023-03-01 11:05:00,1002,0.750000");
        }
        return file;
    }

    private File createServerCsv(Path tempDir) throws IOException {
        File file = tempDir.resolve("server_log.csv").toFile();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("Entry Date,ID,Exit Date,Pages Viewed,Conversion\n" +
                "2023-03-01 10:05:30,1001,2023-03-01 10:10:30,5,Yes\n" +
                "2023-03-01 11:05:30,1002,2023-03-01 11:10:30,3,No");
        }
        return file;
    }

    private File createTestZipFile(Path tempDir) throws IOException {
        File zipFile = tempDir.resolve("test_campaign.zip").toFile();

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            // Add impression log
            ZipEntry impressionEntry = new ZipEntry("impression_log.csv");
            zos.putNextEntry(impressionEntry);
            String impressionContent = "Date,ID,Gender,Age,Income,Context,Impression Cost\n" +
                "2023-03-01 10:00:00,1001,Male,<25,Medium,News,0.123456\n" +
                "2023-03-01 11:00:00,1002,Female,25-34,High,Shopping,0.234567";
            zos.write(impressionContent.getBytes());
            zos.closeEntry();

            // Add click log
            ZipEntry clickEntry = new ZipEntry("click_log.csv");
            zos.putNextEntry(clickEntry);
            String clickContent = "Date,ID,Click Cost\n" +
                "2023-03-01 10:05:00,1001,1.230000\n" +
                "2023-03-01 11:05:00,1002,0.750000";
            zos.write(clickContent.getBytes());
            zos.closeEntry();

            // Add server log
            ZipEntry serverEntry = new ZipEntry("server_log.csv");
            zos.putNextEntry(serverEntry);
            String serverContent = "Entry Date,ID,Exit Date,Pages Viewed,Conversion\n" +
                "2023-03-01 10:05:30,1001,2023-03-01 10:10:30,5,Yes\n" +
                "2023-03-01 11:05:30,1002,2023-03-01 11:10:30,3,No";
            zos.write(serverContent.getBytes());
            zos.closeEntry();
        }

        return zipFile;
    }
}