package com.example.ad_auction_dashboard;

import com.example.ad_auction_dashboard.logic.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the context and audience segment filtering functionality
 * Verifies that filters correctly select data based on demographic and contextual criteria
 */
public class FilterFunctionalityTest {

    private ImpressionLog[] mockImpressionLogs;
    private ClickLog[] mockClickLogs;
    private ServerLog[] mockServerLogs;
    private TimeFilteredMetrics timeFilteredMetrics;
    private CampaignMetrics campaignMetrics;

    // Define test date range
    private final LocalDateTime testStartDate = LocalDateTime.of(2023, 3, 1, 8, 0, 0);
    private final LocalDateTime testEndDate = LocalDateTime.of(2023, 3, 1, 18, 0, 0);

    @BeforeEach
    void setUp() {
        // Set up mock impression logs with diverse demographic data
        mockImpressionLogs = new ImpressionLog[]{
            // Different genders
            new ImpressionLog("2023-03-01 10:00:00", "1001", "Male", "<25", "Medium", "News", "0.123456"),
            new ImpressionLog("2023-03-01 10:30:00", "1002", "Female", "25-34", "High", "Shopping", "0.234567"),

            // Different age groups
            new ImpressionLog("2023-03-01 11:00:00", "1003", "Male", "35-44", "Low", "Blog", "0.345678"),
            new ImpressionLog("2023-03-01 11:30:00", "1004", "Female", "45-54", "Medium", "Social Media", "0.456789"),
            new ImpressionLog("2023-03-01 12:00:00", "1005", "Male", ">54", "High", "News", "0.567890"),

            // Different income levels
            new ImpressionLog("2023-03-01 13:00:00", "1006", "Female", "<25", "Low", "Blog", "0.678901"),
            new ImpressionLog("2023-03-01 14:00:00", "1007", "Male", "25-34", "Medium", "Shopping", "0.789012"),

            // Different contexts
            new ImpressionLog("2023-03-01 15:00:00", "1008", "Female", "35-44", "High", "Travel", "0.890123"),
            new ImpressionLog("2023-03-01 16:00:00", "1009", "Male", "45-54", "Low", "Hobbies", "0.901234")
        };

        // Set up matching click logs for each impression
        mockClickLogs = new ClickLog[]{
            new ClickLog("2023-03-01 10:05:00", "1001", "1.230000"),
            new ClickLog("2023-03-01 10:35:00", "1002", "1.450000"),
            new ClickLog("2023-03-01 11:05:00", "1003", "1.670000"),
            new ClickLog("2023-03-01 11:35:00", "1004", "1.890000"),
            new ClickLog("2023-03-01 12:05:00", "1005", "2.010000"),
            new ClickLog("2023-03-01 13:05:00", "1006", "2.230000"),
            new ClickLog("2023-03-01 14:05:00", "1007", "2.450000"),
            new ClickLog("2023-03-01 15:05:00", "1008", "2.670000"),
            new ClickLog("2023-03-01 16:05:00", "1009", "2.890000")
        };

        // Set up server logs with conversions
        mockServerLogs = new ServerLog[]{
            new ServerLog("2023-03-01 10:05:30", "1001", "2023-03-01 10:10:30", "5", "Yes"), // Male, <25, News, Conversion
            new ServerLog("2023-03-01 10:35:30", "1002", "2023-03-01 10:40:30", "3", "No"),  // Female, 25-34, Shopping, No Conversion
            new ServerLog("2023-03-01 11:05:30", "1003", "2023-03-01 11:15:30", "7", "Yes"), // Male, 35-44, Blog, Conversion
            new ServerLog("2023-03-01 11:35:30", "1004", "2023-03-01 11:38:30", "2", "No"),  // Female, 45-54, Social Media, No Conversion
            new ServerLog("2023-03-01 12:05:30", "1005", "2023-03-01 12:10:30", "4", "Yes"), // Male, >54, News, Conversion
            new ServerLog("2023-03-01 13:05:30", "1006", "2023-03-01 13:20:30", "8", "Yes"), // Female, <25, Blog, Conversion
            new ServerLog("2023-03-01 14:05:30", "1007", "2023-03-01 14:07:30", "1", "No"),  // Male, 25-34, Shopping, No Conversion
            new ServerLog("2023-03-01 15:05:30", "1008", "2023-03-01 15:12:30", "3", "Yes"), // Female, 35-44, Travel, Conversion
            new ServerLog("2023-03-01 16:05:30", "1009", "2023-03-01 16:10:30", "6", "No")   // Male, 45-54, Hobbies, No Conversion
        };

        // Create campaign and campaign metrics
        Campaign campaign = new Campaign(mockImpressionLogs, mockClickLogs, mockServerLogs);
        campaignMetrics = new CampaignMetrics(campaign);

        // Initialize TimeFilteredMetrics with bounce criteria
        timeFilteredMetrics = new TimeFilteredMetrics(
            mockImpressionLogs,
            mockServerLogs,
            mockClickLogs,
            1, // bouncePagesThreshold
            60  // bounceSecondsThreshold
        );
    }

