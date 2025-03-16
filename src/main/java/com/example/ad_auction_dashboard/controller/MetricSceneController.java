package com.example.ad_auction_dashboard.controller;

import com.example.ad_auction_dashboard.logic.CampaignMetrics;
import com.example.ad_auction_dashboard.logic.LogoutHandler;
import com.example.ad_auction_dashboard.logic.SaveCampaignDialog;
import com.example.ad_auction_dashboard.logic.TimeFilteredMetrics;
import com.example.ad_auction_dashboard.logic.UserSession;
import com.example.ad_auction_dashboard.viewer.AdminPanelScene;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
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
    @FXML
    private Button adminPanelBtn;

    @FXML
    private Label userWelcomeLabel;

    @FXML
    private Button logoutBtn;

    private CampaignMetrics metrics; // the campaign data model
    @FXML
    private ComboBox<String> genderFilterComboBox;

    @FXML
    private ComboBox<String> contextFilterComboBox;

    @FXML
    private Button resetFiltersButton;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;


    // Add a field for TimeFilteredMetrics
    private TimeFilteredMetrics timeFilteredMetrics;

    @FXML
    private Button saveToDatabaseBtn;

    @FXML
    public void initialize() {
        // Update welcome message with username if the label exists
        if (userWelcomeLabel != null && UserSession.getInstance().getUser() != null) {
            userWelcomeLabel.setText("Hello, " + UserSession.getInstance().getUser().getUsername());
        }
        if (genderFilterComboBox != null) {
            genderFilterComboBox.getItems().addAll("All", "Male", "Female");
            genderFilterComboBox.setValue("All");
            genderFilterComboBox.setOnAction(e -> applyFilters());
        }

        if (contextFilterComboBox != null) {
            contextFilterComboBox.getItems().addAll("All", "News", "Shopping", "Social Media",
                "Blog", "Hobbies", "Travel");
            contextFilterComboBox.setValue("All");
            contextFilterComboBox.setOnAction(e -> applyFilters());
        }
        // Date picker event listeners
        startDatePicker.setOnAction(e -> {
            validateDateRange();
            applyFilters();

        });

        endDatePicker.setOnAction(e -> {
            validateDateRange();
            applyFilters();
        });
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

    public void setMetrics(CampaignMetrics metrics) {
        this.metrics = metrics;

        // Create TimeFilteredMetrics for filtering
        this.timeFilteredMetrics = new TimeFilteredMetrics(
            metrics.getImpressionLogs(),
            metrics.getServerLogs(),
            metrics.getClickLogs(),
            metrics.getBouncePagesThreshold(),
            metrics.getBounceSecondsThreshold()
        );

        // Save metrics in UserSession
        UserSession.getInstance().setCurrentCampaignMetrics(metrics);

        // Initialize date pickers with campaign start and end dates
        if (startDatePicker != null && endDatePicker != null) {
            LocalDateTime campaignStart = metrics.getCampaignStartDate();
            LocalDateTime campaignEnd = metrics.getCampaignEndDate();

            if (campaignStart != null && campaignEnd != null) {
                startDatePicker.setValue(campaignStart.toLocalDate());
                endDatePicker.setValue(campaignEnd.toLocalDate());
                LocalDate startDate = campaignStart.toLocalDate();
                LocalDate endDate = campaignEnd.toLocalDate();
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

        }


        // Get filter values from UserSession if available
        applyFilterSettingsFromSession();

        updateUI();
    }
    private void applyFilters() {
        if (timeFilteredMetrics == null) return;

        // Get filter values
        String gender = (genderFilterComboBox != null) ? genderFilterComboBox.getValue() : "All";
        String context = (contextFilterComboBox != null) ? contextFilterComboBox.getValue() : "All";

        // Apply filters
        timeFilteredMetrics.setGenderFilter(gender.equals("All") ? null : gender);
        timeFilteredMetrics.setContextFilter(context.equals("All") ? null : context);

        // Save filter settings to UserSession
        saveFilterSettingsToSession();

        // Get time boundaries from date pickers if available, otherwise use full range
        LocalDateTime start, end;

        if (startDatePicker != null && startDatePicker.getValue() != null) {
            // Start at beginning of selected day
            start = startDatePicker.getValue().atStartOfDay();
        } else {
            start = metrics.getCampaignStartDate();
        }

        if (endDatePicker != null && endDatePicker.getValue() != null) {
            // End at end of selected day (23:59:59)
            end = endDatePicker.getValue().atTime(23, 59, 59);
        } else {
            end = metrics.getCampaignEndDate();
        }

        // Apply time frame
        timeFilteredMetrics.computeForTimeFrame(start, end, "Daily");

        // Update metrics display with filtered data
        updateUIWithFilteredData();
    }

    @FXML
    private void handleResetFilters() {
        if (genderFilterComboBox != null) genderFilterComboBox.setValue("All");
        if (contextFilterComboBox != null) contextFilterComboBox.setValue("All");

        // Reset date pickers to campaign start/end dates if available
        if (startDatePicker != null && endDatePicker != null && metrics != null) {
            LocalDateTime campaignStart = metrics.getCampaignStartDate();
            LocalDateTime campaignEnd = metrics.getCampaignEndDate();

            if (campaignStart != null && campaignEnd != null) {
                startDatePicker.setValue(campaignStart.toLocalDate());
                endDatePicker.setValue(campaignEnd.toLocalDate());
            }
        }

        if (timeFilteredMetrics != null) {
            timeFilteredMetrics.setGenderFilter(null);
            timeFilteredMetrics.setContextFilter(null);

            // Clear filters in session
            UserSession.getInstance().clearFilterSettings();

            // Recompute for full range
            LocalDateTime start = metrics.getCampaignStartDate();
            LocalDateTime end = metrics.getCampaignEndDate();
            timeFilteredMetrics.computeForTimeFrame(start, end, "Daily");

            updateUI(); // Use original unfiltered metrics
        }
    }
    private void updateUIWithFilteredData() {
        // Update all metric text fields with filtered values
        impressionsText.setText(String.valueOf(timeFilteredMetrics.getNumberOfImpressions()));
        clicksText.setText(String.valueOf(timeFilteredMetrics.getNumberOfClicks()));
        uniquesText.setText(String.valueOf(timeFilteredMetrics.getNumberOfUniques()));
        bouncesText.setText(String.valueOf(timeFilteredMetrics.getNumberOfBounces()));
        conversionsText.setText(String.valueOf(timeFilteredMetrics.getNumberOfConversions()));
        totalCostText.setText(String.format("%.6f", timeFilteredMetrics.getTotalCost()));
        ctrText.setText(String.format("%.6f", timeFilteredMetrics.getCTR()));
        cpcText.setText(String.format("%.6f", timeFilteredMetrics.getCPC()));
        cpaText.setText(String.format("%.6f", timeFilteredMetrics.getCPA()));
        cpmText.setText(String.format("%.6f", timeFilteredMetrics.getCPM()));
        bounceRateText.setText(String.format("%.6f", timeFilteredMetrics.getBounceRate()));
    }
    private void saveFilterSettingsToSession() {
        UserSession session = UserSession.getInstance();

        if (genderFilterComboBox != null) {
            session.setFilterSetting("gender", genderFilterComboBox.getValue());
        }

        if (contextFilterComboBox != null) {
            session.setFilterSetting("context", contextFilterComboBox.getValue());
        }
    }

    // Method to apply filter settings from UserSession
    private void applyFilterSettingsFromSession() {
        UserSession session = UserSession.getInstance();

        // Apply gender filter if saved
        String gender = session.getFilterSetting("gender");
        if (gender != null && genderFilterComboBox != null) {
            genderFilterComboBox.setValue(gender);
        }

        // Apply context filter if saved
        String context = session.getFilterSetting("context");
        if (context != null && contextFilterComboBox != null) {
            contextFilterComboBox.setValue(context);
        }

        // If any filters were applied, update metrics
        if ((gender != null && !gender.equals("All")) ||
            (context != null && !context.equals("All"))) {
            applyFilters();
        }
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
            UserSession.getInstance().setCurrentCampaignMetrics(metrics);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/ad_auction_dashboard/fxml/ChartScene.fxml"));
            Parent root = loader.load();
            // Retrieve the ChartSceneController
            ChartSceneController chartController = loader.getController();
            // Pass the campaign data
            chartController.setCampaignMetrics(metrics);

            Stage stage = (Stage) impressionsText.getScene().getWindow();
            Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles the Change Bounce button click.
     * Uses a simple dialog to get new bounce threshold values and updates the metrics.
     */
    @FXML
    private void handleChangeBounce(ActionEvent event) {
        // Get current bounce thresholds for initial values
        int currentPagesThreshold = metrics.getBouncePagesThreshold();
        int currentSecondsThreshold = metrics.getBounceSecondsThreshold();

        // Create input dialog components
        TextInputDialog pagesDialog = new TextInputDialog(String.valueOf(currentPagesThreshold));
        pagesDialog.setTitle("Bounce Settings");
        pagesDialog.setHeaderText("Set bounce pages threshold");
        pagesDialog.setContentText("Pages viewed:");

        // Show pages dialog and get result
        Optional<String> pagesResult = pagesDialog.showAndWait();
        if (pagesResult.isPresent()) {
            try {
                int newPagesThreshold = Integer.parseInt(pagesResult.get());

                // Get seconds threshold
                TextInputDialog secondsDialog = new TextInputDialog(String.valueOf(currentSecondsThreshold));
                secondsDialog.setTitle("Bounce Settings");
                secondsDialog.setHeaderText("Set bounce time threshold");
                secondsDialog.setContentText("Seconds on site:");

                // Show seconds dialog and get result
                Optional<String> secondsResult = secondsDialog.showAndWait();
                if (secondsResult.isPresent()) {
                    try {
                        int newSecondsThreshold = Integer.parseInt(secondsResult.get());

                        // Update the bounce criteria in the metrics object
                        // This will automatically recalculate bounce-related metrics
                        metrics.setBounceCriteria(newPagesThreshold, newSecondsThreshold);

                        // Update the UI to show the new values
                        updateUI();

                    } catch (NumberFormatException e) {
                        showAlert("Invalid number format for seconds threshold");
                    } catch (IllegalArgumentException e) {
                        showAlert(e.getMessage());
                    }
                }
            } catch (NumberFormatException e) {
                showAlert("Invalid number format for pages threshold");
            } catch (IllegalArgumentException e) {
                showAlert(e.getMessage());
            }
        }
    }

    @FXML
    private void handleHistogramView(ActionEvent event) {
        try {
            UserSession.getInstance().setCurrentCampaignMetrics(metrics);

            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/com/example/ad_auction_dashboard/fxml/HistogramScene.fxml"));
            Parent root = loader.load();

            // Get controller and pass data
            HistogramController controller = loader.getController();
            controller.setCampaignMetrics(metrics);

            // Default to click cost histogram
            controller.setHistogramType("Click Cost");

            // Switch scene
            Stage stage = (Stage) impressionsText.getScene().getWindow();
            Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Event handler for opening the admin panel
    @FXML
    private void handleAdminPanel(ActionEvent event) {

        if (!UserSession.getInstance().isAdmin()) {
            showAlert("Admin Level Access Required");
            return;
        }

        try {
            UserSession.getInstance().setCurrentCampaignMetrics(metrics);
            UserSession.getInstance().setPreviousScene("MetricScene");
            Stage stage = (Stage) adminPanelBtn.getScene().getWindow();
            new AdminPanelScene(stage, 930, 692);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error accessing Admin panel");
        }
    }
    @FXML
    private void handleToSaveToDatabase(ActionEvent event) {
        if (metrics == null) {
            showAlert("No campaign data to save");
            return;
        }

        // Only editors and admins can save campaigns
        if (!UserSession.getInstance().isEditor()) {
            showAlert("You need at least Editor permissions to save campaigns");
            return;
        }

        // Get the current stage
        Stage stage = (Stage) saveToDatabaseBtn.getScene().getWindow();

        // Show save dialog
        boolean saved = SaveCampaignDialog.showDialog(stage, metrics);

        if (saved) {
            // Optional: Update UI to reflect successful save
            if (UserSession.getInstance().getUser() != null) {
                userWelcomeLabel.setText("Hello, " + UserSession.getInstance().getUser().getUsername() +
                    " (Campaign saved)");
            }
        }
    }
    @FXML
    private void handleLogout(ActionEvent event) {
        if (logoutBtn != null) {
            LogoutHandler.handleLogout(event);
        }
    }

    /**
     * Shows an alert dialog with the specified message
     */
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}