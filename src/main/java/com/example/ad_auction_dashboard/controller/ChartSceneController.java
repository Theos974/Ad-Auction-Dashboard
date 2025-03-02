package com.example.ad_auction_dashboard.controller;

import com.example.ad_auction_dashboard.charts.Chart;
import com.example.ad_auction_dashboard.charts.BounceRateChart;
import com.example.ad_auction_dashboard.charts.CPAChart;
import com.example.ad_auction_dashboard.charts.CPCChart;
import com.example.ad_auction_dashboard.charts.CTRChart;
import com.example.ad_auction_dashboard.charts.CPMChart;
import com.example.ad_auction_dashboard.charts.ConversionsChart;
import com.example.ad_auction_dashboard.charts.ClicksChart;
import com.example.ad_auction_dashboard.charts.ImpressionsChart;
import com.example.ad_auction_dashboard.charts.TotalCostChart;
import com.example.ad_auction_dashboard.charts.UniquesChart;
import com.example.ad_auction_dashboard.charts.BounceChart;
import com.example.ad_auction_dashboard.logic.CampaignMetrics;
import com.example.ad_auction_dashboard.logic.TimeFilteredMetrics;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ChartSceneController {
    @FXML
    private ComboBox<String> chartTypeComboBox;

    @FXML
    private ComboBox<String> timeGranularityComboBox;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private Pane chartContainer;

    @FXML
    private Label statusLabel; // Add this to your FXML if not already present

    private CampaignMetrics campaignMetrics;
    private TimeFilteredMetrics timeFilteredMetrics;
    private final Map<String, Chart> chartRegistry = new LinkedHashMap<>(); // LinkedHashMap to maintain insertion order

    // Current granularity selection
    private String currentGranularity = "Daily"; // Default

    @FXML
    public void initialize() {
        // Register all available chart types - all 11 metrics
        chartRegistry.put("Impressions", new ImpressionsChart());
        chartRegistry.put("Clicks", new ClicksChart());
        chartRegistry.put("Unique Users", new UniquesChart());
        chartRegistry.put("Bounces", new BounceChart());
        chartRegistry.put("Conversions", new ConversionsChart());
        chartRegistry.put("Total Cost", new TotalCostChart());
        chartRegistry.put("CTR", new CTRChart());
        chartRegistry.put("CPC", new CPCChart());
        chartRegistry.put("CPA", new CPAChart());
        chartRegistry.put("CPM", new CPMChart());
        chartRegistry.put("Bounce Rate", new BounceRateChart());

        // Add chart types to combo box
        chartTypeComboBox.getItems().addAll(chartRegistry.keySet());
        chartTypeComboBox.setValue("Impressions"); // Default selection

        // Setup time granularity options
        timeGranularityComboBox.getItems().addAll("Hourly", "Daily", "Weekly");
        timeGranularityComboBox.setValue("Daily"); // Default

        // Setup event listeners
        chartTypeComboBox.setOnAction(e -> updateChart());
        timeGranularityComboBox.setOnAction(e -> {
            currentGranularity = timeGranularityComboBox.getValue();
            updateChart();

            // Provide feedback about hourly view for long time periods
            if (currentGranularity.equals("Hourly")) {
                long days = ChronoUnit.DAYS.between(startDatePicker.getValue(), endDatePicker.getValue()) + 1;
                if (days > 7) {
                    statusLabel.setText("Note: Hourly view with " + days +
                        " days. X-axis labels may be compressed for readability.");
                }
            } else {
                statusLabel.setText("");
            }
        });

        // Date picker event listeners
        startDatePicker.setOnAction(e -> {
            validateDateRange();
            updateChart();
        });

        endDatePicker.setOnAction(e -> {
            validateDateRange();
            updateChart();
        });

        // Initialize status label if present
        if (statusLabel != null) {
            statusLabel.setText("");
        }
    }

    public void setCampaignMetrics(CampaignMetrics metrics) {
        this.campaignMetrics = metrics;

        // Create TimeFilteredMetrics instance
        this.timeFilteredMetrics = new TimeFilteredMetrics(
            metrics.getImpressionLogs(),
            metrics.getServerLogs(),
            metrics.getClickLogs(),
            metrics.getBouncePagesThreshold(),
            metrics.getBounceSecondsThreshold()
        );

        // Set date pickers to campaign date range
        LocalDateTime campaignStart = metrics.getCampaignStartDate();
        LocalDateTime campaignEnd = metrics.getCampaignEndDate();

        if (campaignStart != null && campaignEnd != null) {
            startDatePicker.setValue(campaignStart.toLocalDate());
            endDatePicker.setValue(campaignEnd.toLocalDate());
        }

        // Show initial chart
        updateChart();
    }

    /**
     * Validates and corrects the date range if needed
     */
    private void validateDateRange() {
        if (startDatePicker.getValue() != null && endDatePicker.getValue() != null) {
            if (endDatePicker.getValue().isBefore(startDatePicker.getValue())) {
                // End date is before start date, correct it
                endDatePicker.setValue(startDatePicker.getValue());
                showAlert("Invalid date range detected. End date has been adjusted to match start date.");
            }
        }
    }

    /**
     * Shows an alert to the user
     */
    private void showAlert(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        }
    }

    private void updateChart() {
        if (campaignMetrics == null) return;

        String selectedChartType = chartTypeComboBox.getValue();
        if (selectedChartType == null) return;

        Chart chartImpl = chartRegistry.get(selectedChartType);
        if (chartImpl == null) return;

        // Get date range
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        if (startDate == null || endDate == null) return;

        // Convert to LocalDateTime
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        try {
            // Update status if we're processing a lot of data
            if (statusLabel != null && currentGranularity.equals("Hourly") &&
                ChronoUnit.DAYS.between(startDate, endDate) > 14) {
                statusLabel.setText("Processing large amount of hourly data...");
            }

            // Compute metrics for the time frame with granularity
            timeFilteredMetrics.computeForTimeFrame(start, end, currentGranularity);

            // Create chart and display it
            VBox chartNode = chartImpl.createChart(timeFilteredMetrics, start, end, currentGranularity);

            // Update display
            chartContainer.getChildren().clear();
            chartContainer.getChildren().add(chartNode);

            // Clear status if everything went well and we don't have any warnings
            if (statusLabel != null && !currentGranularity.equals("Hourly")) {
                statusLabel.setText("");
            }
        } catch (Exception e) {
            // Show error to user
            showAlert("Error creating chart: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBackToMetrics(ActionEvent event) {
        try {
            // Load the metrics scene
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/ad_auction_dashboard/fxml/MetricScene.fxml"));
            Parent root = loader.load();

            // Get the controller and pass the campaign metrics
            MetricSceneController controller = loader.getController();
            controller.setMetrics(campaignMetrics);

            // Switch to the metrics scene
            Scene scene = new Scene(root);
            Stage stage = (Stage) chartContainer.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error returning to metrics view: " + e.getMessage());
        }
    }
}