package com.example.ad_auction_dashboard.charts;

import com.example.ad_auction_dashboard.logic.TimeFilteredMetrics;
import java.time.LocalDateTime;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.VBox;

public class BounceChart implements Chart {
    @Override
    public VBox createChart(TimeFilteredMetrics timeFilteredMetrics, LocalDateTime start, LocalDateTime end) {
        LineChart<String, Number> chart = new LineChart<>(new CategoryAxis(), new NumberAxis());
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Bounce Rate");

        LocalDateTime pointer = start;
        while (!pointer.isAfter(end)) {
            Number bounceRate = timeFilteredMetrics.filterBounces(pointer, pointer.plusDays(1));
            series.getData().add(new XYChart.Data<>(pointer.toString(), bounceRate));
            pointer = pointer.plusDays(1);
        }

        chart.getData().add(series);
        return new VBox(chart);
    }
}
