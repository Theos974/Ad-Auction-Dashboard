package com.example.ad_auction_dashboard.BoundaryAndPartitionTests;

import static org.junit.jupiter.api.Assertions.*;
import com.example.ad_auction_dashboard.logic.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

public class TimeRangeEdgeCaseTest {

    private ImpressionLog[] mockImpressionLogs;
    private ClickLog[] mockClickLogs;
    private ServerLog[] mockServerLogs;
    private TimeFilteredMetrics timeFilteredMetrics;

    // Define test date range
    private final LocalDateTime testStartDate = LocalDateTime.of(2023, 3, 1, 8, 0, 0);
    private final LocalDateTime testEndDate = LocalDateTime.of(2023, 3, 1, 18, 0, 0);

    @BeforeEach
    void setUp() {
        // Set up mock impression logs spread over different hours
        mockImpressionLogs = new ImpressionLog[]{
            new ImpressionLog("2023-03-01 10:00:00", "1001", "Male", "<25", "Medium", "News", "0.123456"),
            new ImpressionLog("2023-03-01 10:30:00", "1002", "Female", "25-34", "High", "Shopping", "0.234567"),
            new ImpressionLog("2023-03-01 11:00:00", "1003", "Male", "35-44", "Low", "Blog", "0.345678"),
            new ImpressionLog("2023-03-01 11:30:00", "1004", "Female", "45-54", "Medium", "Social Media", "0.456789"),
            new ImpressionLog("2023-03-01 12:00:00", "1005", "Male", ">54", "High", "News", "0.567890"),
            new ImpressionLog("2023-03-01 13:00:00", "1006", "Female", "<25", "Low", "Blog", "0.678901"),
            new ImpressionLog("2023-03-01 14:00:00", "1007", "Male", "25-34", "Medium", "Shopping", "0.789012")
        };

        // Set up mock click logs
        mockClickLogs = new ClickLog[]{
            new ClickLog("2023-03-01 10:05:00", "1001", "1.230000"),
            new ClickLog("2023-03-01 10:35:00", "1002", "1.450000"),
            new ClickLog("2023-03-01 11:10:00", "1003", "1.670000"),
            new ClickLog("2023-03-01 13:10:00", "1006", "1.890000"),
            new ClickLog("2023-03-01 14:10:00", "1007", "2.010000")
        };

        // Set up mock server logs
        mockServerLogs = new ServerLog[]{
            new ServerLog("2023-03-01 10:05:30", "1001", "2023-03-01 10:06:30", "1", "No"),  // Bounce (1 page)
            new ServerLog("2023-03-01 10:35:30", "1002", "2023-03-01 10:42:30", "5", "Yes"), // Conversion
            new ServerLog("2023-03-01 11:10:30", "1003", "2023-03-01 11:11:00", "2", "No"),  // Bounce (short time)
            new ServerLog("2023-03-01 13:10:30", "1006", "2023-03-01 13:20:30", "3", "Yes"), // Conversion
            new ServerLog("2023-03-01 14:10:30", "1007", "2023-03-01 14:15:30", "1", "No")   // Bounce (1 page)
        };

        // Initialize TimeFilteredMetrics with default bounce criteria
        timeFilteredMetrics = new TimeFilteredMetrics(
            mockImpressionLogs,
            mockServerLogs,
            mockClickLogs,
            1, // bouncePagesThreshold
            60  // bounceSecondsThreshold - increasing this to 60 seconds to catch all expected bounces
        );
    }

    @Test
    void testEmptyTimeRange() {
        // Define a time range outside of our data
        LocalDateTime futureStart = LocalDateTime.of(2023, 4, 1, 0, 0, 0);
        LocalDateTime futureEnd = LocalDateTime.of(2023, 4, 2, 0, 0, 0);

        // Compute metrics for this range
        timeFilteredMetrics.computeForTimeFrame(futureStart, futureEnd, "Daily");

        // Verify all metrics are zero
        assertEquals(0, timeFilteredMetrics.getNumberOfImpressions(), "Should count 0 impressions for empty range");
        assertEquals(0, timeFilteredMetrics.getNumberOfClicks(), "Should count 0 clicks for empty range");
        assertEquals(0, timeFilteredMetrics.getNumberOfUniques(), "Should count 0 uniques for empty range");
        assertEquals(0, timeFilteredMetrics.getNumberOfBounces(), "Should count 0 bounces for empty range");
        assertEquals(0, timeFilteredMetrics.getNumberOfConversions(), "Should count 0 conversions for empty range");
        assertEquals(0, timeFilteredMetrics.getTotalCost(), "Should have 0 cost for empty range");
        assertEquals(0, timeFilteredMetrics.getCTR(), "Should have 0 CTR for empty range");
        assertEquals(0, timeFilteredMetrics.getCPC(), "Should have 0 CPC for empty range");
        assertEquals(0, timeFilteredMetrics.getCPM(), "Should have 0 CPM for empty range");
        assertEquals(0, timeFilteredMetrics.getBounceRate(), "Should have 0 bounce rate for empty range");
    }

    @Test
    void testOneHourTimeRange() {
        // Define a very narrow time range (exactly one hour)
        LocalDateTime hourStart = LocalDateTime.of(2023, 3, 1, 11, 0, 0);
        LocalDateTime hourEnd = LocalDateTime.of(2023, 3, 1, 11, 59, 59);

        // Compute metrics for this narrow range
        timeFilteredMetrics.computeForTimeFrame(hourStart, hourEnd, "Hourly");

        // Verify metrics for this specific hour
        // During 11:00-11:59, we should have 2 impressions, 1 click, 1 bounce
        assertEquals(2, timeFilteredMetrics.getNumberOfImpressions(), "Should have 2 impressions in hour 11");
        assertEquals(1, timeFilteredMetrics.getNumberOfClicks(), "Should have 1 click in hour 11");
        assertEquals(1, timeFilteredMetrics.getNumberOfBounces(), "Should have 1 bounce in hour 11");
        assertEquals(0, timeFilteredMetrics.getNumberOfConversions(), "Should have 0 conversions in hour 11");
    }

}