package com.example.ad_auction_dashboard.controller;

import com.example.ad_auction_dashboard.logic.CampaignDatabase;
import com.example.ad_auction_dashboard.logic.CampaignMetrics;
import com.example.ad_auction_dashboard.logic.LogoutHandler;
import com.example.ad_auction_dashboard.logic.UserDatabase;
import com.example.ad_auction_dashboard.logic.UserSession;
import com.example.ad_auction_dashboard.viewer.StartScene;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class AdminPanelController {
    // User Management Tab Fields
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

    // Campaign Assignment Tab Fields
    @FXML
    private TableView<CampaignDatabase.CampaignInfo> campaignTable;

    @FXML
    private TableColumn<CampaignDatabase.CampaignInfo, Integer> campaignIdColumn;

    @FXML
    private TableColumn<CampaignDatabase.CampaignInfo, String> campaignNameColumn;

    @FXML
    private TableColumn<CampaignDatabase.CampaignInfo, String> campaignDateColumn;

    @FXML
    private TableView<UserAccessItem> assignmentTable;

    @FXML
    private TableColumn<UserAccessItem, String> assignmentUserColumn;

    @FXML
    private TableColumn<UserAccessItem, String> assignmentRoleColumn;

    @FXML
    private TableColumn<UserAccessItem, Boolean> assignmentAccessColumn;

    @FXML
    private Button refreshCampaignsBtn;

    @FXML
    private Button grantAccessBtn;

    @FXML
    private Button revokeAccessBtn;

    @FXML
    private Text campaignStatusText;

    @FXML
    private Button backBtn;

    @FXML
    private Button logoutBtn;

    // State variables
    private CampaignDatabase.CampaignInfo selectedCampaign;
    private UserAccessItem selectedUserAccess;

    @FXML
    public void initialize() {
        // Check if current user is admin
        if (!UserSession.getInstance().isAdmin()) {
            showErrorMessage("You don't have permission to access this page.");
            disableAllControls();
            return;
        }

        // Initialize User Management Tab
        initializeUserManagementTab();

        // Initialize Campaign Assignment Tab
        initializeCampaignAssignmentTab();

        // Initial button state
        updateAccessButtons();
    }

    private void initializeUserManagementTab() {
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

    private void initializeCampaignAssignmentTab() {
        // Set up campaign table columns
        if (campaignTable != null) {
            campaignIdColumn.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getCampaignId()).asObject());

            campaignNameColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCampaignName()));

            campaignDateColumn.setCellValueFactory(data -> {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                StringBuilder dateRange = new StringBuilder();

                if (data.getValue().getStartDate() != null) {
                    dateRange.append(data.getValue().getStartDate().format(formatter));
                } else {
                    dateRange.append("Unknown");
                }

                dateRange.append(" to ");

                if (data.getValue().getEndDate() != null) {
                    dateRange.append(data.getValue().getEndDate().format(formatter));
                } else {
                    dateRange.append("Unknown");
                }

                return new SimpleStringProperty(dateRange.toString());
            });

            // Add selection listener for campaign table
            campaignTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                selectedCampaign = newVal;
                if (newVal != null) {
                    loadUserAssignments(newVal);
                    showCampaignStatus("Selected campaign: " + newVal.getCampaignName());
                } else {
                    clearUserAssignments();
                    showCampaignStatus("");
                }
                updateAccessButtons();
            });

            // Load campaigns
            refreshCampaignList();
        }

        // Set up assignment table columns
        if (assignmentTable != null) {
            assignmentUserColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getUsername()));

            assignmentRoleColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getRole()));

            assignmentAccessColumn.setCellValueFactory(data ->
                new SimpleBooleanProperty(data.getValue().getHasAccess()));

            assignmentAccessColumn.setCellFactory(column -> new CheckBoxTableCell<>());

            // Add selection listener for assignment table
            assignmentTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                selectedUserAccess = newVal;
                updateAccessButtons();
            });
        }
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
                showErrorMessage("You cannot change your own role.");
                return;
            }

            // Update role in database
            boolean success = UserDatabase.changeUserRole(selectedUser.getUsername(), newRole);
            if (success) {
                showSuccessMessage("Role updated successfully.");
                refreshUserTable();

                // If we have a campaign selected, reload user assignments to reflect new role
                if (selectedCampaign != null) {
                    loadUserAssignments(selectedCampaign);
                }
            } else {
                showErrorMessage("Failed to update role.");
            }
        } else {
            showErrorMessage("Please select a user first.");
        }
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        UserDatabase.User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser != null) {
            // Don't allow deleting yourself
            if (selectedUser.getUsername().equals(UserSession.getInstance().getUser().getUsername())) {
                showErrorMessage("You cannot delete your own account.");
                return;
            }

            UserDatabase.deleteUser(selectedUser.getId());
            showSuccessMessage("User deleted successfully");
            refreshUserTable();

            // If we have a campaign selected, reload user assignments to reflect deletion
            if (selectedCampaign != null) {
                loadUserAssignments(selectedCampaign);
            }
        } else {
            showErrorMessage("Please select a user to delete");
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
            showErrorMessage("All fields are required.");
            return;
        }

        try {
            // Add user to database
            UserDatabase.addUser(username, email, phone, password, role);
            showSuccessMessage("User added successfully.");

            // Clear form
            newUsernameField.clear();
            newEmailField.clear();
            newPhoneField.clear();
            newPasswordField.clear();

            // Refresh user table
            refreshUserTable();

            // Reload user assignments if a campaign is selected
            if (selectedCampaign != null) {
                loadUserAssignments(selectedCampaign);
            }
        } catch (Exception e) {
            showErrorMessage("Error adding user: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRefreshCampaigns(ActionEvent event) {
        refreshCampaignList();
        showCampaignStatus("Campaign list refreshed.");
    }

    @FXML
    private void handleGrantAccess(ActionEvent event) {
        if (selectedCampaign == null || selectedUserAccess == null) {
            showCampaignStatus("Please select both a campaign and a user.");
            return;
        }

        int userId = selectedUserAccess.getUser().getId();
        int campaignId = selectedCampaign.getCampaignId();
        int adminId = UserSession.getInstance().getUser().getId(); // Current admin's ID

        if (CampaignDatabase.assignCampaignToUser(campaignId, userId, adminId)) {
            showCampaignStatus("Access granted successfully to " + selectedUserAccess.getUsername());
            selectedUserAccess.setHasAccess(true);
            assignmentTable.refresh();
        } else {
            showCampaignStatus("Failed to grant access.");
        }
    }

    @FXML
    private void handleRevokeAccess(ActionEvent event) {
        if (selectedCampaign == null || selectedUserAccess == null) {
            showCampaignStatus("Please select both a campaign and a user.");
            return;
        }

        int userId = selectedUserAccess.getUser().getId();
        int campaignId = selectedCampaign.getCampaignId();

        if (CampaignDatabase.removeCampaignFromUser(campaignId, userId)) {
            showCampaignStatus("Access revoked successfully from " + selectedUserAccess.getUsername());
            selectedUserAccess.setHasAccess(false);
            assignmentTable.refresh();
        } else {
            showCampaignStatus("Failed to revoke access.");
        }
    }

    /**
     * Refreshes the campaign list from the database
     */
    private void refreshCampaignList() {
        if (campaignTable == null) return;

        List<CampaignDatabase.CampaignInfo> campaigns = CampaignDatabase.getAllCampaigns();
        campaignTable.setItems(FXCollections.observableArrayList(campaigns));

        // Clear selection to avoid stale data
        campaignTable.getSelectionModel().clearSelection();
        selectedCampaign = null;

        // Clear user assignments
        clearUserAssignments();
    }

    /**
     * Loads the user assignments for the selected campaign
     */
    private void loadUserAssignments(CampaignDatabase.CampaignInfo campaign) {
        if (assignmentTable == null) return;

        List<UserAccessItem> userAccesses = new ArrayList<>();
        List<UserDatabase.User> allUsers = UserDatabase.getAllUsers();

        // Get list of user IDs with access to this campaign
        List<Integer> usersWithAccess = CampaignDatabase.getUsersWithAccess(campaign.getCampaignId());

        // For each user, check if they have access to this campaign
        for (UserDatabase.User user : allUsers) {
            boolean hasAccess = usersWithAccess.contains(user.getId());
            userAccesses.add(new UserAccessItem(user, hasAccess));
        }

        assignmentTable.setItems(FXCollections.observableArrayList(userAccesses));
    }

    /**
     * Clears the user assignments table
     */
    private void clearUserAssignments() {
        if (assignmentTable != null) {
            assignmentTable.getItems().clear();
        }
        selectedUserAccess = null;
        updateAccessButtons();
    }

    /**
     * Updates the enabled state of access buttons based on selection
     */
    private void updateAccessButtons() {
        boolean campaignSelected = selectedCampaign != null;
        boolean userAccessSelected = selectedUserAccess != null;

        if (grantAccessBtn != null) {
            grantAccessBtn.setDisable(!campaignSelected || !userAccessSelected ||
                (userAccessSelected && selectedUserAccess.getHasAccess()));
        }

        if (revokeAccessBtn != null) {
            revokeAccessBtn.setDisable(!campaignSelected || !userAccessSelected ||
                (userAccessSelected && !selectedUserAccess.getHasAccess()));
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

    // Helper methods
    private void disableAllControls() {
        // Disable User Management controls
        userTable.setDisable(true);
        roleComboBox.setDisable(true);
        newUsernameField.setDisable(true);
        newEmailField.setDisable(true);
        newPhoneField.setDisable(true);
        newPasswordField.setDisable(true);
        newRoleComboBox.setDisable(true);

        // Disable Campaign Assignment controls
        if (campaignTable != null) campaignTable.setDisable(true);
        if (assignmentTable != null) assignmentTable.setDisable(true);
        if (refreshCampaignsBtn != null) refreshCampaignsBtn.setDisable(true);
        if (grantAccessBtn != null) grantAccessBtn.setDisable(true);
        if (revokeAccessBtn != null) revokeAccessBtn.setDisable(true);
    }

    private void showErrorMessage(String message) {
        if (statusText != null) {
            statusText.setText(message);
        }
    }

    private void showSuccessMessage(String message) {
        if (statusText != null) {
            statusText.setText(message);
        }
    }

    private void showCampaignStatus(String message) {
        if (campaignStatusText != null) {
            campaignStatusText.setText(message);
        }
    }

    // Helper class for user assignment table
    public static class UserAccessItem {
        private final UserDatabase.User user;
        private boolean hasAccess;

        public UserAccessItem(UserDatabase.User user, boolean hasAccess) {
            this.user = user;
            this.hasAccess = hasAccess;
        }

        public UserDatabase.User getUser() { return user; }
        public String getUsername() { return user.getUsername(); }
        public String getRole() { return user.getRole(); }
        public boolean getHasAccess() { return hasAccess; }
        public void setHasAccess(boolean hasAccess) { this.hasAccess = hasAccess; }
    }
}