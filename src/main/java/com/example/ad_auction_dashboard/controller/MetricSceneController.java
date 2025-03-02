package com.example.ad_auction_dashboard.controller;

import com.example.ad_auction_dashboard.logic.CampaignMetrics;
import java.io.IOException;
import java.time.LocalDate;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.text.Text;
import javafx.stage.Stage;

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


    private CampaignMetrics metrics; // the campaign data model

    @FXML
    public void initialize() {

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


    }

    // Transition back to the Main Menu (StartScene)
    @FXML
    private void handleMainMenu(ActionEvent event) {
        try {
            // Load the start scene FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/ad_auction_dashboard/fxml/StartScene.fxml"));
            Parent root = loader.load();
            // Optionally clear previous campaign from memory if necessary
            Stage stage = (Stage) impressionsText.getScene().getWindow();
            Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Transition to the Chart Scene
    @FXML
    private void handleChartView(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/ad_auction_dashboard/fxml/ChartScene.fxml"));
            Parent root = loader.load();
            // Retrieve the ChartSceneController
            ChartSceneController chartController = loader.getController();
            // Obtain campaign start/end dates from your metrics object
            // (Assuming metrics has been set and computed in this scene)
            LocalDate campaignStart = metrics.getCampaignStartDate().toLocalDate();
            LocalDate campaignEnd = metrics.getCampaignEndDate().toLocalDate();
            // Pass the campaign date range to the ChartSceneController so the DatePickers are restricted.
            chartController.setCampaignMetrics(metrics);

            Stage stage = (Stage) impressionsText.getScene().getWindow();
            Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
