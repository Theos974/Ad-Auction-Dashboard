package com.example.ad_auction_dashboard.charts;

import com.example.ad_auction_dashboard.logic.TimeFilteredMetrics;
import java.time.LocalDateTime;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.VBox;

/**
 * A chart showing the number of Conversions over time.
 */
public class ConversionsChart implements Chart {

    @Override
    public VBox createChart(TimeFilteredMetrics timeFilteredMetrics,
                            LocalDateTime start,
                            LocalDateTime end) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Time");
        yAxis.setLabel("Conversions");

        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Conversions Over Time");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Conversions");

        LocalDateTime pointer = start;
        while (!pointer.isAfter(end)) {
            LocalDateTime bucketEnd = pointer.plusDays(1).minusSeconds(1);
            if (bucketEnd.isAfter(end)) {
                bucketEnd = end;
            }

            // Evaluate the conversions for the time bucket
            timeFilteredMetrics.computeForTimeFrame(pointer, bucketEnd);
            int convCount = timeFilteredMetrics.getNumberOfConversions();

            String label = pointer.toLocalDate().toString();
            series.getData().add(new XYChart.Data<>(label, convCount));

            pointer = bucketEnd.plusSeconds(1);
        }

        lineChart.getData().add(series);
        return new VBox(lineChart);
    }
}
