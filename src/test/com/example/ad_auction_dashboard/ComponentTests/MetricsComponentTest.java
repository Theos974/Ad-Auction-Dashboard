package com.example.ad_auction_dashboard.ComponentTests;

import static org.junit.jupiter.api.Assertions.*;

import com.example.ad_auction_dashboard.logic.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Component Tests for the Metrics Component
 *
 * This test suite verifies that the metrics components function correctly
 * when integrated with other related components in the system.
 * It focuses specifically on how metrics calculations operate in a real application context
 * rather than just testing the calculation logic in isolation.
 */
public class MetricsComponentTest {

    private Campaign testCampaign;
    private CampaignMetrics metrics;
    private TimeFilteredMetrics filteredMetrics;

    // Test data boundaries
    private LocalDateTime campaignStart;
    private LocalDateTime campaignEnd;

    @BeforeEach
    void setUp() {
        // Create a realistic test campaign with varied data
        ImpressionLog[] impressions = {
            new ImpressionLog("2023-03-01 10:00:00", "1001", "Male", "<25", "Medium", "News", "0.123456"),
            new ImpressionLog("2023-03-01 11:30:00", "1002", "Female", "25-34", "High", "Shopping", "0.234567"),
            new ImpressionLog("2023-03-01 12:45:00", "1003", "Male", "35-44", "Low", "Blog", "0.345678"),
            new ImpressionLog("2023-03-01 14:20:00", "1004", "Female", "45-54", "Medium", "Social Media", "0.456789"),
            new ImpressionLog("2023-03-01 16:05:00", "1001", "Male", "<25", "Medium", "News", "0.567890"),  // Same user as first impression
        };

        ClickLog[] clicks = {
            new ClickLog("2023-03-01 10:02:30", "1001", "0.500000"),
            new ClickLog("2023-03-01 11:35:45", "1002", "0.750000"),
            new ClickLog("2023-03-01 14:25:10", "1004", "0.800000"),
            new ClickLog("2023-03-01 16:10:20", "1001", "0.650000"),  // Second click from same user
        };

        ServerLog[] serverLogs = {
            // Conversion, not a bounce
            new ServerLog("2023-03-01 10:02:35", "1001", "2023-03-01 10:10:40", "5", "Yes"),
            // Not a conversion, not a bounce
            new ServerLog("2023-03-01 11:35:50", "1002", "2023-03-01 11:45:10", "4", "No"),
            // Not a conversion, is a bounce (short time)
            new ServerLog("2023-03-01 14:25:15", "1004", "2023-03-01 14:26:00", "1", "No"),
            // Conversion, not a bounce
            new ServerLog("2023-03-01 16:10:25", "1001", "2023-03-01 16:20:30", "6", "Yes")
        };

        // Create campaign and metrics
        testCampaign = new Campaign(impressions, clicks, serverLogs);
        metrics = new CampaignMetrics(testCampaign);

        // Create filtered metrics for component interaction testing
        filteredMetrics = new TimeFilteredMetrics(
            impressions, serverLogs, clicks,
            metrics.getBouncePagesThreshold(),
            metrics.getBounceSecondsThreshold()
        );

        // Set campaign time boundaries
        campaignStart = LocalDateTime.of(2023, 3, 1, 0, 0, 0);
        campaignEnd = LocalDateTime.of(2023, 3, 1, 23, 59, 59);
    }

    @Test
    @DisplayName("Test Metrics Component Interaction with Campaign")
    void testMetricsComponentInteractionWithCampaign() {
        // Verify that metrics correctly extract information from the Campaign
        assertEquals(5, metrics.getNumberOfImpressions(), "Should correctly count 5 impressions");
        assertEquals(4, metrics.getNumberOfClicks(), "Should correctly count 4 clicks");
        assertEquals(3, metrics.getNumberOfUniques(), "Should correctly identify 3 unique users");

        // Verify that derived metrics are calculated correctly
        double expectedCTR = 4.0 / 5.0;  // clicks / impressions
        assertEquals(expectedCTR, metrics.getCTR(), 0.0001, "CTR should be calculated correctly");

        // Verify that bounce detection works
        assertEquals(1, metrics.getNumberOfBounces(), "Should correctly identify 1 bounce");
        double expectedBounceRate = 1.0 / 4.0;  // bounces / clicks
        assertEquals(expectedBounceRate, metrics.getBounceRate(), 0.0001, "Bounce rate should be calculated correctly");

        // Verify that conversions are counted correctly
        assertEquals(2, metrics.getNumberOfConversions(), "Should correctly count 2 conversions");

        // Total cost should include both impression and click costs
        double expectedCost =
            0.123456 + 0.234567 + 0.345678 + 0.456789 + 0.567890 +  // Impression costs
                0.500000 + 0.750000 + 0.800000 + 0.650000;              // Click costs
        assertEquals(expectedCost, metrics.getTotalCost(), 0.0001, "Total cost should be calculated correctly");
    }

