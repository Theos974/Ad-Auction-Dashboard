package com.example.ad_auction_dashboard.IntegrationTests;

import com.example.ad_auction_dashboard.logic.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for enhanced filter capabilities in the TimeFilteredMetrics class
 * Focuses on new age, income, gender, and context filter combinations
 */
public class EnhancedFilterTest {

    private ImpressionLog[] mockImpressionLogs;
    private ClickLog[] mockClickLogs;
    private ServerLog[] mockServerLogs;
    private TimeFilteredMetrics timeFilteredMetrics;

    // Define test date range
    private final LocalDateTime testStartDate = LocalDateTime.of(2023, 3, 1, 8, 0, 0);
    private final LocalDateTime testEndDate = LocalDateTime.of(2023, 3, 1, 18, 0, 0);

    @BeforeEach
    void setUp() {
        // Set up mock impression logs with comprehensive demographic data for testing all filters
        mockImpressionLogs = new ImpressionLog[]{
            // Different combinations of Gender, Age, Income, Context
            // Male demographic
            new ImpressionLog("2023-03-01 10:00:00", "1001", "Male", "<25", "Low", "News", "0.123456"),
            new ImpressionLog("2023-03-01 10:30:00", "1002", "Male", "25-34", "Medium", "Shopping", "0.234567"),
            new ImpressionLog("2023-03-01 11:00:00", "1003", "Male", "35-44", "High", "Blog", "0.345678"),
            new ImpressionLog("2023-03-01 11:30:00", "1004", "Male", "45-54", "Low", "Social Media", "0.456789"),
            new ImpressionLog("2023-03-01 12:00:00", "1005", "Male", ">54", "Medium", "Travel", "0.567890"),

            // Female demographic
            new ImpressionLog("2023-03-01 12:30:00", "1006", "Female", "<25", "High", "News", "0.678901"),
            new ImpressionLog("2023-03-01 13:00:00", "1007", "Female", "25-34", "Low", "Shopping", "0.789012"),
            new ImpressionLog("2023-03-01 13:30:00", "1008", "Female", "35-44", "Medium", "Blog", "0.890123"),
            new ImpressionLog("2023-03-01 14:00:00", "1009", "Female", "45-54", "High", "Social Media", "0.901234"),
            new ImpressionLog("2023-03-01 14:30:00", "1010", "Female", ">54", "Low", "Travel", "0.102345")
        };

        // Create matching click logs for each impression
        mockClickLogs = new ClickLog[]{
            new ClickLog("2023-03-01 10:05:00", "1001", "1.230000"),
            new ClickLog("2023-03-01 10:35:00", "1002", "1.450000"),
            new ClickLog("2023-03-01 11:05:00", "1003", "1.670000"),
            new ClickLog("2023-03-01 11:35:00", "1004", "1.890000"),
            new ClickLog("2023-03-01 12:05:00", "1005", "2.010000"),
            new ClickLog("2023-03-01 12:35:00", "1006", "2.230000"),
            new ClickLog("2023-03-01 13:05:00", "1007", "2.450000"),
            new ClickLog("2023-03-01 13:35:00", "1008", "2.670000"),
            new ClickLog("2023-03-01 14:05:00", "1009", "2.890000"),
            new ClickLog("2023-03-01 14:35:00", "1010", "3.010000")
        };

        // Create server logs with different conversion patterns
        mockServerLogs = new ServerLog[]{
            // First half with conversions
            new ServerLog("2023-03-01 10:05:30", "1001", "2023-03-01 10:10:30", "5", "Yes"),
            new ServerLog("2023-03-01 10:35:30", "1002", "2023-03-01 10:40:30", "3", "Yes"),
            new ServerLog("2023-03-01 11:05:30", "1003", "2023-03-01 11:10:30", "7", "Yes"),
            new ServerLog("2023-03-01 11:35:30", "1004", "2023-03-01 11:40:30", "2", "Yes"),
            new ServerLog("2023-03-01 12:05:30", "1005", "2023-03-01 12:10:30", "4", "Yes"),

            // Second half without conversions
            new ServerLog("2023-03-01 12:35:30", "1006", "2023-03-01 12:36:30", "1", "No"), // Bounce (pages = 1)
            new ServerLog("2023-03-01 13:05:30", "1007", "2023-03-01 13:06:00", "2", "No"), // Bounce (time = 30sec)
            new ServerLog("2023-03-01 13:35:30", "1008", "2023-03-01 13:40:30", "3", "No"),
            new ServerLog("2023-03-01 14:05:30", "1009", "2023-03-01 14:06:00", "1", "No"), // Bounce (both)
            new ServerLog("2023-03-01 14:35:30", "1010", "2023-03-01 14:40:30", "5", "No")
        };

        // Initialize TimeFilteredMetrics with data and bounce criteria
        timeFilteredMetrics = new TimeFilteredMetrics(
            mockImpressionLogs,
            mockServerLogs,
            mockClickLogs,
            1, // bouncePagesThreshold
            60  // bounceSecondsThreshold
        );
    }

