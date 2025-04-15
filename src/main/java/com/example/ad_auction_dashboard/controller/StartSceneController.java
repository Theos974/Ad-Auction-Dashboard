package com.example.ad_auction_dashboard.controller;

import com.example.ad_auction_dashboard.Multimedia;
import com.example.ad_auction_dashboard.logic.Campaign;
import com.example.ad_auction_dashboard.logic.CampaignMetrics;
import com.example.ad_auction_dashboard.logic.FileHandler;
import com.example.ad_auction_dashboard.logic.LoadCampaignDialog;
import com.example.ad_auction_dashboard.logic.LogoutHandler;
import com.example.ad_auction_dashboard.logic.UserSession;
import com.example.ad_auction_dashboard.viewer.AdminPanelScene;
import java.io.IOException;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import java.io.File;
import java.util.Objects;

import javafx.stage.Popup;
import javafx.stage.Stage;

public class StartSceneController {

    @FXML
    private Button loadZipBtn;

    @FXML
    private Button createCampaignBtn;

    @FXML
    private Button adminPanelBtn;

    @FXML
    private Button loadFromDbBtn;

    @FXML
    private Button logoutBtn;

    @FXML
    private Label userWelcomeLabel;

    @FXML
    private Text statusText;

    @FXML
    private ToggleButton colourSwitch;

    private Popup loadingPopup = null;
    private Label loadingLabel = null;

    private String currentStyle;

    private Campaign campaign;

    @FXML
    public void initialize() {
        UserSession session = UserSession.getInstance();

        // Update welcome message with username
        if (session.getUser() != null) {
            userWelcomeLabel.setText("Hello, " + session.getUser().getUsername());
        }

        // Handle different permission levels
        if (!session.isEditor()) {
            // Viewers can't import ZIP files
            loadZipBtn.setDisable(true);
            loadZipBtn.setVisible(false);

            // Show alternative button for loading from database
            loadFromDbBtn.setVisible(true);
        } else {
            // Editors can see both options
            loadFromDbBtn.setVisible(true);
        }

        if (session.isAdmin()) {
            // Show admin panel button only for admins
            adminPanelBtn.setVisible(true);
        } else {
            adminPanelBtn.setVisible(false);
        }

        Circle thumb = new Circle(12);
        thumb.getStyleClass().add("thumb");
        colourSwitch.setGraphic(thumb);

        currentStyle = session.getCurrentStyle();
        System.out.println("STYLE: " + currentStyle);
        if (Objects.equals(currentStyle, this.getClass().getClassLoader().getResource("styles/lightStyle.css").toString())){
            colourSwitch.setSelected(true);
        }
    }
    // Event handler for loading a ZIP file and creating the campaign
    @FXML
    private void handleLoadZip(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load ZIP File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP Files", "*.zip"));
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home") + File.separator + "Documents"));
        File selected = fileChooser.showOpenDialog(loadZipBtn.getScene().getWindow());
        if (selected != null) {
            FileHandler fileHandler = new FileHandler();
//            loadingLabel = new Label("Loading ZIP");
//            loadingPopup = new Popup();
//            loadingPopup.getContent().add(loadingLabel);
//            loadingPopup.show(loadZipBtn.getScene().getWindow());
            fileHandler.setStartScene(this);
            loadZipBtn.getScene().setCursor(Cursor.WAIT);
            new Thread(() -> {
                toggleControls(true);
                campaign = fileHandler.openZip(selected.getAbsolutePath());
                if (campaign != null) {
                    statusText.setText("Campaign loaded from: " + selected.getName());
                } else {
                    statusText.setText("Error loading campaign from ZIP.");
                }
                statusText.getScene().setCursor(Cursor.DEFAULT);
                toggleControls(false);
            }).start();
        }
    }

    public void updatePopup(String update){
        if (loadingPopup != null && loadingPopup.isShowing()){
            loadingLabel.setText("Loading: " + update);
        }
    }

