package com.example.ad_auction_dashboard.controller;

import com.example.ad_auction_dashboard.logic.TimeFilteredMetrics;
import java.time.LocalDateTime;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;

public class ChartPaneController {
    @FXML private LineChart<String, Number> lineChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private ComboBox<String> metricComboBox;
    @FXML private ComboBox<String> granularityComboBox;

    private TimeFilteredMetrics timeFilteredMetrics;
    private LocalDateTime currentStart;
    private LocalDateTime currentEnd;
    private String currentMetric = "Impressions";

    @FXML
    public void initialize() {
        // Set up metric options.
        metricComboBox.setItems(FXCollections.observableArrayList(
            "Impressions", "Clicks", "Uniques", "Bounces", "Conversions",
            "Total Cost", "CTR", "CPC", "CPA", "CPM", "Bounce Rate"
        ));
        metricComboBox.getSelectionModel().select("Impressions");

        // Set up granularity options.
        granularityComboBox.setItems(FXCollections.observableArrayList("Hourly", "Daily", "Weekly"));
        granularityComboBox.getSelectionModel().select("Daily");

        // Set initial axis labels.
        xAxis.setLabel("Time");
        yAxis.setLabel(metricComboBox.getSelectionModel().getSelectedItem());

        // When the user changes the metric or granularity, update the chart.
        metricComboBox.setOnAction(e -> {
            currentMetric = metricComboBox.getSelectionModel().getSelectedItem();
            yAxis.setLabel(currentMetric);
            updateChart();
        });
        granularityComboBox.setOnAction(e -> updateChart());
    }

    /**
     * Sets the TimeFilteredMetrics instance (shared across chart panes).
     */
    public void setTimeFilteredMetrics(TimeFilteredMetrics tfm) {
        this.timeFilteredMetrics = tfm;
    }

    /**
     * Updates the chart using the given time range.
     */
    public void updateTimeRange(LocalDateTime start, LocalDateTime end) {
        this.currentStart = start;
        this.currentEnd = end;
        updateChart();
    }

    /**
     * Aggregates data in buckets (based on granularity) and updates the LineChart.
     */
    private void updateChart() {
        if (currentStart == null || currentEnd == null || timeFilteredMetrics == null) {
            return;
        }
        lineChart.getData().clear();
        String granularity = granularityComboBox.getSelectionModel().getSelectedItem();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(currentMetric);

        LocalDateTime pointer = currentStart;
        while (!pointer.isAfter(currentEnd)) {
            LocalDateTime bucketStart = pointer;
            LocalDateTime bucketEnd;
            // Determine the end of the current bucket.
            switch (granularity) {
                case "Hourly":
                    bucketEnd = pointer.plusHours(1).minusSeconds(1);
                    break;
                case "Daily":
                    bucketEnd = pointer.plusDays(1).minusSeconds(1);
                    break;
                case "Weekly":
                    bucketEnd = pointer.plusWeeks(1).minusSeconds(1);
                    break;
                default:
                    bucketEnd = pointer.plusDays(1).minusSeconds(1);
            }
            if (bucketEnd.isAfter(currentEnd)) {
                bucketEnd = currentEnd;
            }
            // Format a label for the bucket.
            String timeLabel = formatTimeLabel(pointer, granularity);
            // Compute the aggregated value for this bucket.
            Number aggregatedValue = getAggregatedValueForBucket(currentMetric, bucketStart, bucketEnd);
            series.getData().add(new XYChart.Data<>(timeLabel, aggregatedValue));

            pointer = bucketEnd.plusSeconds(1);
        }
        lineChart.getData().add(series);
        // Optionally disable auto-ranging and set bounds manually if needed:
        yAxis.setAutoRanging(false);
        double minValue = 0; // compute or decide a minimum value
        double maxValue = 100; // compute or decide a maximum value based on your data
        yAxis.setLowerBound(minValue);
        yAxis.setUpperBound(maxValue);
        yAxis.setTickUnit((maxValue - minValue) / 10); // for example, 10 tick marks

        // Force layout update:
        lineChart.layout();
    }

    /**
     * Uses the TimeFilteredMetrics instance to compute the aggregated value for the specified metric in the given time bucket.
     */
    private Number getAggregatedValueForBucket(String metric, LocalDateTime bucketStart, LocalDateTime bucketEnd) {
        timeFilteredMetrics.computeForTimeFrame(bucketStart, bucketEnd);
        switch (metric) {
            case "Impressions":
                return timeFilteredMetrics.getNumberOfImpressions();
            case "Clicks":
                return timeFilteredMetrics.getNumberOfClicks();
            case "Uniques":
                return timeFilteredMetrics.getNumberOfUniques();
            case "Bounces":
                return timeFilteredMetrics.getNumberOfBounces();
            case "Conversions":
                return timeFilteredMetrics.getNumberOfConversions();
            case "Total Cost":
                return timeFilteredMetrics.getTotalCost();
            case "CTR":
                return timeFilteredMetrics.getCTR();
            case "CPC":
                return timeFilteredMetrics.getCPC();
            case "CPA":
                return timeFilteredMetrics.getCPA();
            case "CPM":
                return timeFilteredMetrics.getCPM();
            case "Bounce Rate":
                return timeFilteredMetrics.getBounceRate();
            default:
                return 0;
        }
    }

    /**
     * Formats a LocalDateTime pointer as a string based on the selected granularity.
     */
    private String formatTimeLabel(LocalDateTime dt, String granularity) {
        switch (granularity) {
            case "Hourly":
                return dt.getHour() + ":00";
            case "Daily":
                return dt.getMonthValue() + "/" + dt.getDayOfMonth();
            case "Weekly":
                return "Week " + (dt.getDayOfYear() / 7);
            default:
                return dt.toString();
        }
    }
}
