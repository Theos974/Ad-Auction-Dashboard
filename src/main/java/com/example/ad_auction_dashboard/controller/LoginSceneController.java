package com.example.ad_auction_dashboard.controller;

import com.example.ad_auction_dashboard.Multimedia;
import com.example.ad_auction_dashboard.logic.UserDatabase;
import com.example.ad_auction_dashboard.logic.UserSession;
import com.example.ad_auction_dashboard.viewer.RegisterScene;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class LoginSceneController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginBtn;

    @FXML
    private Button registerBtn;

    @FXML
    private Text statusText;

    @FXML
    private ToggleButton colourSwitch;

    private String currentStyle;
    private UserDatabase.User user;

    // Initialize method for any setup
    @FXML
    public void initialize() {
        // Clear any previous status messages
        statusText.setText("");
        Multimedia.playMusic("menu.mp3");

        Circle thumb = new Circle(12);
        thumb.getStyleClass().add("thumb");
        colourSwitch.setGraphic(thumb);

        if (UserSession.getInstance().getCurrentStyle() == null){
            currentStyle = this.getClass().getClassLoader().getResource("styles/style.css").toString();
        } else {
            currentStyle = UserSession.getInstance().getCurrentStyle();
        }
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
       // Basic validation
        if (username.isEmpty() || password.isEmpty()) {
            statusText.setText("Please enter both username and password");
            return;
        }

        // Authenticate user with database
        boolean isAuthenticated = UserDatabase.authenticateUser(username, password);

        if (isAuthenticated) {
            // Get user info and create session
            UserDatabase.User user = UserDatabase.getUser(username);
            UserSession.getInstance().setUser(user);

            UserSession.getInstance().setCurrentStyle(currentStyle);

            // Show welcome message
            loginBtn.setDisable(true); // Prevent multiple clicks
            statusText.setFill(javafx.scene.paint.Color.GREEN);
            statusText.setText("Welcome, " + username + "! Logging you in...");

            // Create a delay with animation
            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.5));
            delay.setOnFinished(e -> {
                try {
                    // Navigate to main screen
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/ad_auction_dashboard/fxml/StartScene.fxml"));
                    Parent root = loader.load();
                    Stage stage = (Stage) loginBtn.getScene().getWindow();
                    Scene scene = new Scene(root);
                    scene.getStylesheets().add(currentStyle);
                    stage.setScene(scene);
                    stage.show();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    statusText.setFill(javafx.scene.paint.Color.RED);
                    statusText.setText("Error navigating to main page");
                    loginBtn.setDisable(false); // Re-enable button
                }
            });
            delay.play();
        } else {
            // Show error for incorrect credentials
            statusText.setFill(javafx.scene.paint.Color.RED);
            statusText.setText("Incorrect username or password");

            // Subtle shake animation for feedback
            javafx.animation.TranslateTransition shake = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(100), loginBtn);
            shake.setFromX(0);
            shake.setByX(10);
            shake.setCycleCount(4);
            shake.setAutoReverse(true);
            shake.play();
        }
    }


    @FXML
    private void handleRegister(ActionEvent event) {
        UserSession.getInstance().setCurrentStyle(this.currentStyle);
        try {
            // Create a new RegisterScene using the current stage
            Stage stage = (Stage) registerBtn.getScene().getWindow();
            new RegisterScene(stage, 930, 692, this.currentStyle);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            statusText.setText("Error navigating to registration page");
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
}