    @Test
    void testGenderFiltering() {
        // Test filtering by Male gender
        timeFilteredMetrics.setGenderFilter("Male");
        timeFilteredMetrics.computeForTimeFrame(testStartDate, testEndDate, "Daily");

        // There are 5 Male impressions in the test data
        assertEquals(5, timeFilteredMetrics.getNumberOfImpressions(), "Should count 5 Male impressions");
        assertEquals(5, timeFilteredMetrics.getNumberOfClicks(), "Should count 5 Male clicks");
        assertEquals(5, timeFilteredMetrics.getNumberOfUniques(), "Should count 5 Male uniques");
        assertEquals(3, timeFilteredMetrics.getNumberOfConversions(), "Should count 3 Male conversions");

        // Test filtering by Female gender
        timeFilteredMetrics.setGenderFilter("Female");
        timeFilteredMetrics.computeForTimeFrame(testStartDate, testEndDate, "Daily");

        // There are 4 Female impressions in the test data
        assertEquals(4, timeFilteredMetrics.getNumberOfImpressions(), "Should count 4 Female impressions");
        assertEquals(4, timeFilteredMetrics.getNumberOfClicks(), "Should count 4 Female clicks");
        assertEquals(4, timeFilteredMetrics.getNumberOfUniques(), "Should count 4 Female uniques");
        assertEquals(2, timeFilteredMetrics.getNumberOfConversions(), "Should count 2 Female conversions");
    }

    @Test
    void testAgeFiltering() {
        // Test filtering by age group <25
        timeFilteredMetrics.setAgeFilter("<25");
        timeFilteredMetrics.computeForTimeFrame(testStartDate, testEndDate, "Daily");

        // There are 2 impressions with age <25
        assertEquals(2, timeFilteredMetrics.getNumberOfImpressions(), "Should count 2 impressions for age <25");
        assertEquals(2, timeFilteredMetrics.getNumberOfClicks(), "Should count 2 clicks for age <25");
        assertEquals(2, timeFilteredMetrics.getNumberOfUniques(), "Should count 2 uniques for age <25");
        assertEquals(2, timeFilteredMetrics.getNumberOfConversions(), "Should count 2 conversions for age <25");

        // Test filtering by age group 25-34
        timeFilteredMetrics.setAgeFilter("25-34");
        timeFilteredMetrics.computeForTimeFrame(testStartDate, testEndDate, "Daily");

        // There are 2 impressions with age 25-34
        assertEquals(2, timeFilteredMetrics.getNumberOfImpressions(), "Should count 2 impressions for age 25-34");
        assertEquals(2, timeFilteredMetrics.getNumberOfClicks(), "Should count 2 clicks for age 25-34");
        assertEquals(2, timeFilteredMetrics.getNumberOfUniques(), "Should count 2 uniques for age 25-34");
        assertEquals(0, timeFilteredMetrics.getNumberOfConversions(), "Should count 0 conversions for age 25-34");
    }

