package com.example.ad_auction_dashboard.logic;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.time.temporal.ChronoUnit;
import java.text.DecimalFormat;

/**
 * A view for comparing full campaign metrics side by side
 */
public class FullCampaignComparisonView {

    /**
     * Show a dialog with a full comparison of two campaigns
     *
     * @param owner The owner window
     * @param currentMetrics Metrics from the current campaign
     * @param comparisonMetrics Metrics from the comparison campaign
     * @param currentName Name of the current campaign
     * @param comparisonName Name of the comparison campaign
     */
    public static void show(Stage owner,
                            CampaignMetrics currentMetrics,
                            CampaignMetrics comparisonMetrics,
                            String currentName,
                            String comparisonName) {

        // Create stage
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle("Campaign Comparison: " + currentName + " vs " + comparisonName);
        dialog.setMinWidth(800);
        dialog.setMinHeight(600);

        // Create layout
        BorderPane layout = new BorderPane();
        layout.getStyleClass().add("metrics-container");
        layout.setStyle("-fx-background-color: #2d2d3b;");

        // Header
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(20, 10, 10, 10));

        Text headerText = new Text("Full Campaign Comparison");
        headerText.setFont(Font.font("System", FontWeight.BOLD, 24));
        headerText.setStyle("-fx-fill: white;");

        header.getChildren().add(headerText);
        layout.setTop(header);

        // Main content grid
        GridPane grid = new GridPane();
        grid.setHgap(30);
        grid.setVgap(15);
        grid.setPadding(new Insets(20, 30, 20, 30));
        grid.setAlignment(Pos.CENTER);

        // Column headers
        Text metricsLabel = new Text("Metrics");
        metricsLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        metricsLabel.setStyle("-fx-fill: white;");

        Text currentLabel = new Text(currentName);
        currentLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        currentLabel.setStyle("-fx-fill: #a365f5;");

        Text comparisonLabel = new Text(comparisonName);
        comparisonLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        comparisonLabel.setStyle("-fx-fill: #65a3f5;");

        Text differenceLabel = new Text("Difference");
        differenceLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        differenceLabel.setStyle("-fx-fill: #ffcc66;");

        grid.add(metricsLabel, 0, 0);
        grid.add(currentLabel, 1, 0);
        grid.add(comparisonLabel, 2, 0);
        grid.add(differenceLabel, 3, 0);

        // Add separator
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #666;");
        grid.add(sep, 0, 1, 4, 1);

        // Add metrics rows
        addMetricRow(grid, "Impressions",
            currentMetrics.getNumberOfImpressions(),
            comparisonMetrics.getNumberOfImpressions(), 2, false);

        addMetricRow(grid, "Clicks",
            currentMetrics.getNumberOfClicks(),
            comparisonMetrics.getNumberOfClicks(), 3, false);

        addMetricRow(grid, "Unique Users",
            currentMetrics.getNumberOfUniques(),
            comparisonMetrics.getNumberOfUniques(), 4, false);

        addMetricRow(grid, "Bounces",
            currentMetrics.getNumberOfBounces(),
            comparisonMetrics.getNumberOfBounces(), 5, false);

        addMetricRow(grid, "Conversions",
            currentMetrics.getNumberOfConversions(),
            comparisonMetrics.getNumberOfConversions(), 6, false);

        addMetricRow(grid, "Total Cost",
            currentMetrics.getTotalCost(),
            comparisonMetrics.getTotalCost(), 7, true);

        addMetricRow(grid, "CTR",
            currentMetrics.getCTR(),
            comparisonMetrics.getCTR(), 8, true);

        addMetricRow(grid, "CPC",
            currentMetrics.getCPC(),
            comparisonMetrics.getCPC(), 9, true);

        addMetricRow(grid, "CPA",
            currentMetrics.getCPA(),
            comparisonMetrics.getCPA(), 10, true);

        addMetricRow(grid, "CPM",
            currentMetrics.getCPM(),
            comparisonMetrics.getCPM(), 11, true);

        addMetricRow(grid, "Bounce Rate",
            currentMetrics.getBounceRate(),
            comparisonMetrics.getBounceRate(), 12, true);

        // Add campaign date ranges
        addDateRangeInfo(grid, "Date Range",
            currentMetrics.getCampaignStartDate(), currentMetrics.getCampaignEndDate(),
            comparisonMetrics.getCampaignStartDate(), comparisonMetrics.getCampaignEndDate(),
            13);

