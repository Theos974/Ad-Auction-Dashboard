package com.example.ad_auction_dashboard.controller;

import com.example.ad_auction_dashboard.logic.CampaignMetrics;
import com.example.ad_auction_dashboard.logic.LogoutHandler;
import com.example.ad_auction_dashboard.logic.UserDatabase;
import com.example.ad_auction_dashboard.logic.UserSession;
import com.example.ad_auction_dashboard.viewer.StartScene;
import java.io.IOException;
import java.sql.SQLOutput;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class AdminPanelController {
    @FXML
    private TableView<UserDatabase.User> userTable;

    @FXML
    private TableColumn<UserDatabase.User, Integer> idColumn;

    @FXML
    private TableColumn<UserDatabase.User, String> usernameColumn;

    @FXML
    private TableColumn<UserDatabase.User, String> emailColumn;

    @FXML
    private TableColumn<UserDatabase.User, String> phoneColumn;

    @FXML
    private TableColumn<UserDatabase.User, String> roleColumn;
    @FXML
    private TableColumn<UserDatabase.User, String> rolePassword;

    @FXML
    private Label selectedUserLabel;

    @FXML
    private ComboBox<String> roleComboBox;

    @FXML
    private TextField newUsernameField;

    @FXML
    private TextField newEmailField;

    @FXML
    private TextField newPhoneField;

    @FXML
    private TextField newPasswordField;

    @FXML
    private ComboBox<String> newRoleComboBox;

    @FXML
    private Text statusText;

    @FXML
    private Button backBtn;

    @FXML
    private Button logoutBtn;

    @FXML
    public void initialize() {
        // Check if current user is admin
        if (!UserSession.getInstance().isAdmin()) {
            statusText.setText("You don't have permission to access this page.");
            // Disable all controls
            userTable.setDisable(true);
            roleComboBox.setDisable(true);
            newUsernameField.setDisable(true);
            newEmailField.setDisable(true);
            newPhoneField.setDisable(true);
            newPasswordField.setDisable(true);
            newRoleComboBox.setDisable(true);
            return;
        }

        // Set up role combo boxes
        String[] roles = {"admin", "editor", "viewer"};
        roleComboBox.setItems(FXCollections.observableArrayList(roles));
        roleComboBox.setValue("viewer"); // Default

        newRoleComboBox.setItems(FXCollections.observableArrayList(roles));
        newRoleComboBox.setValue("viewer"); // Default

        // Set up table columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        rolePassword.setCellValueFactory(new PropertyValueFactory<>("password"));


        // Load all users into table
        refreshUserTable();

        // Add selection listener
        userTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedUserLabel.setText(newVal.getUsername());
                roleComboBox.setValue(newVal.getRole());
            } else {
                selectedUserLabel.setText("None");
            }
        });
    }

    private void refreshUserTable() {
        userTable.getItems().setAll(UserDatabase.getAllUsers());
    }

    @FXML
    private void handleChangeRole(ActionEvent event) {
        UserDatabase.User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser != null) {
            String newRole = roleComboBox.getValue();

            // Don't allow changing your own role
            if (selectedUser.getUsername().equals(UserSession.getInstance().getUser().getUsername())) {
                statusText.setText("You cannot change your own role.");
                return;
            }

            // Update role in database
            boolean success = UserDatabase.changeUserRole(selectedUser.getUsername(), newRole);
            if (success) {
                statusText.setText("Role updated successfully.");
                refreshUserTable();
            } else {
                statusText.setText("Failed to update role.");
            }
        } else {
            statusText.setText("Please select a user first.");
        }
    }


    @FXML
    private void handleAddUser(ActionEvent event) {
        String username = newUsernameField.getText().trim();
        String email = newEmailField.getText().trim();
        String phone = newPhoneField.getText().trim();
        String password = newPasswordField.getText();
        String role = newRoleComboBox.getValue();

        // Basic validation
        if (username.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            statusText.setText("All fields are required.");
            return;
        }

        try {
            // Add user to database
            UserDatabase.addUser(username, email, phone, password, role);
            statusText.setText("User added successfully.");

            // Clear form
            newUsernameField.clear();
            newEmailField.clear();
            newPhoneField.clear();
            newPasswordField.clear();

            // Refresh table
            refreshUserTable();
        } catch (Exception e) {
            statusText.setText("Error adding user: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Stage stage = (Stage) backBtn.getScene().getWindow();

            // Determine previous scene and navigate accordingly
            if (UserSession.getInstance().getPreviousScene() != null) {
                switch (UserSession.getInstance().getPreviousScene()) {
                    case "StartScene":
                        new StartScene(stage, 930, 692);
                        break;
                    case "MetricScene":
                        // Reload Metric Scene with preserved metrics
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/ad_auction_dashboard/fxml/MetricScene2.fxml"));
                        Parent root = loader.load();
                        MetricSceneController controller = loader.getController();

                        // Retrieve the last used metrics from UserSession or some static storage
                        CampaignMetrics metrics = UserSession.getInstance().getCurrentCampaignMetrics();
                        if (metrics != null) {
                            controller.setMetrics(metrics);
                        } else {
                            // Fallback error handling
                            System.out.println("No metrics found to restore");
                        }

                        Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
                        stage.setScene(scene);
                        break;
                    default:
                        // Fallback to Start Scene if previous scene is not recognized
                        new StartScene(stage, 930, 692);
                }
            } else {
                // Default fallback
                new StartScene(stage, 930, 692);
            }
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error navigating back");
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        LogoutHandler.handleLogout(event);
    }
}