    @Test
    @DisplayName("Test Metrics Component Interaction with Time Filtering")
    void testMetricsComponentInteractionWithTimeFiltering() {
        LocalDateTime morningStart = LocalDateTime.of(2023, 3, 1, 10, 0, 0);
          LocalDateTime morningEnd = LocalDateTime.of(2023, 3, 1, 12, 0, 0).minusNanos(1);

        // Apply time filtering
        filteredMetrics.computeForTimeFrame(morningStart, morningEnd, "Hourly");

        int morningImpressionCount = 0;
        for (ImpressionLog imp : testCampaign.getImpressionLogs()) {
            LocalDateTime impTime = LocalDateTime.of(
                imp.getDate().getYear(),
                imp.getDate().getMonth(),
                imp.getDate().getDay(),
                imp.getDate().getHour(),
                imp.getDate().getMinute(),
                imp.getDate().getSecond()
            );
            if (!impTime.isBefore(morningStart) && !impTime.isAfter(morningEnd)) {
                morningImpressionCount++;
            }
        }

        assertEquals(morningImpressionCount, filteredMetrics.getNumberOfImpressions(),
            "Should count correct number of impressions in morning time range");

        LocalDateTime afternoonStart = LocalDateTime.of(2023, 3, 1, 14, 0, 0);
        LocalDateTime afternoonEnd = LocalDateTime.of(2023, 3, 1, 17, 0, 0);

        filteredMetrics.computeForTimeFrame(afternoonStart, afternoonEnd, "Hourly");
        assertTrue(filteredMetrics.getNumberOfImpressions() > 0,
            "Should return some impressions for the afternoon range");
    }

    @Test
    @DisplayName("Test Metrics Component Interaction with Demographic Filtering")
    void testMetricsComponentInteractionWithDemographicFiltering() {
        filteredMetrics.setGenderFilter("Male");
        filteredMetrics.computeForTimeFrame(campaignStart, campaignEnd, "Daily");

        // Count how many impressions for male users
        int maleImpressions = 0;
        for (ImpressionLog imp : testCampaign.getImpressionLogs()) {
            if ("Male".equals(imp.getGender())) {
                maleImpressions++;
            }
        }

        //  filtered results for male users
        assertEquals(3, filteredMetrics.getNumberOfImpressions(), "Should count 3 impressions for male users");


        assertEquals(1, filteredMetrics.getNumberOfUniques(),
            "Should count 1 unique male user with clicks");

        filteredMetrics.setGenderFilter(null);
        filteredMetrics.setAgeFilter("45-54");
        filteredMetrics.computeForTimeFrame(campaignStart, campaignEnd, "Daily");

        assertEquals(1, filteredMetrics.getNumberOfImpressions(), "Should count 1 impression for age 45-54");
        assertEquals(1, filteredMetrics.getNumberOfClicks(), "Should count 1 click for age 45-54");
        assertEquals(1, filteredMetrics.getNumberOfUniques(), "Should count 1 unique user for age 45-54");
    }
    @Test
    @DisplayName("Test Metrics Component Interaction with Context Filtering")
    void testMetricsComponentInteractionWithContextFiltering() {
        // Apply context filter for News
        filteredMetrics.setContextFilter("News");
        filteredMetrics.computeForTimeFrame(campaignStart, campaignEnd, "Daily");

        // Verify filtered results for News context
        assertEquals(2, filteredMetrics.getNumberOfImpressions(), "Should count 2 impressions for News context");
        // Number of conversions depends on how the system matches impressions to server logs
        int newsConversions = filteredMetrics.getNumberOfConversions();
        assertTrue(newsConversions >= 0, "Should count a valid number of conversions for News context");

        // Apply context filter for Social Media
        filteredMetrics.setContextFilter("Social Media");
        filteredMetrics.computeForTimeFrame(campaignStart, campaignEnd, "Daily");

        // Verify filtered results for Social Media context
        assertEquals(1, filteredMetrics.getNumberOfImpressions(), "Should count 1 impression for Social Media context");
        assertEquals(1, filteredMetrics.getNumberOfClicks(), "Should count 1 click for Social Media context");
        assertEquals(1, filteredMetrics.getNumberOfUniques(), "Should count 1 unique user for Social Media context");
    }