    @Test
    @DisplayName("Test comprehensive age group filtering")
    void testAgeGroupFiltering() {
        // Test each age group separately
        String[] ageGroups = {"<25", "25-34", "35-44", "45-54", ">54"};

        for (String ageGroup : ageGroups) {
            // Apply filter for this age group
            timeFilteredMetrics.setAgeFilter(ageGroup);
            timeFilteredMetrics.computeForTimeFrame(testStartDate, testEndDate, "Daily");

            // Each age group should have 2 impressions (1 male, 1 female)
            assertEquals(2, timeFilteredMetrics.getNumberOfImpressions(),
                "Should have 2 impressions for age group " + ageGroup);
            assertEquals(2, timeFilteredMetrics.getNumberOfClicks(),
                "Should have 2 clicks for age group " + ageGroup);
            assertEquals(2, timeFilteredMetrics.getNumberOfUniques(),
                "Should have 2 unique users for age group " + ageGroup);
        }
    }

    @Test
    @DisplayName("Test comprehensive income level filtering")
    void testIncomeLevelFiltering() {
        // Test each income level separately
        String[] incomeLevels = {"Low", "Medium", "High"};
        int[] expectedCounts = {4, 3, 3}; // Expected impression counts for each income level

        for (int i = 0; i < incomeLevels.length; i++) {
            // Apply filter for this income level
            timeFilteredMetrics.setIncomeFilter(incomeLevels[i]);
            timeFilteredMetrics.computeForTimeFrame(testStartDate, testEndDate, "Daily");

            // Verify counts match expected values
            assertEquals(expectedCounts[i], timeFilteredMetrics.getNumberOfImpressions(),
                "Should have " + expectedCounts[i] + " impressions for income level " + incomeLevels[i]);
        }
    }

    @Test
    @DisplayName("Test comprehensive context filtering")
    void testContextFiltering() {
        // Test each context type separately
        String[] contextTypes = {"News", "Shopping", "Blog", "Social Media", "Travel"};
        int[] expectedCounts = {2, 2, 2, 2, 2}; // Expected impression counts for each context

        for (int i = 0; i < contextTypes.length; i++) {
            // Apply filter for this context
            timeFilteredMetrics.setContextFilter(contextTypes[i]);
            timeFilteredMetrics.computeForTimeFrame(testStartDate, testEndDate, "Daily");

            // Verify counts match expected values
            assertEquals(expectedCounts[i], timeFilteredMetrics.getNumberOfImpressions(),
                "Should have " + expectedCounts[i] + " impressions for context " + contextTypes[i]);
        }
    }

