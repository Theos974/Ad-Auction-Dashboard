package com.example.ad_auction_dashboard.controller;

import com.example.ad_auction_dashboard.logic.UserDatabase;
import com.example.ad_auction_dashboard.viewer.LoginScene;
import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class RegisterSceneController {

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField phoneField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private ComboBox<String> roleComboBox;

    @FXML
    private Button registerBtn;

    @FXML
    private Button loginBtn;

    @FXML
    private Text statusText;

    @FXML
    public void initialize() {
        // Initialize the role dropdown - only "user" available during registration
        roleComboBox.getItems().add("Viewer");
        roleComboBox.setValue("Viewer");
        roleComboBox.setDisable(true); // Disable role selection - regular users can only sign up as "user"

        // Clear any previous status messages
        statusText.setText("");
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String role = roleComboBox.getValue();

        // Basic validation
        if (username.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            statusText.setText("Please fill in all fields");
            return;
        }

        // Validate username format (alphanumeric)
        if (!username.matches("^[a-zA-Z0-9_]{3,20}$")) {
            statusText.setText("Username must be 3-20 characters, alphanumeric with underscores only");
            return;
        }

        // Validate email format (simple check)
        if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            statusText.setText("Please enter a valid email address");
            return;
        }

        // Validate phone format (simple check)
        if (!phone.matches("^[0-9]{10,15}$")) {
            statusText.setText("Please enter a valid phone number (10-15 digits)");
            return;
        }

        // Check if passwords match
        if (!password.equals(confirmPassword)) {
            statusText.setText("Passwords do not match");
            return;
        }

        try {
            // Attempt to add the user to the database
            System.out.println(UserDatabase.isUserUnique(username,email,phone));
            if (!UserDatabase.isUserUnique(username,email,phone)){
                statusText.setText("User is not unique/already in database");
                return;
            }
            UserDatabase.addUser(username, email, phone, password, role);

            // Show success message and navigate to login
            statusText.setText("Registration successful! Redirecting to login...");

            // Slight delay before redirecting
            new Thread(() -> {
                try {
                    Thread.sleep(1500);

                    // Use JavaFX thread to update UI
                    javafx.application.Platform.runLater(() -> {
                        try {
                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/ad_auction_dashboard/fxml/LoginScene.fxml"));
                            Parent root = loader.load();

                            Stage stage = (Stage) registerBtn.getScene().getWindow();
                            Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
                            stage.setScene(scene);
                            stage.show();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            statusText.setText("Error during registration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBackToLogin(ActionEvent event) throws IOException {
        // Create a new LoginScene using the current stage
        Stage stage = (Stage) loginBtn.getScene().getWindow();
        new LoginScene(stage, 930, 692);
        stage.show();
    }
}