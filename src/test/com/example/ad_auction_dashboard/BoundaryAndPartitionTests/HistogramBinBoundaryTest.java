package com.example.ad_auction_dashboard.BoundaryAndPartitionTests;

import static org.junit.jupiter.api.Assertions.*;
import com.example.ad_auction_dashboard.logic.*;
import com.example.ad_auction_dashboard.charts.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Tests for boundary conditions in histogram bin generation
 */
public class HistogramBinBoundaryTest {

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
}