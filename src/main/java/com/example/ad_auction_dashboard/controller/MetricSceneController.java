package com.example.ad_auction_dashboard.controller;

import com.example.ad_auction_dashboard.logic.Campaign;
import com.example.ad_auction_dashboard.logic.CampaignComparisonDialog;
import com.example.ad_auction_dashboard.logic.CampaignDatabase;
import com.example.ad_auction_dashboard.logic.CampaignMetrics;
import com.example.ad_auction_dashboard.logic.FullCampaignComparisonView;
import com.example.ad_auction_dashboard.logic.LogoutHandler;
import com.example.ad_auction_dashboard.logic.SaveCampaignDialog;
import com.example.ad_auction_dashboard.logic.TimeFilteredMetrics;
import com.example.ad_auction_dashboard.logic.UserSession;
import com.example.ad_auction_dashboard.viewer.AdminPanelScene;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

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
    @FXML
    private Button mainMenuButton;
    @FXML
    private Button chartsViewButton;
    @FXML
    private Button histogramButton;

    private CampaignMetrics metrics; // the campaign data model
    @FXML
    private ComboBox<String> genderFilterComboBox;

    @FXML
    private ComboBox<String> contextFilterComboBox;

    @FXML
    private ComboBox<String> ageFilterComboBox;

    @FXML
    private ComboBox<String> incomeFilterComboBox;

    @FXML
    private Button resetFiltersButton;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private ToggleButton colourSwitch;

    private String currentStyle;
    private Rectangle document, folder;
    private StackPane animationContainer;
    private Popup animationPopup;
    private Boolean playAnimation = false;


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

        // Initialize gender filter
        if (genderFilterComboBox != null) {
            genderFilterComboBox.getItems().addAll("All", "Male", "Female");
            genderFilterComboBox.setValue("All");
            genderFilterComboBox.setOnAction(e -> applyFilters());
        }

        // Initialize context filter
        if (contextFilterComboBox != null) {
            contextFilterComboBox.getItems().addAll("All", "News", "Shopping", "Social Media",
                "Blog");
            contextFilterComboBox.setValue("All");
            contextFilterComboBox.setOnAction(e -> applyFilters());
        }

        // Initialize age filter
        if (ageFilterComboBox != null) {
            ageFilterComboBox.getItems().addAll("All", "<25", "25-34", "35-44", "45-54", ">54");
            ageFilterComboBox.setValue("All");
            ageFilterComboBox.setOnAction(e -> applyFilters());
        }

        // Initialize income filter
        if (incomeFilterComboBox != null) {
            incomeFilterComboBox.getItems().addAll("All", "Low", "Medium", "High");
            incomeFilterComboBox.setValue("All");
            incomeFilterComboBox.setOnAction(e -> applyFilters());
        }

        // Date picker event listeners
        startDatePicker.setOnAction(e -> {
            validateDateRange();
            applyFilters();
        });
        animationPopup = new Popup();
        animationPopup.centerOnScreen();

        animationContainer = new StackPane();
        animationContainer.setMinSize(200, 125);
        animationContainer.setStyle("-fx-background-color: transparent;");

        // Create folder shape
        folder = new javafx.scene.shape.Rectangle(60, 40);
        folder.setFill(javafx.scene.paint.Color.GOLD);
        folder.setStroke(javafx.scene.paint.Color.DARKGOLDENROD);
        folder.setStrokeWidth(1);
        folder.setTranslateY(25);
        folder.setTranslateX(50);
        folder.setArcWidth(10);
        folder.setArcHeight(10);

        // Create document shape
        document = new Rectangle(30, 40);
        document.setFill(javafx.scene.paint.Color.WHITE);
        document.setStroke(Color.BLACK);
        document.setStrokeWidth(1);
        document.setTranslateY(25);
        document.setTranslateX(50);
        document.setArcWidth(10);
        document.setArcHeight(10);

        // Add all elements to the container
        animationContainer.getChildren().addAll(
                document, folder
        );
        animationPopup.getContent().add(animationContainer);

        endDatePicker.setOnAction(e -> {
            validateDateRange();
            applyFilters();
        });
        Circle thumb = new Circle(12);
        thumb.getStyleClass().add("thumb");
        colourSwitch.setGraphic(thumb);

        currentStyle = UserSession.getInstance().getCurrentStyle();
        if (Objects.equals(currentStyle, this.getClass().getClassLoader().getResource("styles/lightStyle.css").toString())){
            colourSwitch.setSelected(true);
            System.out.println("Switched");
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

        new Thread(() -> {
            toggleFilters(true);
            // Get filter values
            String gender = (genderFilterComboBox != null) ? genderFilterComboBox.getValue() : "All";
            String context = (contextFilterComboBox != null) ? contextFilterComboBox.getValue() : "All";
            String age = (ageFilterComboBox != null) ? ageFilterComboBox.getValue() : "All";
            String income = (incomeFilterComboBox != null) ? incomeFilterComboBox.getValue() : "All";

            // Apply filters
            timeFilteredMetrics.setGenderFilter(gender.equals("All") ? null : gender);
            timeFilteredMetrics.setContextFilter(context.equals("All") ? null : context);
            timeFilteredMetrics.setAgeFilter(age.equals("All") ? null : age);
            timeFilteredMetrics.setIncomeFilter(income.equals("All") ? null : income);

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
            Platform.runLater(() -> {
                this.updateUIWithFilteredData();
                toggleFilters(false);
            });
        }).start();
    }

    @FXML
    private void handleResetFilters() {
        if (genderFilterComboBox != null) genderFilterComboBox.setValue("All");
        if (contextFilterComboBox != null) contextFilterComboBox.setValue("All");
        if (ageFilterComboBox != null) ageFilterComboBox.setValue("All");
        if (incomeFilterComboBox != null) incomeFilterComboBox.setValue("All");

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
            toggleFilters(true);
            new Thread(() -> {
                timeFilteredMetrics.setGenderFilter(null);
                timeFilteredMetrics.setContextFilter(null);
                timeFilteredMetrics.setAgeFilter(null);
                timeFilteredMetrics.setIncomeFilter(null);

                // Clear filters in session
                UserSession.getInstance().clearFilterSettings();

                // Recompute for full range
                LocalDateTime start = metrics.getCampaignStartDate();
                LocalDateTime end = metrics.getCampaignEndDate();
                timeFilteredMetrics.computeForTimeFrame(start, end, "Daily");

                Platform.runLater(() -> {
                    this.updateUI();
                    toggleFilters(false);
                }); // Use original unfiltered metrics);
            }).start();
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

        if (ageFilterComboBox != null) {
            session.setFilterSetting("age", ageFilterComboBox.getValue());
        }

        if (incomeFilterComboBox != null) {
            session.setFilterSetting("income", incomeFilterComboBox.getValue());
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

        // Apply age filter if saved
        String age = session.getFilterSetting("age");
        if (age != null && ageFilterComboBox != null) {
            ageFilterComboBox.setValue(age);
        }

        // Apply income filter if saved
        String income = session.getFilterSetting("income");
        if (income != null && incomeFilterComboBox != null) {
            incomeFilterComboBox.setValue(income);
        }

        // If any filters were applied, update metrics
        if ((gender != null && !gender.equals("All")) ||
            (context != null && !context.equals("All")) ||
            (age != null && !age.equals("All")) ||
            (income != null && !income.equals("All"))) {
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
        UserSession.getInstance().setCurrentStyle(currentStyle);
        toggleControls(true);
        new Thread(() -> {
            try {
                // Load the start scene FXML
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/ad_auction_dashboard/fxml/StartScene.fxml"));
                Parent root = loader.load();
                // Optionally clear previous campaign from memory if necessary
                Stage stage = (Stage) impressionsText.getScene().getWindow();
                Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
                scene.getStylesheets().add(currentStyle);
                Platform.runLater(() -> stage.setScene(scene));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Transition to the Chart Scene
    @FXML
    private void handleChartView(ActionEvent event) {
        UserSession.getInstance().setCurrentStyle(currentStyle);
        toggleControls(true);
        // Retrieve the ChartSceneController
        new Thread(() -> {
            try {
                UserSession.getInstance().setCurrentCampaignMetrics(metrics);
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/ad_auction_dashboard/fxml/ChartScene.fxml"));
//                ChartSceneController chartController = loader.getController();
//                chartController.setCampaignMetrics(metrics);
                Parent root = loader.load();
                // Pass the campaign data

                Stage stage = (Stage) impressionsText.getScene().getWindow();
                Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
                scene.getStylesheets().add(currentStyle);
                Platform.runLater(() -> {
                    stage.setScene(scene);
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
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
                        new Thread(() -> {
                            metrics.setBounceCriteria(newPagesThreshold, newSecondsThreshold);

                            // Update the UI to show the new values
                            Platform.runLater(this::updateUI);
                        }).start();

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
        UserSession.getInstance().setCurrentStyle(this.currentStyle);
        toggleControls(true);
        new Thread(() -> {
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
                scene.getStylesheets().add(this.currentStyle);
                Platform.runLater(() -> stage.setScene(scene));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Event handler for opening the admin panel
    @FXML
    private void handleAdminPanel(ActionEvent event) {

        if (!UserSession.getInstance().isAdmin()) {
            showAlert("Admin Level Access Required");
            return;
        }
        UserSession.getInstance().setCurrentStyle(this.currentStyle);
        toggleControls(true);
        new Thread(() -> {
            try {
                UserSession.getInstance().setCurrentCampaignMetrics(metrics);
                UserSession.getInstance().setPreviousScene("MetricScene");
                Platform.runLater(() -> {
                    Stage stage = (Stage) adminPanelBtn.getScene().getWindow();
                    new AdminPanelScene(stage, 930, 692, this.currentStyle);
                    stage.show();
                });
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error accessing Admin panel");
            }
        }).start();
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
        toggleControls(true);
        SaveCampaignDialog.showDialog(stage, metrics,this);

//        if (saved) {
//            // Optional: Update UI to reflect successful save
//            if (UserSession.getInstance().getUser() != null) {
//                userWelcomeLabel.setText("Hello, " + UserSession.getInstance().getUser().getUsername() +
//                    " (Campaign saved)");
//            }
//        }
    }
    @FXML
    private void handleLogout(ActionEvent event) {
        UserSession.getInstance().setCurrentStyle(this.currentStyle);
        if (logoutBtn != null) {
            toggleControls(true);
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

    @FXML
    private void handleCompareFullCampaigns(ActionEvent event) {
        // Check permissions first
        if (!UserSession.getInstance().isEditor()) {
            showAlert("You need Editor or Admin permissions to compare campaigns");
            return;
        }

        // Get current campaign name
        String currentCampaignName = "Current Campaign";

        // Show dialog to select campaign to compare with
        Stage stage = (Stage) impressionsText.getScene().getWindow();
        CampaignDatabase.CampaignInfo campaignToCompare =
                //aware of problems with dialogs
            CampaignComparisonDialog.showDialog(stage, currentCampaignName);

        if (campaignToCompare == null) {
            // User cancelled
            return;
        }

        try {
            // Simply pass the campaign ID to show comparison - no need to load full campaign!
            FullCampaignComparisonView.showComparison(
                stage,
                metrics,
                campaignToCompare.getCampaignId(),
                currentCampaignName,
                campaignToCompare.getCampaignName()
            );
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error comparing campaigns: " + e.getMessage());
        }
    }

    @FXML
    private void toggleColour(ActionEvent event){
        if (colourSwitch.isSelected()){
            currentStyle = this.getClass().getClassLoader().getResource("styles/lightStyle.css").toString();
            colourSwitch.getScene().getStylesheets().clear();
            colourSwitch.getScene().getStylesheets().add(currentStyle);
        } else {
            currentStyle = this.getClass().getClassLoader().getResource("styles/style.css").toString();
            colourSwitch.getScene().getStylesheets().clear();
            colourSwitch.getScene().getStylesheets().add(currentStyle);
        }
    }

    public void toggleControls(Boolean bool){
        mainMenuButton.setDisable(bool);
        adminPanelBtn.setDisable(bool);
        logoutBtn.setDisable(bool);
        chartsViewButton.setDisable(bool);
        histogramButton.setDisable(bool);
        saveToDatabaseBtn.setDisable(bool);
    }

    public void toggleFilters(Boolean bool){
        contextFilterComboBox.setDisable(bool);
        genderFilterComboBox.setDisable(bool);
        ageFilterComboBox.setDisable(bool);
        incomeFilterComboBox.setDisable(bool);
        startDatePicker.setDisable(bool);
        endDatePicker.setDisable(bool);
        resetFiltersButton.setDisable(bool);
    }

    public void startSaveAnimation(){
        System.out.println("Starting Animation");
        playAnimation = true;
        animationPopup.show(logoutBtn.getScene().getWindow());
        playSaveAnimation();
    }
    public void stopSaveAnimation(){
        playAnimation = false;
        animationPopup.hide();
    }
    private void playSaveAnimation(){
        // Reset document position
        document.setTranslateX(-50);
        document.setTranslateY(-25);

        // Document movement animation
        TranslateTransition moveDocument = new TranslateTransition(Duration.seconds(1.5), document);
        moveDocument.setToX(50);
        moveDocument.setToY(25);
        moveDocument.setInterpolator(Interpolator.EASE_BOTH);

        // When document reaches folder, make it disappear
        moveDocument.setOnFinished(e -> {
            // Make folder "pulse" to indicate receipt
            ScaleTransition documentPulse = new ScaleTransition(Duration.millis(200), document);
            documentPulse.setFromX(1.0);
            documentPulse.setFromY(1.0);
            documentPulse.setToX(1.2);
            documentPulse.setToY(1.2);
            documentPulse.setCycleCount(2);
            documentPulse.setAutoReverse(true);
            documentPulse.play();

            // Fade out document
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), document);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.play();
        });

        Timer timer = new Timer();
        // Start animations
        moveDocument.play();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                document.setOpacity(1.0);
                System.out.println("TRYING");

                if (playAnimation){
                    playSaveAnimation();
                }
            }
        }, 2000);
    }

}