        // Add Bounce settings comparison
        addBounceSettingsInfo(grid, "Bounce Settings",
            currentMetrics.getBouncePagesThreshold(), currentMetrics.getBounceSecondsThreshold(),
            comparisonMetrics.getBouncePagesThreshold(), comparisonMetrics.getBounceSecondsThreshold(),
            14);

        // Wrap grid in ScrollPane to handle overflow
        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        layout.setCenter(scrollPane);

        // Bottom buttons
        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(20, 10, 20, 10));

        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> dialog.close());
        closeButton.setPrefWidth(120);
        closeButton.setPrefHeight(40);
        closeButton.setStyle("-fx-background-color: #363642; -fx-text-fill: white; -fx-background-radius: 5;");

        buttonBox.getChildren().add(closeButton);
        layout.setBottom(buttonBox);

        // Create scene and show
        Scene scene = new Scene(layout, 900, 700);

        dialog.setScene(scene);
        dialog.showAndWait();
    }

    /**
     * Add a metric row to the comparison grid
     */
    private static void addMetricRow(GridPane grid, String metricName, double currentValue,
                                     double comparisonValue, int rowIndex, boolean formatAsDecimal) {
        // Metric name label
        Text nameLabel = new Text(metricName);
        nameLabel.setStyle("-fx-fill: white;");
        nameLabel.setFont(Font.font("System", FontWeight.NORMAL, 16));

        // Format values appropriately
        String currentText, comparisonText;
        if (formatAsDecimal) {
            DecimalFormat df = new DecimalFormat("#,##0.000000");
            currentText = df.format(currentValue);
            comparisonText = df.format(comparisonValue);
        } else {
            // For integers like impressions, clicks, etc.
            DecimalFormat df = new DecimalFormat("#,###");
            currentText = df.format(currentValue);
            comparisonText = df.format(comparisonValue);
        }

        // Value labels
        Text currentLabel = new Text(currentText);
        currentLabel.setStyle("-fx-fill: #a365f5;");
        currentLabel.setFont(Font.font("System", FontWeight.NORMAL, 16));

        Text comparisonLabel = new Text(comparisonText);
        comparisonLabel.setStyle("-fx-fill: #65a3f5;");
        comparisonLabel.setFont(Font.font("System", FontWeight.NORMAL, 16));

        // Calculate difference
        double diff = comparisonValue - currentValue;
        double percentDiff = 0;
        if (currentValue != 0) {
            percentDiff = (diff / Math.abs(currentValue)) * 100;
        }

        // Format difference with percentage
        String diffText;
        if (currentValue == 0 && comparisonValue == 0) {
            diffText = "No difference";
        } else if (currentValue == 0) {
            diffText = "∞";
        } else {
            String sign = diff >= 0 ? "+" : "";

            if (formatAsDecimal) {
                diffText = String.format("%s%.6f (%.2f%%)", sign, diff, percentDiff);
            } else {
                diffText = String.format("%s%,.0f (%.2f%%)", sign, diff, percentDiff);
            }
        }

        Text diffLabel = new Text(diffText);
        // Color based on whether higher is better for this metric
        String diffColor;
        if (metricName.equals("CPC") || metricName.equals("CPA") ||
            metricName.equals("CPM") || metricName.equals("Bounce Rate") ||
            metricName.equals("Total Cost") || metricName.equals("Bounces")) {
            // For these metrics, lower is better
            diffColor = diff < 0 ? "#90ee90" : "#ff7f7f"; // Green if lower, red if higher
        } else {
            // For these metrics, higher is better
            diffColor = diff > 0 ? "#90ee90" : "#ff7f7f"; // Green if higher, red if lower
        }
        diffLabel.setStyle("-fx-fill: " + diffColor + ";");
        diffLabel.setFont(Font.font("System", FontWeight.NORMAL, 16));

        // Add to grid
        grid.add(nameLabel, 0, rowIndex);
        grid.add(currentLabel, 1, rowIndex);
        grid.add(comparisonLabel, 2, rowIndex);
        grid.add(diffLabel, 3, rowIndex);
    }

    /**
     * Add a row with date range information
     */
    private static void addDateRangeInfo(GridPane grid, String labelText,
                                         java.time.LocalDateTime currentStart, java.time.LocalDateTime currentEnd,
                                         java.time.LocalDateTime comparisonStart, java.time.LocalDateTime comparisonEnd,
                                         int rowIndex) {
        // Add separator above this row
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #666;");
        grid.add(sep, 0, rowIndex, 4, 1);
        rowIndex++;

        // Label
        Text nameLabel = new Text(labelText);
        nameLabel.setStyle("-fx-fill: white;");
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

        // Format date ranges
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");

        String currentDateRange = formatDateRange(currentStart, currentEnd, formatter);
        String comparisonDateRange = formatDateRange(comparisonStart, comparisonEnd, formatter);

        Text currentLabel = new Text(currentDateRange);
        currentLabel.setStyle("-fx-fill: #a365f5;");
        currentLabel.setFont(Font.font("System", FontWeight.NORMAL, 16));
        currentLabel.setWrappingWidth(180); // Add width constraint for wrapping

        Text comparisonLabel = new Text(comparisonDateRange);
        comparisonLabel.setStyle("-fx-fill: #65a3f5;");
        comparisonLabel.setFont(Font.font("System", FontWeight.NORMAL, 16));
        comparisonLabel.setWrappingWidth(180); // Add width constraint for wrapping

        // Calculate difference in days
        long daysDiff = 0;
        String diffText = "N/A";

        if (currentStart != null && currentEnd != null &&
            comparisonStart != null && comparisonEnd != null) {

            long currentDays = ChronoUnit.DAYS.between(currentStart, currentEnd) + 1;
            long comparisonDays = ChronoUnit.DAYS.between(comparisonStart, comparisonEnd) + 1;
            daysDiff = comparisonDays - currentDays;

            String sign = daysDiff >= 0 ? "+" : "";
            diffText = String.format("%s%d days", sign, daysDiff);
        }

        Text diffLabel = new Text(diffText);
        diffLabel.setStyle("-fx-fill: #ffcc66;");
        diffLabel.setFont(Font.font("System", FontWeight.NORMAL, 16));

        // Add to grid
        grid.add(nameLabel, 0, rowIndex);
        grid.add(currentLabel, 1, rowIndex);
        grid.add(comparisonLabel, 2, rowIndex);
        grid.add(diffLabel, 3, rowIndex);
    }

    /**
     * Format a date range as a string
     */
    private static String formatDateRange(java.time.LocalDateTime start, java.time.LocalDateTime end,
                                          java.time.format.DateTimeFormatter formatter) {
        if (start == null || end == null) {
            return "Unknown date range";
        }

        return start.format(formatter) + " to " + end.format(formatter);
    }

    /**
     * Add row with bounce settings information
     */
    private static void addBounceSettingsInfo(GridPane grid, String labelText,
                                              int currentPages, int currentSeconds,
                                              int comparisonPages, int comparisonSeconds,
                                              int rowIndex) {
        // Label
        Text nameLabel = new Text(labelText);
        nameLabel.setStyle("-fx-fill: white;");
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

        // Format settings
        String currentSettings = String.format("Pages ≤ %d or Time ≤ %ds",
            currentPages, currentSeconds);
        String comparisonSettings = String.format("Pages ≤ %d or Time ≤ %ds",
            comparisonPages, comparisonSeconds);

        // Create settings labels
        Text currentLabel = new Text(currentSettings);
        currentLabel.setStyle("-fx-fill: #a365f5;");
        currentLabel.setFont(Font.font("System", FontWeight.NORMAL, 16));

        Text comparisonLabel = new Text(comparisonSettings);
        comparisonLabel.setStyle("-fx-fill: #65a3f5;");
        comparisonLabel.setFont(Font.font("System", FontWeight.NORMAL, 16));

        // Describe difference
        String diffText = "Same settings";
        if (currentPages != comparisonPages || currentSeconds != comparisonSeconds) {
            diffText = "Different settings";
        }

        Text diffLabel = new Text(diffText);
        diffLabel.setStyle("-fx-fill: #ffcc66;");
        diffLabel.setFont(Font.font("System", FontWeight.NORMAL, 16));

        // Add to grid
        grid.add(nameLabel, 0, rowIndex);
        grid.add(currentLabel, 1, rowIndex);
        grid.add(comparisonLabel, 2, rowIndex);
        grid.add(diffLabel, 3, rowIndex);
    }
}