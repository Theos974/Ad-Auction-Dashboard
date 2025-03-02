package com.example.ad_auction_dashboard.charts;

import com.example.ad_auction_dashboard.logic.TimeFilteredMetrics;
import java.time.LocalDateTime;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.VBox;

/**
 * A chart showing CTR (click-through rate) over time.
 */
public class CTRChart implements Chart {

    @Override
    public VBox createChart(TimeFilteredMetrics timeFilteredMetrics,
                            LocalDateTime start,
                            LocalDateTime end) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Time");
        yAxis.setLabel("CTR");

        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("CTR Over Time");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("CTR");

        LocalDateTime pointer = start;

        while (!pointer.isAfter(end)) {
            LocalDateTime bucketEnd = pointer.plusDays(1).minusSeconds(1);
            if (bucketEnd.isAfter(end)) {
                bucketEnd = end;
            }

            // Let the TimeFilteredMetrics do the heavy-lifting:
            timeFilteredMetrics.computeForTimeFrame(pointer, bucketEnd);
            double ctrValue = timeFilteredMetrics.getCTR();

            String label = pointer.toLocalDate().toString();
            series.getData().add(new XYChart.Data<>(label, ctrValue));

            pointer = bucketEnd.plusSeconds(1);
        }

        lineChart.getData().add(series);
        return new VBox(lineChart);
    }
}
