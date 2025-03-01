package com.example.ad_auction_dashboard.controller;

import com.example.ad_auction_dashboard.logic.CampaignMetrics;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.text.Text;

public class MetricSceneController {

    // These should match the fx:id's of the Text nodes in your FXML for displaying metric values.
    @FXML
    private Text impressionsText;
    @FXML
    private Text clicksText;
    @FXML
    private Text uniquesText;
    @FXML
    private Text bouncesText;
    @FXML
    private Text conversionsText;
    @FXML
    private Text totalCostText;
    @FXML
    private Text ctrText;
    @FXML
    private Text cpcText;
    @FXML
    private Text cpaText;
    @FXML
    private Text cpmText;
    @FXML
    private Text bounceRateText;

    // Change from LineChart to BarChart with a CategoryAxis for metric names.
    @FXML
    private BarChart<String, Number> barChart;
    @FXML
    private CategoryAxis xAxis;
    @FXML
    private NumberAxis yAxis;

    private CampaignMetrics metrics; // the campaign data model

    @FXML
    public void initialize() {
        // Set up the bar chart axes
        xAxis.setLabel("Metric");
        yAxis.setLabel("Value");
    }

    public void setMetrics(CampaignMetrics metrics) {
        this.metrics = metrics;
        updateUI();
    }

    private void updateUI() {
        if (metrics == null) return;

        // Update the Text nodes with metric values
        impressionsText.setText(String.valueOf(metrics.getNumberOfImpressions()));
        clicksText.setText(String.valueOf(metrics.getNumberOfClicks()));
        uniquesText.setText(String.valueOf(metrics.getNumberOfUniques()));
        bouncesText.setText(String.valueOf(metrics.getNumberOfBounces()));
        conversionsText.setText(String.valueOf(metrics.getNumberOfConversions()));
        totalCostText.setText(String.format("%.6f", metrics.getTotalCost()));
        ctrText.setText(String.format("%.6f", metrics.getCTR()));
        cpcText.setText(String.format("%.6f", metrics.getCPC()));
        cpaText.setText(String.format("%.6f", metrics.getCPA()));
        cpmText.setText(String.format("%.6f", metrics.getCPM()));
        bounceRateText.setText(String.format("%.6f", metrics.getBounceRate()));

        // Build a bar chart series with all metrics
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Campaign Metrics");
        series.getData().add(new XYChart.Data<>("Impressions", metrics.getNumberOfImpressions()));
        series.getData().add(new XYChart.Data<>("Clicks", metrics.getNumberOfClicks()));
        series.getData().add(new XYChart.Data<>("Uniques", metrics.getNumberOfUniques()));
        series.getData().add(new XYChart.Data<>("Bounces", metrics.getNumberOfBounces()));
        series.getData().add(new XYChart.Data<>("Conversions", metrics.getNumberOfConversions()));
        series.getData().add(new XYChart.Data<>("Total Cost", metrics.getTotalCost()));
        series.getData().add(new XYChart.Data<>("CTR", metrics.getCTR()));
        series.getData().add(new XYChart.Data<>("CPA", metrics.getCPA()));
        series.getData().add(new XYChart.Data<>("CPC", metrics.getCPC()));
        series.getData().add(new XYChart.Data<>("CPM", metrics.getCPM()));
        series.getData().add(new XYChart.Data<>("Bounce Rate", metrics.getBounceRate()));

        // Clear any previous data and add the new series to the chart
        barChart.getData().clear();
        barChart.getData().add(series);
    }
}
