package com.example.ad_auction_dashboard.controller;

import com.example.ad_auction_dashboard.logic.UserDatabase;
import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
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

    // Initialize method for any setup
    @FXML
    public void initialize() {
        // Clear any previous status messages
        statusText.setText("");
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
            // Get the user's role
            String role = UserDatabase.getUserRole(username);

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/ad_auction_dashboard/fxml/StartScene.fxml"));
                Parent root = loader.load();

                // Get the controller and pass the user info
                StartSceneController controller = loader.getController();

                // Pass user info to the controller if method exists
                if (controller != null) {
                    // Store current user information in the controller
                    try {
                        // Using reflection to check if the method exists
                        controller.getClass().getMethod("setUserInfo", String.class, String.class)
                            .invoke(controller, username, role);
                    } catch (NoSuchMethodException e) {
                        // Method doesn't exist, which is fine - just log it
                        System.out.println("Note: setUserInfo method not found in StartSceneController");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // Display a welcome message based on role
                String welcomeMessage = "admin".equals(role) ?
                    "Welcome, Administrator!" : "Welcome to Ad Auction Dashboard!";
                statusText.setText(welcomeMessage);

                // Add a small delay before transition for the message to be visible
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        javafx.application.Platform.runLater(() -> {
                            // Navigate to main scene
                            Stage stage = (Stage) loginBtn.getScene().getWindow();
                            Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
                            stage.setScene(scene);
                            stage.show();
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();

            } catch (IOException e) {
                e.printStackTrace();
                statusText.setText("Error navigating to main page");
            }
        } else {
            statusText.setText("Invalid username or password");
        }
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        // Navigate to register page
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/ad_auction_dashboard/fxml/RegisterScene.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) registerBtn.getScene().getWindow();
            Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            statusText.setText("Error navigating to registration page");
        }
    }
}