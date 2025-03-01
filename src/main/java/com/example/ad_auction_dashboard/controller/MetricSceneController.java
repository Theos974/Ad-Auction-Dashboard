package com.example.ad_auction_dashboard.controller;

import com.example.ad_auction_dashboard.logic.CampaignMetrics;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;

public class MetricSceneController {

    @FXML
    private Label impressionsLabel;
    @FXML
    private Label clicksLabel;
    @FXML
    private Label uniquesLabel;
    @FXML
    private Label bouncesLabel;
    @FXML
    private Label conversionsLabel;
    @FXML
    private Label totalCostLabel;
    @FXML
    private Label ctrLabel;
    @FXML
    private Label cpcLabel;
    @FXML
    private Label cpaLabel;
    @FXML
    private Label cpmLabel;
    @FXML
    private Label bounceRateLabel;

    @FXML
    private LineChart<Number, Number> lineChart;
    @FXML
    private NumberAxis xAxis, yAxis;

    private CampaignMetrics metrics; // model

    @FXML
    public void initialize() {
        // Optional: set up the chart with sample or placeholder data
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Metric Trend");
        series.getData().add(new XYChart.Data<>(0, 0));
        series.getData().add(new XYChart.Data<>(1, 1));
        series.getData().add(new XYChart.Data<>(2, 0.5));
        lineChart.getData().add(series);
    }

    public void setMetrics(CampaignMetrics metrics) {
        this.metrics = metrics;
        updateUI();
    }

    private void updateUI() {
        if (metrics == null) return;
        impressionsLabel.setText(String.valueOf(metrics.getNumberOfImpressions()));
        clicksLabel.setText(String.valueOf(metrics.getNumberOfClicks()));
        uniquesLabel.setText(String.valueOf(metrics.getNumberOfUniques()));
        bouncesLabel.setText(String.valueOf(metrics.getNumberOfBounces()));
        conversionsLabel.setText(String.valueOf(metrics.getNumberOfConversions()));
        totalCostLabel.setText(String.format("%.6f", metrics.getTotalCost()));
        ctrLabel.setText(String.format("%.6f", metrics.getCTR()));
        cpcLabel.setText(String.format("%.6f", metrics.getCPC()));
        cpaLabel.setText(String.format("%.6f", metrics.getCPA()));
        cpmLabel.setText(String.format("%.6f", metrics.getCPM()));
        bounceRateLabel.setText(String.format("%.6f", metrics.getBounceRate()));
    }
}
