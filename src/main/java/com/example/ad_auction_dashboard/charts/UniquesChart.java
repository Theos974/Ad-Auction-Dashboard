package com.example.ad_auction_dashboard.charts;

import com.example.ad_auction_dashboard.logic.TimeFilteredMetrics;
import java.time.LocalDateTime;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.VBox;

/**
 * A chart showing the number of Unique Users (Uniques) over time.
 */
public class UniquesChart implements Chart {

    @Override
    public VBox createChart(TimeFilteredMetrics timeFilteredMetrics,
                            LocalDateTime start,
                            LocalDateTime end) {
        // X-axis = time, Y-axis = number of uniques
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Time");
        yAxis.setLabel("Unique Users");

        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Uniques Over Time");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Uniques");

        // Example: daily time buckets
        LocalDateTime pointer = start;
        while (!pointer.isAfter(end)) {
            // define the bucket end (daily)
            LocalDateTime bucketEnd = pointer.plusDays(1).minusSeconds(1);
            if (bucketEnd.isAfter(end)) {
                bucketEnd = end;
            }

            // Filter the logs in [pointer, bucketEnd]
            timeFilteredMetrics.computeForTimeFrame(pointer, bucketEnd);
            int uniquesCount = timeFilteredMetrics.getNumberOfUniques();

            // Create a label (e.g., "yyyy-MM-dd")
            String label = pointer.toLocalDate().toString();

            // Add a data point
            series.getData().add(new XYChart.Data<>(label, uniquesCount));

            // Move pointer to next bucket
            pointer = bucketEnd.plusSeconds(1);
        }

        lineChart.getData().add(series);
        return new VBox(lineChart);
    }
}