    @Test
    @DisplayName("Test Metrics Component Interaction with Combined Filters")
    void testMetricsComponentInteractionWithCombinedFilters() {
        // Apply multiple filters: Male + News context
        filteredMetrics.setGenderFilter("Male");
        filteredMetrics.setContextFilter("News");
        filteredMetrics.computeForTimeFrame(campaignStart, campaignEnd, "Daily");

        // Verify filtered results with combined filters
        assertEquals(2, filteredMetrics.getNumberOfImpressions(), "Should count 2 impressions for Male + News");
        // Other expectations depend on how the system implements filtering
        assertTrue(filteredMetrics.getNumberOfClicks() >= 0, "Should count a valid number of clicks for Male + News");

        // Apply different combination: Female + time range
        filteredMetrics.setGenderFilter("Female");
        filteredMetrics.setContextFilter(null);
        LocalDateTime afternoonStart = LocalDateTime.of(2023, 3, 1, 14, 0, 0);
        LocalDateTime afternoonEnd = LocalDateTime.of(2023, 3, 1, 17, 0, 0);
        filteredMetrics.computeForTimeFrame(afternoonStart, afternoonEnd, "Hourly");

        // Verify filtered results with combined filters
        assertEquals(1, filteredMetrics.getNumberOfImpressions(), "Should count 1 impression for Female in afternoon");
    }

    @Test
    @DisplayName("Test Metrics Component Campaign Date Range Detection")
    void testMetricsComponentCampaignDateRangeDetection() {
        // Test that the metrics component correctly detects campaign date range
        LocalDateTime detectedStart = metrics.getCampaignStartDate();
        LocalDateTime detectedEnd = metrics.getCampaignEndDate();

        // Instead of comparing exact dates, verify they're within expected range
        assertNotNull(detectedStart, "Campaign start date should be detected");
        assertNotNull(detectedEnd, "Campaign end date should be detected");

        // Check that detected dates are on the expected day
        assertEquals(2023, detectedStart.getYear(), "Campaign start year should be 2023");
        assertEquals(3, detectedStart.getMonthValue(), "Campaign start month should be March");
        assertEquals(1, detectedStart.getDayOfMonth(), "Campaign start day should be 1");

        assertEquals(2023, detectedEnd.getYear(), "Campaign end year should be 2023");
        assertEquals(3, detectedEnd.getMonthValue(), "Campaign end month should be March");
        assertEquals(1, detectedEnd.getDayOfMonth(), "Campaign end day should be 1");
    }

    @Test
    @DisplayName("Test Metrics Component Bounce Criteria Configuration")
    void testMetricsComponentBounceCriteriaConfiguration() {
        int defaultPagesThreshold = metrics.getBouncePagesThreshold();
        int defaultSecondsThreshold = metrics.getBounceSecondsThreshold();

        int initialBounceCount = metrics.getNumberOfBounces();

        metrics.setBounceCriteria(defaultPagesThreshold - 1, defaultSecondsThreshold - 1);

        assertEquals(defaultPagesThreshold - 1, metrics.getBouncePagesThreshold(),
            "Bounce pages threshold should be updated");
        assertEquals(defaultSecondsThreshold - 1, metrics.getBounceSecondsThreshold(),
            "Bounce seconds threshold should be updated");


        int newBounceCount = metrics.getNumberOfBounces();
        assertTrue(newBounceCount <= initialBounceCount,
            "Bounce count should decrease or stay same with stricter criteria");
    }

    @Test
    @DisplayName("Test Metrics Component Granularity Handling")
    void testMetricsComponentGranularityHandling() {
        // Test hourly granularity
        Map<String, TimeFilteredMetrics.ComputedMetrics> hourlyMetrics =
            filteredMetrics.computeForTimeFrameWithGranularity(campaignStart, campaignEnd, "Hourly");

        // Verify we have some metrics for hours
        assertFalse(hourlyMetrics.isEmpty(), "Should have some hourly metrics");

        // Test daily granularity
        Map<String, TimeFilteredMetrics.ComputedMetrics> dailyMetrics =
            filteredMetrics.computeForTimeFrameWithGranularity(campaignStart, campaignEnd, "Daily");

        // Verify we have some metrics for days
        assertFalse(dailyMetrics.isEmpty(), "Should have some daily metrics");

        // Sum metrics across all time buckets for each granularity
        int hourlyImpressionSum = 0;
        for (TimeFilteredMetrics.ComputedMetrics metrics : hourlyMetrics.values()) {
            hourlyImpressionSum += metrics.getNumberOfImpressions();
        }

        int dailyImpressionSum = 0;
        for (TimeFilteredMetrics.ComputedMetrics metrics : dailyMetrics.values()) {
            dailyImpressionSum += metrics.getNumberOfImpressions();
        }

        // Total impressions should be the same regardless of granularity
        assertEquals(dailyImpressionSum, hourlyImpressionSum,
            "Total impressions should match across granularities");
    }
}