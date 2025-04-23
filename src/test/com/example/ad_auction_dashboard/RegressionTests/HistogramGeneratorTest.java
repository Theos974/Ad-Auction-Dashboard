package com.example.ad_auction_dashboard.RegressionTests;

import com.example.ad_auction_dashboard.charts.ClickCostHistogramGenerator;
import com.example.ad_auction_dashboard.charts.HistogramGenerator;
import com.example.ad_auction_dashboard.logic.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HistogramGenerator implementation to validate histogram data generation
 */
public class HistogramGeneratorTest {

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
    void testHistogramTitleAndLabels() {
        // Verify the histogram metadata
        assertNotNull(histogramGenerator.getTitle(), "Histogram should have a title");
        assertNotNull(histogramGenerator.getXAxisLabel(), "Histogram should have an X-axis label");
        assertNotNull(histogramGenerator.getYAxisLabel(), "Histogram should have a Y-axis label");
        assertNotNull(histogramGenerator.getDescription(), "Histogram should have a description");
    }

    @Test
    void testHistogramBinCount() {
        // Test each bin count separately
        testBinCount(5);
        testBinCount(10);
        testBinCount(15);
        testBinCount(20);
    }

    private void testBinCount(int binCount) {
        // Generate histogram with specified bin count
        Map<String, Integer> histogramData = histogramGenerator.generateHistogramData(
            campaignMetrics, startDate, endDate, binCount);

        // Verify histogram data is not null
        assertNotNull(histogramData, "Histogram data should not be null");

        // Verify each bin has a label and count
        for (Map.Entry<String, Integer> entry : histogramData.entrySet()) {
            assertNotNull(entry.getKey(), "Bin label should not be null");
            assertNotNull(entry.getValue(), "Bin count should not be null");
        }

        // Verify total count in all bins matches click count
        int totalCount = histogramData.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(10, totalCount, "Total count in histogram should match click count");
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
    void testFilteredTimeRangeHistogram() {
        // Define a narrower time range (10:00 - 12:00)
        LocalDateTime narrowStart = LocalDateTime.of(2023, 3, 1, 10, 0, 0);
        LocalDateTime narrowEnd = LocalDateTime.of(2023, 3, 1, 12, 0, 0);

        // Generate histogram for this range
        Map<String, Integer> histogramData = histogramGenerator.generateHistogramData(
            campaignMetrics, narrowStart, narrowEnd, 10);

        // Verify histogram data is not null
        assertNotNull(histogramData, "Histogram data should not be null");

        // Verify total count in all bins matches click count in this time range (5 clicks)
        int totalCount = histogramData.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(5, totalCount, "Total count should match filtered click count");
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
    void testHistogramBinLabels() {
        // Generate histogram
        Map<String, Integer> histogramData = histogramGenerator.generateHistogramData(
            campaignMetrics, startDate, endDate, 10);

        // Verify bin labels are properly formatted
        for (String binLabel : histogramData.keySet()) {
            if (!binLabel.equals("No data available") && !binLabel.equals("No data in selected range")) {
                // Bin labels should be in the format "$X.XX-$Y.YY"
                assertTrue(binLabel.matches("\\$\\d+\\.\\d+-\\$\\d+\\.\\d+"),
                    "Bin label should be in the format $X.XX-$Y.YY");
            }
        }
    }

    @Test
    void testHistogramBinWidth() {
        // Generate histogram with 5 bins
        Map<String, Integer> histogramData = histogramGenerator.generateHistogramData(
            campaignMetrics, startDate, endDate, 5);

        // Calculate expected bin width: (0.55 - 0.10) / 5 = 0.09
        double expectedBinWidth = 0.09;

        // Extract bin edges from first bin label
        String firstBinLabel = histogramData.keySet().iterator().next();
        if (!firstBinLabel.equals("No data available") && !firstBinLabel.equals("No data in selected range")) {
            String[] parts = firstBinLabel.split("-");
            double lowerBound = Double.parseDouble(parts[0].substring(1)); // Remove $ and parse
            double upperBound = Double.parseDouble(parts[1].substring(1)); // Remove $ and parse

            // Verify bin width is close to expected width (allowing for some floating point error)
            assertEquals(expectedBinWidth, upperBound - lowerBound, 0.01,
                "Bin width should match expected width");
        }
    }
    @Test
    void testFilteredHistogramDataGeneration() {
        // Create filtered metrics
        TimeFilteredMetrics filteredMetrics = new TimeFilteredMetrics(
            campaignMetrics.getImpressionLogs(),
            campaignMetrics.getServerLogs(),
            campaignMetrics.getClickLogs(),
            campaignMetrics.getBouncePagesThreshold(),
            campaignMetrics.getBounceSecondsThreshold()
        );

        // Apply gender filter
        filteredMetrics.setGenderFilter("Male");

        // Generate filtered histogram data
        Map<String, Integer> filteredData = histogramGenerator.generateFilteredHistogramData(
            campaignMetrics, filteredMetrics, startDate, endDate, 10);

        // Verify filtered data is not null
        assertNotNull(filteredData, "Filtered histogram data should not be null");

        // Verify filtered data has fewer counts than unfiltered data
        Map<String, Integer> unfilteredData = histogramGenerator.generateHistogramData(
            campaignMetrics, startDate, endDate, 10);

        int filteredTotal = filteredData.values().stream().mapToInt(Integer::intValue).sum();
        int unfilteredTotal = unfilteredData.values().stream().mapToInt(Integer::intValue).sum();

        // Since we filtered, we expect fewer data points
        assertTrue(filteredTotal <= unfilteredTotal,
            "Filtered data should have fewer or equal points than unfiltered data");
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

    @Test
    void testUniformValueData() {
        // Create campaign with uniform click costs
        ClickLog[] uniformClicks = new ClickLog[]{
            new ClickLog("2023-03-01 10:00:00", "1001", "0.500000"),
            new ClickLog("2023-03-01 10:30:00", "1002", "0.500000"),
            new ClickLog("2023-03-01 11:00:00", "1003", "0.500000"),
            new ClickLog("2023-03-01 11:30:00", "1004", "0.500000"),
            new ClickLog("2023-03-01 12:00:00", "1005", "0.500000")
        };

        Campaign uniformCampaign = new Campaign(new ImpressionLog[0], uniformClicks, new ServerLog[0]);
        CampaignMetrics uniformMetrics = new CampaignMetrics(uniformCampaign);

        // Generate histogram with uniform data
        Map<String, Integer> histogramData = histogramGenerator.generateHistogramData(
            uniformMetrics, startDate, endDate, 10);

        // Verify histogram handles uniform data gracefully
        assertNotNull(histogramData, "Histogram data should not be null for uniform data");

        // Either we should have a single bin with all values, or the algorithm should
        // adjust bin width to create multiple bins
        int nonZeroBins = (int) histogramData.values().stream().filter(count -> count > 0).count();
        assertTrue(nonZeroBins >= 1, "Should have at least one bin with data");

        int totalCount = histogramData.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(5, totalCount, "Total count should match uniform click count");
    }

}