    @Test
    @DisplayName("Test complex filter combinations")
    void testComplexFilterCombinations() {
        // Test various combinations of multiple filters

        // Test: Male + <25 + Low income
        timeFilteredMetrics.setGenderFilter("Male");
        timeFilteredMetrics.setAgeFilter("<25");
        timeFilteredMetrics.setIncomeFilter("Low");
        timeFilteredMetrics.computeForTimeFrame(testStartDate, testEndDate, "Daily");

        assertEquals(1, timeFilteredMetrics.getNumberOfImpressions(),
            "Should have 1 impression for Male + <25 + Low income");
        assertEquals(1, timeFilteredMetrics.getNumberOfClicks(),
            "Should have 1 click for Male + <25 + Low income");
        assertEquals(1, timeFilteredMetrics.getNumberOfConversions(),
            "Should have 1 conversion for Male + <25 + Low income");

        // Reset filters
        timeFilteredMetrics.setGenderFilter(null);
        timeFilteredMetrics.setAgeFilter(null);
        timeFilteredMetrics.setIncomeFilter(null);

        // Test: Female + 35-44 + Medium income + Blog context
        timeFilteredMetrics.setGenderFilter("Female");
        timeFilteredMetrics.setAgeFilter("35-44");
        timeFilteredMetrics.setIncomeFilter("Medium");
        timeFilteredMetrics.setContextFilter("Blog");
        timeFilteredMetrics.computeForTimeFrame(testStartDate, testEndDate, "Daily");

        assertEquals(1, timeFilteredMetrics.getNumberOfImpressions(),
            "Should have 1 impression for Female + 35-44 + Medium + Blog");
        assertEquals(1, timeFilteredMetrics.getNumberOfClicks(),
            "Should have 1 click for Female + 35-44 + Medium + Blog");
        assertEquals(0, timeFilteredMetrics.getNumberOfConversions(),
            "Should have 0 conversions for Female + 35-44 + Medium + Blog");
    }

    @Test
    @DisplayName("Test filtering with derived metrics")
    void testFilteringWithDerivedMetrics() {
        // Test how filtering affects derived metrics like CTR, CPC, etc.

        // First get baseline metrics (no filters)
        timeFilteredMetrics.computeForTimeFrame(testStartDate, testEndDate, "Daily");
        double baselineCTR = timeFilteredMetrics.getCTR();
        double baselineCPC = timeFilteredMetrics.getCPC();
        double baselineCPA = timeFilteredMetrics.getCPA();
        double baselineBounceRate = timeFilteredMetrics.getBounceRate();

        // Apply gender filter: Male
        timeFilteredMetrics.setGenderFilter("Male");
        timeFilteredMetrics.computeForTimeFrame(testStartDate, testEndDate, "Daily");

        // All male entries in our test data have conversions, so CTR should be 1.0
        assertEquals(1.0, timeFilteredMetrics.getCTR(), 0.001, "Male CTR should be 1.0");
        assertEquals(0.0, timeFilteredMetrics.getBounceRate(), 0.001, "Male bounce rate should be 0.0");

        // Female entries have no conversions and some bounces
        timeFilteredMetrics.setGenderFilter("Female");
        timeFilteredMetrics.computeForTimeFrame(testStartDate, testEndDate, "Daily");

        assertTrue(timeFilteredMetrics.getBounceRate() > 0, "Female bounce rate should be > 0");

        // Age + Income combined filter
        timeFilteredMetrics.setGenderFilter(null);
        timeFilteredMetrics.setAgeFilter("45-54");
        timeFilteredMetrics.setIncomeFilter("Low");
        timeFilteredMetrics.computeForTimeFrame(testStartDate, testEndDate, "Daily");

        assertEquals(1, timeFilteredMetrics.getNumberOfImpressions(),
            "Should have 1 impression for 45-54 + Low income");
    }

    @Test
    @DisplayName("Test demographic impact using filters")
    void testDemographicImpact() {
        // Compare metrics between different demographic segments

        // Male segment metrics
        timeFilteredMetrics.setGenderFilter("Male");
        timeFilteredMetrics.computeForTimeFrame(testStartDate, testEndDate, "Daily");
        int maleImpressions = timeFilteredMetrics.getNumberOfImpressions();
        int maleClicks = timeFilteredMetrics.getNumberOfClicks();
        int maleConversions = timeFilteredMetrics.getNumberOfConversions();
        double maleCTR = timeFilteredMetrics.getCTR();

        // Female segment metrics
        timeFilteredMetrics.setGenderFilter("Female");
        timeFilteredMetrics.computeForTimeFrame(testStartDate, testEndDate, "Daily");
        int femaleImpressions = timeFilteredMetrics.getNumberOfImpressions();
        int femaleClicks = timeFilteredMetrics.getNumberOfClicks();
        int femaleConversions = timeFilteredMetrics.getNumberOfConversions();
        double femaleCTR = timeFilteredMetrics.getCTR();

        // Verify we can get distinct metrics for different segments
        assertEquals(5, maleImpressions, "Should have 5 male impressions");
        assertEquals(5, femaleImpressions, "Should have 5 female impressions");

        // In our test data, all males have conversions, no females do
        assertEquals(5, maleConversions, "Should have 5 male conversions");
        assertEquals(0, femaleConversions, "Should have 0 female conversions");

        // Young vs. Old comparison
        timeFilteredMetrics.setGenderFilter(null);
        timeFilteredMetrics.setAgeFilter("<25");
        timeFilteredMetrics.computeForTimeFrame(testStartDate, testEndDate, "Daily");
        int youngImpressions = timeFilteredMetrics.getNumberOfImpressions();

        timeFilteredMetrics.setAgeFilter(">54");
        timeFilteredMetrics.computeForTimeFrame(testStartDate, testEndDate, "Daily");
        int oldImpressions = timeFilteredMetrics.getNumberOfImpressions();

        assertEquals(youngImpressions, oldImpressions,
            "Young and old impressions should be equal in our balanced test data");
    }