    @Test
    void testIncomeFiltering() {
        // Test filtering by Low income
        timeFilteredMetrics.setIncomeFilter("Low");
        timeFilteredMetrics.computeForTimeFrame(testStartDate, testEndDate, "Daily");

        // There are 3 impressions with Low income
        assertEquals(3, timeFilteredMetrics.getNumberOfImpressions(), "Should count 3 impressions for Low income");
        assertEquals(3, timeFilteredMetrics.getNumberOfClicks(), "Should count 3 clicks for Low income");
        assertEquals(3, timeFilteredMetrics.getNumberOfUniques(), "Should count 3 uniques for Low income");
        assertEquals(2, timeFilteredMetrics.getNumberOfConversions(), "Should count 2 conversion for Low income");

        // Test filtering by High income
        timeFilteredMetrics.setIncomeFilter("High");
        timeFilteredMetrics.computeForTimeFrame(testStartDate, testEndDate, "Daily");

        // There are 3 impressions with High income
        assertEquals(3, timeFilteredMetrics.getNumberOfImpressions(), "Should count 3 impressions for High income");
        assertEquals(3, timeFilteredMetrics.getNumberOfClicks(), "Should count 3 clicks for High income");
        assertEquals(3, timeFilteredMetrics.getNumberOfUniques(), "Should count 3 uniques for High income");
        assertEquals(2, timeFilteredMetrics.getNumberOfConversions(), "Should count 2 conversions for High income");
    }

    @Test
    void testContextFiltering() {
        // Test filtering by News context
        timeFilteredMetrics.setContextFilter("News");
        timeFilteredMetrics.computeForTimeFrame(testStartDate, testEndDate, "Daily");

        // There are 2 impressions with News context
        assertEquals(2, timeFilteredMetrics.getNumberOfImpressions(), "Should count 2 impressions for News context");
        assertEquals(2, timeFilteredMetrics.getNumberOfClicks(), "Should count 2 clicks for News context");
        assertEquals(2, timeFilteredMetrics.getNumberOfUniques(), "Should count 2 uniques for News context");
        assertEquals(2, timeFilteredMetrics.getNumberOfConversions(), "Should count 2 conversions for News context");

        // Test filtering by Blog context
        timeFilteredMetrics.setContextFilter("Blog");
        timeFilteredMetrics.computeForTimeFrame(testStartDate, testEndDate, "Daily");

        // There are 2 impressions with Blog context
        assertEquals(2, timeFilteredMetrics.getNumberOfImpressions(), "Should count 2 impressions for Blog context");
        assertEquals(2, timeFilteredMetrics.getNumberOfClicks(), "Should count 2 clicks for Blog context");
        assertEquals(2, timeFilteredMetrics.getNumberOfUniques(), "Should count 2 uniques for Blog context");
        assertEquals(2, timeFilteredMetrics.getNumberOfConversions(), "Should count 2 conversions for Blog context");
    }

    @Test
    void testCombinedFiltering() {
        // Test combined filtering: Male + <25 + News
        timeFilteredMetrics.setGenderFilter("Male");
        timeFilteredMetrics.setAgeFilter("<25");
        timeFilteredMetrics.setContextFilter("News");
        timeFilteredMetrics.computeForTimeFrame(testStartDate, testEndDate, "Daily");

        // There is 1 impression matching all criteria
        assertEquals(1, timeFilteredMetrics.getNumberOfImpressions(), "Should count 1 impression for Male, <25, News");
        assertEquals(1, timeFilteredMetrics.getNumberOfClicks(), "Should count 1 click for Male, <25, News");
        assertEquals(1, timeFilteredMetrics.getNumberOfUniques(), "Should count 1 unique for Male, <25, News");
        assertEquals(1, timeFilteredMetrics.getNumberOfConversions(), "Should count 1 conversion for Male, <25, News");

        // Test combined filtering: Female + 35-44 + High income
        timeFilteredMetrics.setGenderFilter("Female");
        timeFilteredMetrics.setAgeFilter("35-44");
        timeFilteredMetrics.setIncomeFilter("High");
        timeFilteredMetrics.setContextFilter(null); // Clear context filter
        timeFilteredMetrics.computeForTimeFrame(testStartDate, testEndDate, "Daily");

        // There is 1 impression matching all criteria
        assertEquals(1, timeFilteredMetrics.getNumberOfImpressions(), "Should count 1 impression for Female, 35-44, High income");
        assertEquals(1, timeFilteredMetrics.getNumberOfClicks(), "Should count 1 click for Female, 35-44, High income");
        assertEquals(1, timeFilteredMetrics.getNumberOfUniques(), "Should count 1 unique for Female, 35-44, High income");
        assertEquals(1, timeFilteredMetrics.getNumberOfConversions(), "Should count 1 conversion for Female, 35-44, High income");
    }

