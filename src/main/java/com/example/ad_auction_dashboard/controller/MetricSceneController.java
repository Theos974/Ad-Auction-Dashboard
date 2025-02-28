package com.example.ad_auction_dashboard.controller;

import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import com.example.ad_auction_dashboard.logic.CampaignMetrics;

public class MetricSceneController {

    @FXML
    private LineChart<Number, Number> lineChart;

    @FXML
    private NumberAxis xAxis, yAxis;

    private CampaignMetrics metrics; // reference to your model if needed

    @FXML
    public void initialize() {
        // Called automatically after FXML loads
        // You can populate the chart with dummy or real data
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Example Metric");
        // Hardcoded sample points
        series.getData().add(new XYChart.Data<>(0, 0.1));
        series.getData().add(new XYChart.Data<>(1, 0.3));
        series.getData().add(new XYChart.Data<>(2, 0.7));
        lineChart.getData().add(series);
    }

    public void setMetrics(CampaignMetrics metrics) {
        this.metrics = metrics;
        // Optionally call a method to update the chart from the metrics
        updateChart();
    }

    private void updateChart() {
        // Clear old data
        lineChart.getData().clear();
        // Build new XYChart.Series from metrics, e.g. CTR over time
        // lineChart.getData().add(...);
    }
}
