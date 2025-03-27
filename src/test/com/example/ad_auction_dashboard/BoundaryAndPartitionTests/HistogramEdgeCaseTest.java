package com.example.ad_auction_dashboard.BoundaryAndPartitionTests;

import static org.junit.jupiter.api.Assertions.*;
import com.example.ad_auction_dashboard.logic.*;
import com.example.ad_auction_dashboard.charts.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Map;

public class HistogramEdgeCaseTest {

    private ClickCostHistogramGenerator histogramGenerator;
    private CampaignMetrics campaignMetrics;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @BeforeEach
    void setUp() {
        // Initialize the histogram generator
        histogramGenerator = new ClickCostHistogramGenerator();

        // Create test data with varied click costs
        ClickLog[] clickLogs = new ClickLog[]{
            new ClickLog("2023-03-01 10:00:00", "1001", "0.100000"),
            new ClickLog("2023-03-01 10:30:00", "1002", "0.150000"),
            new ClickLog("2023-03-01 11:00:00", "1003", "0.200000"),
            new ClickLog("2023-03-01 11:30:00", "1004", "0.250000"),
            new ClickLog("2023-03-01 12:00:00", "1005", "0.300000"),
            new ClickLog("2023-03-01 12:30:00", "1006", "0.350000"),
            new ClickLog("2023-03-01 13:00:00", "1007", "0.400000"),
            new ClickLog("2023-03-01 13:30:00", "1008", "0.450000"),
            new ClickLog("2023-03-01 14:00:00", "1009", "0.500000"),
            new ClickLog("2023-03-01 14:30:00", "1010", "0.550000")
        };

        // Create empty impression and server logs (not needed for click cost histogram)
        ImpressionLog[] impressionLogs = new ImpressionLog[0];
        ServerLog[] serverLogs = new ServerLog[0];

        // Create campaign and metrics
        Campaign campaign = new Campaign(impressionLogs, clickLogs, serverLogs);
        campaignMetrics = new CampaignMetrics(campaign);

        // Set date range covering all data
        startDate = LocalDateTime.of(2023, 3, 1, 0, 0, 0);
        endDate = LocalDateTime.of(2023, 3, 1, 23, 59, 59);
    }
    @Test
    void testEmptyDataHistogram() {
        // Create campaign with no click logs
        Campaign emptyCampaign = new Campaign(new ImpressionLog[0], new ClickLog[0], new ServerLog[0]);
        CampaignMetrics emptyMetrics = new CampaignMetrics(emptyCampaign);

        // Generate histogram
        Map<String, Integer> histogramData = histogramGenerator.generateHistogramData(
            emptyMetrics, startDate, endDate, 10);

        // Verify histogram data has a placeholder for empty data
        assertNotNull(histogramData, "Histogram data should not be null for empty data");
        assertTrue(histogramData.containsKey("No data available"),
            "Histogram should have a placeholder for empty data");
    }

    @Test
    void testOutOfRangeHistogram() {
        // Define a time range outside of our data
        LocalDateTime futureStart = LocalDateTime.of(2023, 4, 1, 0, 0, 0);
        LocalDateTime futureEnd = LocalDateTime.of(2023, 4, 2, 0, 0, 0);

        // Generate histogram for this range
        Map<String, Integer> histogramData = histogramGenerator.generateHistogramData(
            campaignMetrics, futureStart, futureEnd, 10);

        // Verify histogram data has a placeholder for no data in the selected range
        assertNotNull(histogramData, "Histogram data should not be null for out-of-range data");
        assertTrue(histogramData.containsKey("No data in selected range"),
            "Histogram should have a placeholder for out-of-range data");
    }

    @Test
    void testExtremelySmallBinCount() {
        // Test with minimum bin count
        Map<String, Integer> histogramData = histogramGenerator.generateHistogramData(
            campaignMetrics, startDate, endDate, 2);

        // Verify histogram data is generated correctly
        assertNotNull(histogramData, "Histogram data should not be null with small bin count");
        assertTrue(histogramData.size() >= 2, "Should have at least 2 bins");

        int totalCount = histogramData.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(10, totalCount, "Total count should match click count");
    }

    @Test
    void testExtremelyLargeBinCount() {
        // Test with large bin count (more bins than data points)
        Map<String, Integer> histogramData = histogramGenerator.generateHistogramData(
            campaignMetrics, startDate, endDate, 50);

        // Verify histogram data handles large bin count gracefully
        assertNotNull(histogramData, "Histogram data should not be null with large bin count");

        int totalCount = histogramData.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(10, totalCount, "Total count should match click count");
    }

}