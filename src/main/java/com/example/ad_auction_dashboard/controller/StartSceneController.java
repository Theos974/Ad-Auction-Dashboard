package com.example.ad_auction_dashboard.controller;

import com.example.ad_auction_dashboard.Multimedia;
import com.example.ad_auction_dashboard.logic.Campaign;
import com.example.ad_auction_dashboard.logic.CampaignMetrics;
import com.example.ad_auction_dashboard.logic.FileHandler;
import com.example.ad_auction_dashboard.logic.LoadCampaignDialog;
import com.example.ad_auction_dashboard.logic.LogoutHandler;
import com.example.ad_auction_dashboard.logic.UserSession;
import com.example.ad_auction_dashboard.viewer.AdminPanelScene;

import java.awt.*;
import java.io.IOException;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import java.io.File;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.util.Duration;

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
    public Text statusText;

    @FXML
    private ToggleButton colourSwitch;

    private Rectangle document, folder;

    private Popup loadingPopup = new Popup();
    @FXML
    private ImageView logoImage;
    private boolean playAnimation = false;
    private ParallelTransition transition;
    private StackPane animationContainer;

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
        //
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



        Circle thumb = new Circle(12);
        thumb.getStyleClass().add("thumb");
        colourSwitch.setGraphic(thumb);

        currentStyle = session.getCurrentStyle();
        System.out.println("STYLE: " + currentStyle);
        if (Objects.equals(currentStyle, this.getClass().getClassLoader().getResource("styles/lightStyle.css").toString())) {
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
            fileHandler.setStartScene(this);
            startLoadAnimation();
            new Thread(() -> {
                toggleControls(true);
                campaign = fileHandler.openZip(selected.getAbsolutePath());
                if (campaign != null) {
                    statusText.setText("Campaign loaded from: " + selected.getName());
                } else {
                    statusText.setText("Error loading campaign from ZIP.");
                }
                toggleControls(false);
                Platform.runLater(this::stopLoadAnimation);
            }).start();
        }
    }

    public void updatePopup(String logType, int count) {
        statusText.setText("Loading " + logType + " Log Number: " + count);
    }
    public void updatePopup(String logType){
        statusText.setText("Loading all " + logType + " logs from the database!");
    }

    public void playDeleteAnimation(){
        playAnimation = true;
        transition = setDeleteAnimation();

        Platform.runLater(() -> transition.play());
    }
    public void stopDeleteAnimation(){
        playAnimation = false;
        transition.jumpTo(Duration.ZERO);
        transition.stop();
        logoImage.setImage(new Image(String.valueOf(this.getClass().getClassLoader().getResource("images/small logo.png"))));
        //loadingPopup.hide();
    }

    private ParallelTransition setDeleteAnimation(){
        if (Objects.equals(this.currentStyle, this.getClass().getClassLoader().getResource("styles/lightStyle.css").toString())){
            logoImage.setImage(new Image(String.valueOf(this.getClass().getClassLoader().getResource("images/file.png"))));
        } else {
            logoImage.setImage(new Image(String.valueOf(this.getClass().getClassLoader().getResource("images/white_file.png"))));
        }


        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(700), logoImage);
        scaleTransition.setToX(0.1);
        scaleTransition.setToY(0.1);

        // Create a fade animation
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(1000), logoImage);
        fadeTransition.setToValue(0);

        // Play both animations in parallel
        ParallelTransition transition = new ParallelTransition(scaleTransition, fadeTransition);
        transition.setCycleCount(Animation.INDEFINITE);
        return transition;
    }
    public void startLoadAnimation(){
        playAnimation = true;
        VBox temp = ((VBox) logoImage.getParent());
        temp.getChildren().remove(0);
        temp.getChildren().add(0, animationContainer);
        playLoadAnimation();
    }
    private void playLoadAnimation(){
        // Reset document position
        document.setTranslateX(50);
        document.setTranslateY(25);

        // Document movement animation
        TranslateTransition moveDocument = new TranslateTransition(Duration.seconds(1.5), document);
        moveDocument.setToX(-50);
        moveDocument.setToY(-25);
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
                document.setTranslateX(50);
                document.setTranslateY(25);

                if (playAnimation){
                    playLoadAnimation();
                }
            }
        }, 2000);
    }
    public void stopLoadAnimation(){
        playAnimation = false;
        VBox temp = ((VBox) animationContainer.getParent());
        temp.getChildren().remove(0);
        temp.getChildren().add(0, logoImage);
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
        LoadCampaignDialog.showDialog(stage, this);
    }

    public void createCampaignFromData(Campaign campaign) {
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
    private void toggleColour(ActionEvent event) {
        if (colourSwitch.isSelected()) {
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

    public void toggleControls(Boolean bool) {
        logoutBtn.setDisable(bool);
        adminPanelBtn.setDisable(bool);
        loadZipBtn.setDisable(bool);
        loadFromDbBtn.setDisable(bool);
        createCampaignBtn.setDisable(bool);
    }
}