package com.example.ad_auction_dashboard.charts;

import com.example.ad_auction_dashboard.logic.TimeFilteredMetrics;
import java.time.LocalDateTime;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.VBox;

/**
 * A chart showing Impressions over time.
 */
public class ImpressionsChart implements Chart {

    @Override
    public VBox createChart(TimeFilteredMetrics timeFilteredMetrics,
                            LocalDateTime start,
                            LocalDateTime end) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Time");
        yAxis.setLabel("Impressions");

        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Impressions Over Time");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Impressions");

        LocalDateTime pointer = start;

        while (!pointer.isAfter(end)) {
            LocalDateTime bucketEnd = pointer.plusDays(1).minusSeconds(1);
            if (bucketEnd.isAfter(end)) {
                bucketEnd = end;
            }

            int impressions = timeFilteredMetrics.filterImpressions(pointer, bucketEnd);
            String label = pointer.toLocalDate().toString();

            series.getData().add(new XYChart.Data<>(label, impressions));
            pointer = bucketEnd.plusSeconds(1);
        }

        lineChart.getData().add(series);
        return new VBox(lineChart);
    }
}
