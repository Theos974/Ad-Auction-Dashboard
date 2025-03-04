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
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ChartSceneController {

    @FXML
    private ComboBox<String> timeGranularityComboBox;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private Label statusLabel; // Add this to your FXML if not already present
    @FXML
    private ComboBox<String> primaryChartTypeComboBox;

    @FXML
    private ComboBox<String> secondaryChartTypeComboBox;

    @FXML
    private ToggleButton compareToggleButton;

    @FXML
    private VBox primaryChartContainer;

    @FXML
    private VBox secondaryChartContainer;

    @FXML
    private HBox chartsContainer;

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

        // Add chart types to combo boxes
        primaryChartTypeComboBox.getItems().addAll(chartRegistry.keySet());
        primaryChartTypeComboBox.setValue("Impressions"); // Default selection

        secondaryChartTypeComboBox.getItems().addAll(chartRegistry.keySet());
        secondaryChartTypeComboBox.setValue("CPC"); // Default selection

        // Setup time granularity options
        timeGranularityComboBox.getItems().addAll("Hourly", "Daily", "Weekly");
        timeGranularityComboBox.setValue("Daily"); // Default

        // Setup toggle button for comparison
        compareToggleButton.setOnAction(e -> {
            boolean showComparison = compareToggleButton.isSelected();
            secondaryChartContainer.setVisible(showComparison);
            secondaryChartContainer.setManaged(showComparison);
            updateCharts();
        });

        // Setup event listeners
        primaryChartTypeComboBox.setOnAction(e -> updateCharts());
        secondaryChartTypeComboBox.setOnAction(e -> {
            if (compareToggleButton.isSelected()) {
                updateCharts();
            }
        });

        timeGranularityComboBox.setOnAction(e -> {
            currentGranularity = timeGranularityComboBox.getValue();
            updateCharts();

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
            updateCharts();
        });

        endDatePicker.setOnAction(e -> {
            validateDateRange();
            updateCharts();
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

        // Get campaign date boundaries
        LocalDateTime campaignStart = metrics.getCampaignStartDate();
        LocalDateTime campaignEnd = metrics.getCampaignEndDate();

        if (campaignStart != null && campaignEnd != null) {
            LocalDate startDate = campaignStart.toLocalDate();
            LocalDate endDate = campaignEnd.toLocalDate();

            // Set default values to campaign start and end dates
            startDatePicker.setValue(startDate);
            endDatePicker.setValue(endDate);

            // Set date range constraints for both pickers
            startDatePicker.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    setDisable(empty || date.isBefore(startDate) || date.isAfter(endDate));
                }
            });

            endDatePicker.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    setDisable(empty || date.isBefore(startDate) || date.isAfter(endDate));
                }
            });
        }

        // Show initial charts
        updateCharts();
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

    private void updateCharts() {
        if (campaignMetrics == null) return;

        // Get date range
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        if (startDate == null || endDate == null) return;

        // Convert to LocalDateTime
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        try {
            // Update status if processing a lot of data
            if (statusLabel != null && currentGranularity.equals("Hourly") &&
                ChronoUnit.DAYS.between(startDate, endDate) > 14) {
                statusLabel.setText("Processing large amount of hourly data...");
            }

            // Compute metrics for the time frame with granularity
            timeFilteredMetrics.computeForTimeFrame(start, end, currentGranularity);

            // Create and display primary chart
            String primaryChartType = primaryChartTypeComboBox.getValue();
            Chart primaryChartImpl = chartRegistry.get(primaryChartType);
            if (primaryChartImpl != null) {
                VBox primaryChartNode = primaryChartImpl.createChart(
                    timeFilteredMetrics, start, end, currentGranularity);
                primaryChartContainer.getChildren().clear();
                primaryChartContainer.getChildren().add(primaryChartNode);
            }

            // Create and display secondary chart if comparison is enabled
            if (compareToggleButton.isSelected()) {
                String secondaryChartType = secondaryChartTypeComboBox.getValue();
                Chart secondaryChartImpl = chartRegistry.get(secondaryChartType);
                if (secondaryChartImpl != null) {
                    VBox secondaryChartNode = secondaryChartImpl.createChart(
                        timeFilteredMetrics, start, end, currentGranularity);
                    secondaryChartContainer.getChildren().clear();
                    secondaryChartContainer.getChildren().add(secondaryChartNode);
                }
            }

            // Clear status if everything went well (except for hourly warning)
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/ad_auction_dashboard/fxml/MetricScene2.fxml"));
            Parent root = loader.load();

            // Get the controller and pass the campaign metrics
            MetricSceneController controller = loader.getController();
            controller.setMetrics(campaignMetrics);

            // Switch to the metrics scene
            Scene scene = new Scene(root);
            // Use primaryChartContainer instead of chartContainer to get the scene
            Stage stage = (Stage) primaryChartContainer.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error returning to metrics view: " + e.getMessage());
        }
    }
}