package com.example.ad_auction_dashboard.controller;

import com.example.ad_auction_dashboard.charts.ClickCostHistogramGenerator;
import com.example.ad_auction_dashboard.charts.HistogramGenerator;
import com.example.ad_auction_dashboard.logic.CampaignMetrics;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class HistogramController {

    @FXML
    private ComboBox<String> histogramTypeComboBox;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private Slider binSizeSlider;

    @FXML
    private Label binSizeLabel;

    @FXML
    private BarChart<String, Number> histogramChart;

    @FXML
    private CategoryAxis xAxis;

    @FXML
    private NumberAxis yAxis;

    @FXML
    private TextArea descriptionTextArea;

    @FXML
    private Label campaignDateRangeLabel;

    private CampaignMetrics campaignMetrics;
    private final Map<String, HistogramGenerator> histogramGenerators = new HashMap<>();

    @FXML
    public void initialize() {
        // Register histogram generators
        registerHistogramGenerators();

        // Fill histogram type combo box
        histogramTypeComboBox.getItems().addAll(histogramGenerators.keySet());

        // Set default to Click Cost (currently the only required histogram)
        histogramTypeComboBox.setValue("Click Cost");

        // Configure bin size slider
        binSizeSlider.setMin(5);
        binSizeSlider.setMax(20);
        binSizeSlider.setValue(10);
        binSizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            binSizeLabel.setText(String.format("Bins: %d", newVal.intValue()));
            updateHistogram();
        });

        // Set up event handlers
        histogramTypeComboBox.setOnAction(e -> updateHistogram());
        startDatePicker.setOnAction(e -> updateHistogram());
        endDatePicker.setOnAction(e -> updateHistogram());
    }

    /**
     * Register all available histogram generators
     */
    private void registerHistogramGenerators() {
        // Register ClickCost histogram generator (the only one required for now)
        histogramGenerators.put("Click Cost", new ClickCostHistogramGenerator());

        // For future expansion, add more histogram generators here
    }

    public void setCampaignMetrics(CampaignMetrics metrics) {
        this.campaignMetrics = metrics;

        // Get campaign date boundaries
        LocalDateTime campaignStart = metrics.getCampaignStartDate();
        LocalDateTime campaignEnd = metrics.getCampaignEndDate();

        if (campaignStart != null && campaignEnd != null) {
            LocalDate startDate = campaignStart.toLocalDate();
            LocalDate endDate = campaignEnd.toLocalDate();

            // Set default values to campaign start and end dates
            startDatePicker.setValue(startDate);
            endDatePicker.setValue(endDate);

            // Update campaign date range label if it exists
            if (campaignDateRangeLabel != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
                campaignDateRangeLabel.setText(String.format(
                    "Campaign period: %s to %s",
                    startDate.format(formatter),
                    endDate.format(formatter)));
            }

            // Set date range constraints for start date picker
            startDatePicker.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    setDisable(empty || date.isBefore(startDate) || date.isAfter(endDate));
                }
            });

            // Set date range constraints for end date picker
            endDatePicker.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    setDisable(empty || date.isBefore(startDate) || date.isAfter(endDate));
                }
            });
        }

        // Generate initial histogram - done after all setup
        updateHistogram();
    }

    public void setHistogramType(String histogramType) {
        if (histogramGenerators.containsKey(histogramType)) {
            histogramTypeComboBox.setValue(histogramType);
            updateHistogram();
        }
    }

    private void updateHistogram() {
        if (campaignMetrics == null) return;

        // Get date range
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        if (startDate == null || endDate == null) return;

        // Validate date range against campaign boundaries
        if (campaignMetrics.getCampaignStartDate() != null && campaignMetrics.getCampaignEndDate() != null) {
            LocalDate campaignStartDate = campaignMetrics.getCampaignStartDate().toLocalDate();
            LocalDate campaignEndDate = campaignMetrics.getCampaignEndDate().toLocalDate();

            // Ensure dates are within campaign range
            if (startDate.isBefore(campaignStartDate)) {
                startDate = campaignStartDate;
                startDatePicker.setValue(startDate);
            }

            if (endDate.isAfter(campaignEndDate)) {
                endDate = campaignEndDate;
                endDatePicker.setValue(endDate);
            }
        }

        // Convert to LocalDateTime
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        // Get selected histogram type
        String selectedType = histogramTypeComboBox.getValue();
        HistogramGenerator generator = histogramGenerators.get(selectedType);

        if (generator != null) {
            // Update chart labels
            histogramChart.setTitle(generator.getTitle());
            xAxis.setLabel(generator.getXAxisLabel());
            yAxis.setLabel(generator.getYAxisLabel());

            // Ensure description is visible and set
            if (descriptionTextArea != null) {
                descriptionTextArea.setText(generator.getDescription());
                descriptionTextArea.setVisible(true);
            }

            // Get bin count from slider
            int binCount = (int) binSizeSlider.getValue();

            try {
                // Calculate histogram data
                Map<String, Integer> histogramData = generator.generateHistogramData(
                    campaignMetrics, start, end, binCount);

                // Update the chart
                histogramChart.getData().clear();
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName("Frequency");

                // Check if there's actual data
                if (histogramData.isEmpty() ||
                    (histogramData.size() == 1 && histogramData.containsKey("No data available"))) {
                    // Add placeholder for no data
                    series.getData().add(new XYChart.Data<>("No data available", 0));
                } else {
                    // Add real histogram data
                    for (Map.Entry<String, Integer> entry : histogramData.entrySet()) {
                        series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
                    }
                }

                histogramChart.getData().add(series);

                // Set y-axis to start at 0
                NumberAxis yAxis = (NumberAxis) histogramChart.getYAxis();
                yAxis.setForceZeroInRange(true);

                // Improve x-axis label display
                CategoryAxis xAxis = (CategoryAxis) histogramChart.getXAxis();
                xAxis.setTickLabelRotation(45);

            } catch (Exception e) {
                // Handle errors gracefully
                e.printStackTrace();
                if (descriptionTextArea != null) {
                    descriptionTextArea.setText("Error generating histogram: " + e.getMessage() +
                        "\nPlease try different date ranges or bin sizes.");
                }
            }
        }
    }

    @FXML
    private void handleBackToMetrics(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/ad_auction_dashboard/fxml/MetricScene2.fxml"));
            Parent root = loader.load();

            // Get the controller and pass the campaign metrics
            MetricSceneController controller = loader.getController();
            controller.setMetrics(campaignMetrics);

            // Switch to the metrics scene
            Scene scene = new Scene(root);
            Stage stage = (Stage) histogramChart.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}