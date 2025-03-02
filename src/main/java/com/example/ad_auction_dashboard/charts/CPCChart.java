package com.example.ad_auction_dashboard.charts;

import com.example.ad_auction_dashboard.logic.TimeFilteredMetrics;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.VBox;

/**
 * Chart for Cost-Per-Click (CPC) over time
 */
public class CPCChart implements Chart {

    @Override
    public VBox createChart(TimeFilteredMetrics timeFilteredMetrics,
                            LocalDateTime start,
                            LocalDateTime end,
                            String granularity) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Time");
        yAxis.setLabel("CPC");

        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("CPC Over Time");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("CPC");

        // Get time-bucketed data using the granularity parameter
        Map<String, TimeFilteredMetrics.ComputedMetrics> metricsByTime =
            timeFilteredMetrics.computeForTimeFrameWithGranularity(start, end, granularity);

        // Sort the keys to ensure chronological order
        List<String> timeLabels = new ArrayList<>(metricsByTime.keySet());
        Collections.sort(timeLabels);

        // Add data points using the pre-computed metrics
        for (String timeLabel : timeLabels) {
            TimeFilteredMetrics.ComputedMetrics metrics = metricsByTime.get(timeLabel);
            series.getData().add(new XYChart.Data<>(timeLabel, metrics.getCpc()));
        }

        lineChart.getData().add(series);
        return new VBox(lineChart);
    }
}