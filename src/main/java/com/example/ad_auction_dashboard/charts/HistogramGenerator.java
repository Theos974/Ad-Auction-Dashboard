package com.example.ad_auction_dashboard.charts;

import com.example.ad_auction_dashboard.logic.CampaignMetrics;
import com.example.ad_auction_dashboard.logic.TimeFilteredMetrics;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Interface for generating histogram data for different metrics
 */
public interface HistogramGenerator {
    /**
     * Calculate histogram data points for the selected date range and bin count
     *
     * @param metrics The campaign metrics object containing all log data
     * @param start The start datetime for filtering data
     * @param end The end datetime for filtering data
     * @param binCount The number of bins to divide the data into
     * @return A map of bin labels to frequencies/counts
     */
    default Map<String, Integer> generateFilteredHistogramData(
        CampaignMetrics metrics,
        TimeFilteredMetrics timeFilteredMetrics,
        LocalDateTime start,
        LocalDateTime end,
        int binCount) {

        // Default implementation falls back to the original method
        return generateHistogramData(metrics, start, end, binCount);
    }
    Map<String, Integer> generateHistogramData(
        CampaignMetrics metrics,
        LocalDateTime start,
        LocalDateTime end,
        int binCount);

    /**
     * Get the title for this histogram
     */
    String getTitle();

    /**
     * Get the x-axis label for this histogram
     */
    String getXAxisLabel();

    /**
     * Get the y-axis label for this histogram (typically "Frequency" or "Count")
     */
    String getYAxisLabel();

    /**
     * Get a description of what this histogram shows
     */
    String getDescription();
}