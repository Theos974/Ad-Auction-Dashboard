package com.example.ad_auction_dashboard.charts;

import com.example.ad_auction_dashboard.logic.CampaignMetrics;
import com.example.ad_auction_dashboard.logic.ClickLog;
import com.example.ad_auction_dashboard.logic.LogDate;
import com.example.ad_auction_dashboard.logic.TimeFilteredMetrics;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ClickCostHistogramGenerator implements HistogramGenerator {

    @Override
    public Map<String, Integer> generateHistogramData(
        CampaignMetrics metrics,
        LocalDateTime start,
        LocalDateTime end,
        int binCount) {

        // Get all click logs
        ClickLog[] clickLogs = metrics.getClickLogs();
        Map<String, Integer> histogramData = new LinkedHashMap<>();

        if (clickLogs == null || clickLogs.length == 0) {
            // Return empty map with placeholder bin if no data
            histogramData.put("No data available", 0);
            return histogramData;
        }

        // Find min and max costs and collect valid costs for better binning
        float minCost = Float.MAX_VALUE;
        float maxCost = Float.MIN_VALUE;
        List<Float> validCosts = new ArrayList<>();

        for (ClickLog log : clickLogs) {
            // Skip if date is outside our range
            LogDate logDate = log.getDate();
            if (logDate == null || !logDate.getExists()) continue;

            LocalDateTime logDateTime = LocalDateTime.of(
                logDate.getYear(), logDate.getMonth(), logDate.getDay(),
                logDate.getHour(), logDate.getMinute(), logDate.getSecond());

            if (logDateTime.isBefore(start) || logDateTime.isAfter(end)) continue;

            float cost = log.getClickCost();
            if (cost < 0) continue; // Skip invalid costs

            validCosts.add(cost);
            if (cost < minCost) minCost = cost;
            if (cost > maxCost) maxCost = cost;
        }

        if (validCosts.isEmpty() || minCost == Float.MAX_VALUE || maxCost == Float.MIN_VALUE) {
            // Return empty map with placeholder bin if no valid data in range
            histogramData.put("No data in selected range", 0);
            return histogramData;
        }

        // Add a small buffer to max for inclusive binning
        maxCost += 0.001f;

        // Create bins with better spacing
        float binWidth = (maxCost - minCost) / binCount;

        // If bin width is too small (happens with uniform data), enforce a minimum
        if (binWidth < 0.01f) {
            binWidth = 0.01f;
            maxCost = minCost + (binWidth * binCount);
        }

        // Sort costs for better bin distribution
        Collections.sort(validCosts);

        // Initialize bins with clear, formatted labels
        for (int i = 0; i < binCount; i++) {
            float lowerBound = minCost + (i * binWidth);
            float upperBound = lowerBound + binWidth;

            // Format bin labels for better readability
            String binLabel;
            if (lowerBound < 0.01f && upperBound < 0.01f) {
                // Use more precise formatting for very small values
                binLabel = String.format("$%.4f-$%.4f", lowerBound, upperBound);
            } else {
                binLabel = String.format("$%.2f-$%.2f", lowerBound, upperBound);
            }
            histogramData.put(binLabel, 0);
        }

        // Count clicks in each bin
        for (Float cost : validCosts) {
            // Find the right bin
            int binIndex = Math.min(binCount - 1, (int)((cost - minCost) / binWidth));
            float lowerBound = minCost + (binIndex * binWidth);
            float upperBound = lowerBound + binWidth;

            // Create bin label in the same format as during initialization
            String binLabel;
            if (lowerBound < 0.01f && upperBound < 0.01f) {
                binLabel = String.format("$%.4f-$%.4f", lowerBound, upperBound);
            } else {
                binLabel = String.format("$%.2f-$%.2f", lowerBound, upperBound);
            }

            // Increment count
            histogramData.put(binLabel, histogramData.getOrDefault(binLabel, 0) + 1);
        }

        return histogramData;
    }
    public Map<String, Integer> generateFilteredHistogramData(
        CampaignMetrics metrics,
        TimeFilteredMetrics timeFilteredMetrics,
        LocalDateTime start,
        LocalDateTime end,
        int binCount) {

        // Get all click logs
        ClickLog[] clickLogs = metrics.getClickLogs();
        Map<String, Integer> histogramData = new LinkedHashMap<>();

        if (clickLogs == null || clickLogs.length == 0) {
            // Return empty map with placeholder bin if no data
            histogramData.put("No data available", 0);
            return histogramData;
        }

        // Find min and max costs and collect valid costs for better binning
        float minCost = Float.MAX_VALUE;
        float maxCost = Float.MIN_VALUE;
        List<Float> validCosts = new ArrayList<>();

        // Get the filtered user IDs from timeFilteredMetrics
        Set<String> filteredUserIds = new HashSet<>();
        for (int i = 0; i < clickLogs.length; i++) {
            ClickLog log = clickLogs[i];
            if (log == null || log.getId() == null) continue;

            LogDate logDate = log.getDate();
            if (logDate == null || !logDate.getExists()) continue;

            LocalDateTime logDateTime = LocalDateTime.of(
                logDate.getYear(), logDate.getMonth(), logDate.getDay(),
                logDate.getHour(), logDate.getMinute(), logDate.getSecond());

            if (logDateTime.isBefore(start) || logDateTime.isAfter(end)) continue;

            filteredUserIds.add(log.getId());
        }

        // Now filter the clicks using the TimeFilteredMetrics filter logic
        for (ClickLog log : clickLogs) {
            // Skip if date is outside our range
            LogDate logDate = log.getDate();
            if (logDate == null || !logDate.getExists()) continue;

            LocalDateTime logDateTime = LocalDateTime.of(
                logDate.getYear(), logDate.getMonth(), logDate.getDay(),
                logDate.getHour(), logDate.getMinute(), logDate.getSecond());

            if (logDateTime.isBefore(start) || logDateTime.isAfter(end)) continue;

            // Check if user passes filters using the public TimeFilteredMetrics method
            String userId = log.getId();
            if (!timeFilteredMetrics.userPassesFilters(userId)) continue;

            float cost = log.getClickCost();
            if (cost < 0) continue; // Skip invalid costs

            validCosts.add(cost);
            if (cost < minCost) minCost = cost;
            if (cost > maxCost) maxCost = cost;
        }

        if (validCosts.isEmpty() || minCost == Float.MAX_VALUE || maxCost == Float.MIN_VALUE) {
            // Return empty map with placeholder bin if no valid data in range
            histogramData.put("No data in selected range or with selected filters", 0);
            return histogramData;
        }

        // Rest of the method remains the same as before
        // Add a small buffer to max for inclusive binning
        maxCost += 0.001f;

        // Create bins with better spacing
        float binWidth = (maxCost - minCost) / binCount;

        // If bin width is too small (happens with uniform data), enforce a minimum
        if (binWidth < 0.01f) {
            binWidth = 0.01f;
            maxCost = minCost + (binWidth * binCount);
        }

        // Sort costs for better bin distribution
        Collections.sort(validCosts);

        // Initialize bins with clear, formatted labels
        for (int i = 0; i < binCount; i++) {
            float lowerBound = minCost + (i * binWidth);
            float upperBound = lowerBound + binWidth;

            // Format bin labels for better readability
            String binLabel;
            if (lowerBound < 0.01f && upperBound < 0.01f) {
                // Use more precise formatting for very small values
                binLabel = String.format("$%.4f-$%.4f", lowerBound, upperBound);
            } else {
                binLabel = String.format("$%.2f-$%.2f", lowerBound, upperBound);
            }
            histogramData.put(binLabel, 0);
        }

        // Count clicks in each bin
        for (Float cost : validCosts) {
            // Find the right bin
            int binIndex = Math.min(binCount - 1, (int)((cost - minCost) / binWidth));
            float lowerBound = minCost + (binIndex * binWidth);
            float upperBound = lowerBound + binWidth;

            // Create bin label in the same format as during initialization
            String binLabel;
            if (lowerBound < 0.01f && upperBound < 0.01f) {
                binLabel = String.format("$%.4f-$%.4f", lowerBound, upperBound);
            } else {
                binLabel = String.format("$%.2f-$%.2f", lowerBound, upperBound);
            }

            // Increment count
            histogramData.put(binLabel, histogramData.getOrDefault(binLabel, 0) + 1);
        }

        return histogramData;
    }

    @Override
    public String getTitle() {
        return "Distribution of Click Costs";
    }

    @Override
    public String getXAxisLabel() {
        return "Click Cost Range";
    }

    @Override
    public String getYAxisLabel() {
        return "Frequency";
    }

    @Override
    public String getDescription() {
        return "This histogram shows the distribution of costs for clicks across your campaign. " +
            "The x-axis shows different cost ranges, while the y-axis shows how many clicks " +
            "fall into each cost range. This visualization helps identify whether your " +
            "click costs are evenly distributed or if there are clusters at certain price points.";
    }
}