package com.example.ad_auction_dashboard.controller;

import com.example.ad_auction_dashboard.charts.Chart;
import com.example.ad_auction_dashboard.charts.CPAChart;
import com.example.ad_auction_dashboard.charts.CPCChart;
import com.example.ad_auction_dashboard.charts.ConversionsChart;
import com.example.ad_auction_dashboard.charts.ClicksChart;
import com.example.ad_auction_dashboard.charts.ImpressionsChart;
import com.example.ad_auction_dashboard.charts.CTRChart;
import com.example.ad_auction_dashboard.charts.CPMChart;
import com.example.ad_auction_dashboard.charts.BounceRateChart;
import com.example.ad_auction_dashboard.logic.CampaignMetrics;
import com.example.ad_auction_dashboard.logic.TimeFilteredMetrics;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

public class ChartPaneController {
    @FXML
    private ComboBox<String> timeGranularityComboBox;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private Pane chartContainer;

    private CampaignMetrics campaignMetrics;
    private TimeFilteredMetrics timeFilteredMetrics;
    private String currentChartType;
    private Map<String, Chart> charts = new HashMap<>();

    // Current granularity selection
    private String currentGranularity = "Daily"; // Default

    public void initialize() {
        // Setup time granularity combo box
        timeGranularityComboBox.getItems().addAll("Hourly", "Daily", "Weekly");
        timeGranularityComboBox.setValue("Daily"); // Default

        // Initialize chart types - add all chart implementations you have
        charts.put("conversions", new ConversionsChart());
        charts.put("cpa", new CPAChart());
        charts.put("cpc", new CPCChart());
        charts.put("clicks", new ClicksChart());
        charts.put("impressions", new ImpressionsChart());
        charts.put("ctr", new CTRChart());
        charts.put("cpm", new CPMChart());
        charts.put("bounce-rate", new BounceRateChart());
        // Add other chart types as needed
    }

    public void setCampaignMetrics(CampaignMetrics campaignMetrics) {
        this.campaignMetrics = campaignMetrics;

        // Initialize TimeFilteredMetrics with data from campaignMetrics
        this.timeFilteredMetrics = new TimeFilteredMetrics(
            campaignMetrics.getImpressionLogs(),
            campaignMetrics.getServerLogs(),
            campaignMetrics.getClickLogs(),
            campaignMetrics.getBouncePagesThreshold(),
            campaignMetrics.getBounceSecondsThreshold()
        );

        // Set default date range using CampaignMetrics
        LocalDateTime campaignStart = campaignMetrics.getCampaignStartDate();
        LocalDateTime campaignEnd = campaignMetrics.getCampaignEndDate();

        if (campaignStart != null && campaignEnd != null) {
            startDatePicker.setValue(campaignStart.toLocalDate());
            endDatePicker.setValue(campaignEnd.toLocalDate());
        }

        // Setup initial chart (if current chart type is set)
        if (currentChartType != null) {
            updateChart();
        }
    }

    public void setChartType(String chartType) {
        if (charts.containsKey(chartType)) {
            this.currentChartType = chartType;
            updateChart();
        } else {
            System.err.println("Unknown chart type: " + chartType);
        }
    }

    @FXML
    public void handleUpdateTime(ActionEvent event) {
        // Update time granularity based on selection
        currentGranularity = timeGranularityComboBox.getValue();

        // Update chart with new granularity
        updateChart();
    }

    @FXML
    public void handleBackToMetrics(ActionEvent event) {
        try {
            // Load the metrics scene
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/ad_auction_dashboard/fxml/MetricScene.fxml"));
            Parent root = loader.load();

            // Get the controller and pass the campaignMetrics
            MetricSceneController controller = loader.getController();
            if (campaignMetrics != null) {
                controller.setMetrics(campaignMetrics);
            }

            // Switch to the metrics scene
            Scene scene = new Scene(root);
            Stage stage = (Stage) chartContainer.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateChart() {
        if (campaignMetrics == null || currentChartType == null || !charts.containsKey(currentChartType)) {
            return;
        }

        // Get date range from pickers
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        if (startDate == null || endDate == null) {
            return;
        }

        // Convert to LocalDateTime (start at beginning of day, end at end of day)
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        // Clear previous chart
        chartContainer.getChildren().clear();

        // Compute metrics with granularity
        timeFilteredMetrics.computeForTimeFrame(start, end, currentGranularity);

        // Get the chart implementation
        Chart chart = charts.get(currentChartType);

        try {
            // Try to create chart with granularity parameter if supported
            VBox chartNode = createChartWithGranularity(chart, start, end, currentGranularity);
            chartContainer.getChildren().add(chartNode);
        } catch (Exception e) {
            // Fall back to standard method if granularity parameter isn't supported
            System.err.println("Chart doesn't support granularity. Using standard method.");
        }
    }

    // Helper method to try creating a chart with granularity parameter
    private VBox createChartWithGranularity(Chart chart, LocalDateTime start, LocalDateTime end, String granularity) {
        try {
            // Use reflection to check if the chart has a method that accepts granularity
            Method method = chart.getClass().getMethod(
                "createChart",
                TimeFilteredMetrics.class,
                LocalDateTime.class,
                LocalDateTime.class,
                String.class
            );

            // Invoke the method with granularity
            return (VBox) method.invoke(chart, timeFilteredMetrics, start, end, granularity);
        } catch (Exception e) {
            // Method not found or invocation failed, fallback to standard method
            System.err.println("Chart doesn't support granularity. Using standard method.");
            return null;

        }
    }
}