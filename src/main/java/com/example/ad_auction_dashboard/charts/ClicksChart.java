package com.example.ad_auction_dashboard.charts;

import com.example.ad_auction_dashboard.logic.TimeFilteredMetrics;
import java.time.LocalDateTime;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.VBox;

/**
 * A chart showing the number of Clicks over time.
 */
public class ClicksChart implements Chart {

    @Override
    public VBox createChart(TimeFilteredMetrics timeFilteredMetrics,
                            LocalDateTime start,
                            LocalDateTime end) {
        // Create the axes and chart
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Time");
        yAxis.setLabel("Number of Clicks");

        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Clicks Over Time");

        // Prepare a single series
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Clicks");

        // We’ll walk from 'start' to 'end' in daily or hourly buckets, up to you.
        // For demonstration, let's do daily increments:
        LocalDateTime pointer = start.toLocalDate().atStartOfDay();

        while (!pointer.isAfter(end)) {
            // next day boundary
            LocalDateTime bucketEnd = pointer.plusDays(1).minusSeconds(1);
            if (bucketEnd.isAfter(end)) {
                bucketEnd = end;
            }

            // Use TimeFilteredMetrics to get the # of clicks in [pointer, bucketEnd].
            int clicks = timeFilteredMetrics.filterClicks(pointer, bucketEnd);

            // Format the label (e.g. "YYYY-MM-DD" or "MM/dd") or just pointer.toString()
            String label = String.format("%d-%02d-%02d",
                pointer.getYear(), pointer.getMonthValue(), pointer.getDayOfMonth());

            // Add data point to the series
            series.getData().add(new XYChart.Data<>(label, clicks));

            // move pointer to the next bucket
            pointer = bucketEnd.plusSeconds(1);
        }

        // Add the series to the chart
        lineChart.getData().add(series);

        // Return as a simple VBox containing the lineChart
        return new VBox(lineChart);
    }
}
