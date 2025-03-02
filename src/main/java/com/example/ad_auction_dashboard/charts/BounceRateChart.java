package com.example.ad_auction_dashboard.charts;

import com.example.ad_auction_dashboard.logic.TimeFilteredMetrics;
import java.time.LocalDateTime;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.VBox;

/**
 * Chart for Bounce Rate over time
 */
public class BounceRateChart implements Chart {

    @Override
    public VBox createChart(TimeFilteredMetrics timeFilteredMetrics, LocalDateTime start, LocalDateTime end) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Time");
        yAxis.setLabel("Bounce Rate");

        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Bounce Rate Over Time");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Bounce Rate");

        LocalDateTime pointer = start;
        while (!pointer.isAfter(end)) {
            LocalDateTime bucketEnd = pointer.plusDays(1).minusSeconds(1);
            if (bucketEnd.isAfter(end)) {
                bucketEnd = end;
            }

            timeFilteredMetrics.computeForTimeFrame(pointer, bucketEnd);
            double brValue = timeFilteredMetrics.getBounceRate();
            String label = pointer.toLocalDate().toString();

            series.getData().add(new XYChart.Data<>(label, brValue));
            pointer = bucketEnd.plusSeconds(1);
        }

        lineChart.getData().add(series);
        return new VBox(lineChart);
    }
}