    @Test
    void testResettingFilters() {
        // Apply filters
        timeFilteredMetrics.setGenderFilter("Male");
        timeFilteredMetrics.setContextFilter("News");
        timeFilteredMetrics.computeForTimeFrame(testStartDate, testEndDate, "Daily");

        // Verify filtered results
        assertEquals(2, timeFilteredMetrics.getNumberOfImpressions(), "Should count 2 impressions for Male + News");

        // Reset filters
        timeFilteredMetrics.setGenderFilter(null);
        timeFilteredMetrics.setContextFilter(null);
        timeFilteredMetrics.computeForTimeFrame(testStartDate, testEndDate, "Daily");

        // Verify unfiltered results
        assertEquals(9, timeFilteredMetrics.getNumberOfImpressions(), "Should count all 9 impressions after resetting filters");
    }

    @Test
    void testFilteringWithGranularity() {
        // Apply gender filter
        timeFilteredMetrics.setGenderFilter("Female");

        // Get metrics with hourly granularity
        Map<String, TimeFilteredMetrics.ComputedMetrics> hourlyMetrics =
            timeFilteredMetrics.computeForTimeFrameWithGranularity(testStartDate, testEndDate, "Hourly");

        // Verify we have metrics for specific hours with Female impressions
        assertTrue(hourlyMetrics.containsKey("10:00") || hourlyMetrics.containsKey("2023-03-01T10:00") ||
            hourlyMetrics.containsKey("2023-03-01T10:00:00"), "Should have metrics for 10:00 hour");
        assertTrue(hourlyMetrics.containsKey("11:00") || hourlyMetrics.containsKey("2023-03-01T11:00") ||
            hourlyMetrics.containsKey("2023-03-01T11:00:00"), "Should have metrics for 11:00 hour");
        assertTrue(hourlyMetrics.containsKey("13:00") || hourlyMetrics.containsKey("2023-03-01T13:00") ||
            hourlyMetrics.containsKey("2023-03-01T13:00:00"), "Should have metrics for 13:00 hour");
        assertTrue(hourlyMetrics.containsKey("15:00") || hourlyMetrics.containsKey("2023-03-01T15:00") ||
            hourlyMetrics.containsKey("2023-03-01T15:00:00"), "Should have metrics for 15:00 hour");

        // Total count across all hours should match the overall count
        int totalImpressions = 0;
        for (TimeFilteredMetrics.ComputedMetrics metrics : hourlyMetrics.values()) {
            totalImpressions += metrics.getNumberOfImpressions();
        }
        assertEquals(4, totalImpressions, "Total impressions across hourly buckets should be 4");
    }

    @Test
    void testFilterConsistencyBetweenGranularities() {
        // Apply combined filters
        timeFilteredMetrics.setGenderFilter("Male");
        timeFilteredMetrics.setContextFilter("Shopping");

        // Compute metrics with different granularities
        timeFilteredMetrics.computeForTimeFrame(testStartDate, testEndDate, "Hourly");
        int hourlyImpressions = timeFilteredMetrics.getNumberOfImpressions();

        timeFilteredMetrics.computeForTimeFrame(testStartDate, testEndDate, "Daily");
        int dailyImpressions = timeFilteredMetrics.getNumberOfImpressions();

        timeFilteredMetrics.computeForTimeFrame(testStartDate, testEndDate, "Weekly");
        int weeklyImpressions = timeFilteredMetrics.getNumberOfImpressions();

        // Results should be consistent across granularities
        assertEquals(hourlyImpressions, dailyImpressions, "Impressions count should be consistent between hourly and daily");
        assertEquals(dailyImpressions, weeklyImpressions, "Impressions count should be consistent between daily and weekly");

        // Expected value for Male + Shopping filter
        assertEquals(1, hourlyImpressions, "Should count 1 impression for Male + Shopping");
    }
}