package com.example.ad_auction_dashboard.controller;

import com.example.ad_auction_dashboard.charts.ClickCostHistogramGenerator;
import com.example.ad_auction_dashboard.charts.HistogramGenerator;
import com.example.ad_auction_dashboard.logic.CampaignMetrics;
import com.example.ad_auction_dashboard.logic.LogoutHandler;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
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
    private Label statusLabel; // Optional: can add this to your FXML for status messages

    private CampaignMetrics campaignMetrics;
    private final Map<String, HistogramGenerator> histogramGenerators = new HashMap<>();

    @FXML
    private Label histogramTitleLabel;

    @FXML
    private Button logoutBtn;

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

        // Set the initial bin size label
        updateBinSizeLabel((int)binSizeSlider.getValue());

        // Add listener for when the slider value changes
        binSizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            // Ensure we're working with integers and update the label properly
            int binCount = (int)Math.round(newVal.doubleValue());
            updateBinSizeLabel(binCount);

            // Debug output to trace value changes
            System.out.println("Slider raw value: " + newVal + ", Rounded bin count: " + binCount);

            updateHistogram();
        });

        // Set up event handlers with date validation
        histogramTypeComboBox.setOnAction(e -> updateHistogram());

        // Add validation when changing dates
        startDatePicker.setOnAction(e -> {
            validateDateRange();
            updateHistogram();
        });

        endDatePicker.setOnAction(e -> {
            validateDateRange();
            updateHistogram();
        });

        // Ensure the text area is initialized with empty text instead of placeholder
        if (descriptionTextArea != null) {
            descriptionTextArea.setText("");
        }
    }

    /**
     * Validates and corrects the date range if needed
     */
    private void validateDateRange() {
        if (startDatePicker.getValue() != null && endDatePicker.getValue() != null) {
            if (endDatePicker.getValue().isBefore(startDatePicker.getValue())) {
                // End date is before start date, correct it
                endDatePicker.setValue(startDatePicker.getValue());

                // Show alert to user
                showAlert("Invalid date range detected. End date has been adjusted to match start date.");
            }
        }
    }

    /**
     * Shows an alert to the user
     */
    private void showAlert(String message) {
        // First check if there's a status label to display the message
        if (statusLabel != null) {
            statusLabel.setText(message);
        } else {
            // Otherwise show an alert dialog
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        }
    }

    /**
     * Helper method to update the bin size label with consistent formatting
     */
    private void updateBinSizeLabel(int binCount) {
        if (binSizeLabel != null) {
            binSizeLabel.setText(String.format("Num: %d", binCount));
        }
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

        // Additional validation to ensure start date is not after end date
        if (startDate.isAfter(endDate)) {
            endDatePicker.setValue(startDate);
            endDate = startDate;
            showAlert("Start date cannot be after end date. Both dates have been set to the same day.");
            return; // Skip processing to let the user see the message
        }

        // Validate date range against campaign boundaries
        if (campaignMetrics.getCampaignStartDate() != null && campaignMetrics.getCampaignEndDate() != null) {
            LocalDate campaignStartDate = campaignMetrics.getCampaignStartDate().toLocalDate();
            LocalDate campaignEndDate = campaignMetrics.getCampaignEndDate().toLocalDate();

            // Ensure dates are within campaign range
            if (startDate.isBefore(campaignStartDate)) {
                startDate = campaignStartDate;
                startDatePicker.setValue(startDate);
                showAlert("Start date adjusted to campaign start date.");
            }

            if (endDate.isAfter(campaignEndDate)) {
                endDate = campaignEndDate;
                endDatePicker.setValue(endDate);
                showAlert("End date adjusted to campaign end date.");
            }
        }

        // Convert to LocalDateTime
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        // Get selected histogram type
        String selectedType = histogramTypeComboBox.getValue();
        HistogramGenerator generator = histogramGenerators.get(selectedType);

        if (generator != null) {
            // Update chart labels - ensure they're checked for null
            if (histogramTitleLabel != null) {
                histogramTitleLabel.setText(generator.getTitle());
                // Ensure the title is visible
                histogramTitleLabel.setVisible(true);
            }

            if (xAxis != null) {
                xAxis.setLabel(generator.getXAxisLabel());
            }

            if (yAxis != null) {
                yAxis.setLabel(generator.getYAxisLabel());
            }

            // Ensure description is visible and set
            if (descriptionTextArea != null) {
                descriptionTextArea.setText(generator.getDescription());
                descriptionTextArea.setVisible(true);
            }

            // Get bin count from slider with proper rounding
            int binCount = (int)Math.round(binSizeSlider.getValue());

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

                // Apply styling to the bars to make them green
                for (XYChart.Data<String, Number> data : series.getData()) {
                    if (data.getNode() != null) {
                        data.getNode().setStyle("-fx-bar-fill: #4CAF50;");
                    }
                }

                // Set y-axis to start at 0
                if (yAxis != null) {
                    yAxis.setForceZeroInRange(true);
                }

                // Improve x-axis label display
                if (xAxis != null) {
                    xAxis.setTickLabelRotation(45);
                }

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

    @FXML
    private void handleLogout(ActionEvent event) {
        if (logoutBtn != null) {
            LogoutHandler.handleLogout(event);
        }
    }
}