    @Test
    @DisplayName("Test filter interactions with time granularity")
    void testFilterTimeGranularityInteraction() {
        // Apply demographic filter
        timeFilteredMetrics.setGenderFilter("Male");

        // Compute with hourly granularity
        Map<String, TimeFilteredMetrics.ComputedMetrics> hourlyMetrics =
            timeFilteredMetrics.computeForTimeFrameWithGranularity(testStartDate, testEndDate, "Hourly");

        // Verify we have metrics for specific hours (10:00-12:00) with Male impressions
        assertTrue(hourlyMetrics.containsKey("10:00") || hourlyMetrics.containsKey("2023-03-01T10:00") ||
            hourlyMetrics.containsKey("2023-03-01T10:00:00"), "Should have metrics for 10:00 hour");
        assertTrue(hourlyMetrics.containsKey("11:00") || hourlyMetrics.containsKey("2023-03-01T11:00") ||
            hourlyMetrics.containsKey("2023-03-01T11:00:00"), "Should have metrics for 11:00 hour");

        // Sum metrics across all hours
        int totalImpressions = 0;
        for (TimeFilteredMetrics.ComputedMetrics metrics : hourlyMetrics.values()) {
            totalImpressions += metrics.getNumberOfImpressions();
        }

        // Compare with direct computation
        timeFilteredMetrics.computeForTimeFrame(testStartDate, testEndDate, "Hourly");
        int directImpressions = timeFilteredMetrics.getNumberOfImpressions();

        assertEquals(directImpressions, totalImpressions,
            "Total impressions across hourly buckets should match direct calculation");
    }

    @Test
    @DisplayName("Test filter reset behavior")
    void testFilterResetBehavior() {
        // Apply multiple filters
        timeFilteredMetrics.setGenderFilter("Male");
        timeFilteredMetrics.setAgeFilter("<25");
        timeFilteredMetrics.setIncomeFilter("Low");
        timeFilteredMetrics.setContextFilter("News");

        timeFilteredMetrics.computeForTimeFrame(testStartDate, testEndDate, "Daily");
        assertEquals(1, timeFilteredMetrics.getNumberOfImpressions(),
            "Should have 1 impression with all filters applied");

        // Reset individual filters one by one and verify impact
        timeFilteredMetrics.setContextFilter(null);
        timeFilteredMetrics.computeForTimeFrame(testStartDate, testEndDate, "Daily");
        assertEquals(1, timeFilteredMetrics.getNumberOfImpressions(),
            "Should have 1 impression after resetting context filter");

        timeFilteredMetrics.setIncomeFilter(null);
        timeFilteredMetrics.computeForTimeFrame(testStartDate, testEndDate, "Daily");
        assertEquals(1, timeFilteredMetrics.getNumberOfImpressions(),
            "Should have 1 impression after resetting income filter");

        // After removing age filter, we'll see all Male records (5 in our test data)
        timeFilteredMetrics.setAgeFilter(null);
        timeFilteredMetrics.computeForTimeFrame(testStartDate, testEndDate, "Daily");
        assertEquals(5, timeFilteredMetrics.getNumberOfImpressions(),
            "Should have 5 impressions (all Males) after resetting age filter");

        timeFilteredMetrics.setGenderFilter(null);
        timeFilteredMetrics.computeForTimeFrame(testStartDate, testEndDate, "Daily");
        assertEquals(10, timeFilteredMetrics.getNumberOfImpressions(),
            "Should have all 10 impressions after resetting all filters");
    }
}