    // Event handler for switching to the campaign screen
    @FXML
    private void handleCreateCampaign(ActionEvent event) {
        if (campaign == null) {
            statusText.setText("Please load a ZIP file first.");
            return;
        }
        toggleControls(true);
        statusText.setText("Campaign created. Switching scene...");
        CampaignMetrics metrics = new CampaignMetrics(campaign);
        UserSession.getInstance().setCurrentStyle(currentStyle);
        statusText.getScene().setCursor(Cursor.WAIT);
        new Thread (() -> {
            try {
                UserSession.getInstance().setPreviousScene("StartScene");
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/ad_auction_dashboard/fxml/MetricScene2.fxml"));
                Parent root = loader.load();
                MetricSceneController controller = loader.getController();
                controller.setMetrics(metrics);
                Stage stage = (Stage) createCampaignBtn.getScene().getWindow();
                Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
                scene.getStylesheets().add(currentStyle);
                Platform.runLater(() -> stage.setScene(scene));
            } catch (IOException e) {
                e.printStackTrace();
                statusText.setText("Error switching scene.");
            }
        }).start();
    }

    // Event handler for loading from database (for viewers)
    @FXML
    private void handleLoadFromDatabase(ActionEvent event) {
        // Get the current stage
        Stage stage = (Stage) loadFromDbBtn.getScene().getWindow();

        // Show loading status
        statusText.setText("Opening campaign selection dialog...");

        // This must run on the JavaFX Application Thread since it shows a dialog
        toggleControls(true);
        Campaign loadedCampaign = LoadCampaignDialog.showDialog(stage);

        if (loadedCampaign != null) {
            // Now we have a campaign, update status
            statusText.setText("Campaign loaded successfully. Switching to metrics view...");

            // Process the loaded campaign
            createCampaignFromData(loadedCampaign);
        } else {
            toggleControls(false);
            statusText.setText("No campaign was loaded.");
        }
    }

    private void createCampaignFromData(Campaign campaign) {
        statusText.setText("Campaign loaded. Switching to metrics view...");
        CampaignMetrics metrics = new CampaignMetrics(campaign);
        UserSession.getInstance().setCurrentStyle(currentStyle);
        statusText.getScene().setCursor(Cursor.WAIT);
        new Thread(() -> {
            try {
                UserSession.getInstance().setPreviousScene("StartScene");
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/ad_auction_dashboard/fxml/MetricScene2.fxml"));
                Parent root = loader.load();
                MetricSceneController controller = loader.getController();
                controller.setMetrics(metrics);

                Stage stage = (Stage) createCampaignBtn.getScene().getWindow();
                Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
                scene.getStylesheets().add(currentStyle);
                Platform.runLater(() -> stage.setScene(scene));
            } catch (IOException e) {
                e.printStackTrace();
                statusText.setText("Error switching to metrics view.");
            }
        }).start();
    }


    // Event handler for opening the admin panel
    @FXML
    private void handleAdminPanel(ActionEvent event) {
        UserSession.getInstance().setCurrentStyle(this.currentStyle);
        try {
            UserSession.getInstance().setPreviousScene("StartScene");
            Stage stage = (Stage) adminPanelBtn.getScene().getWindow();
            new AdminPanelScene(stage, 930, 692, this.currentStyle);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            statusText.setText("Error opening admin panel.");
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

    // Event handler for logout button
    @FXML
    private void handleLogout(ActionEvent event) {
        UserSession.getInstance().setCurrentStyle(this.currentStyle);
        LogoutHandler.handleLogout(event);
    }

    public void toggleControls(Boolean bool){
        logoutBtn.setDisable(bool);
        adminPanelBtn.setDisable(bool);
        loadZipBtn.setDisable(bool);
        loadFromDbBtn.setDisable(bool);
        createCampaignBtn.setDisable(bool);
    }
}