package com.example.ad_auction_dashboard.logic;

import java.util.concurrent.ExecutionException;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Dialog to load a campaign from the database
 */
public class LoadCampaignDialog {

    /**
     * Show dialog to choose a campaign to load
     *
     * @param owner The owner window
     * @return The loaded campaign, or null if canceled or an error occurred
     */
    public static Campaign showDialog(Stage owner) {
        // Create dialog
        Dialog<CampaignDatabase.CampaignInfo> dialog = new Dialog<>();
        dialog.setTitle("Load Campaign");
        dialog.setHeaderText("Select a campaign to load");
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);

        // Set buttons
        ButtonType loadButtonType = new ButtonType("Load", ButtonBar.ButtonData.OK_DONE);
        ButtonType deleteButtonType = new ButtonType("Delete Selected", ButtonBar.ButtonData.OTHER);
        dialog.getDialogPane().getButtonTypes().addAll(loadButtonType, deleteButtonType, ButtonType.CANCEL);

        // Create layout
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20, 20, 10, 20));

        // Get current user ID and role
        UserSession session = UserSession.getInstance();
        int userId = session.getUser() != null ? session.getUser().getId() : 0;
        boolean isAdmin = session.isAdmin();
        boolean isEditor = session.isEditor();

        // Load campaigns based on user role
        List<CampaignDatabase.CampaignInfo> campaignList;
        if (isAdmin) {
            // Admins can see all campaigns
            campaignList = CampaignDatabase.getAllCampaigns();
        } else if (isEditor) {
            // Editors can see campaigns they created plus those assigned to them
            campaignList = CampaignDatabase.getAccessibleCampaigns(userId);
        } else {
            // Viewers can only see campaigns assigned to them
            campaignList = CampaignDatabase.getAccessibleCampaigns(userId);
        }

        // Create ListView for campaigns
        ListView<CampaignDatabase.CampaignInfo> campaignListView = new ListView<>();
        campaignListView.setItems(FXCollections.observableArrayList(campaignList));
        campaignListView.setCellFactory(param -> new ListCell<CampaignDatabase.CampaignInfo>() {
            @Override
            protected void updateItem(CampaignDatabase.CampaignInfo item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    // Format dates
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    String dateRangeStr = "";

                    if (item.getStartDate() != null && item.getEndDate() != null) {
                        dateRangeStr = " (" + item.getStartDate().format(formatter) +
                            " to " + item.getEndDate().format(formatter) + ")";
                    }

                    setText(item.getCampaignName() + dateRangeStr);
                }
            }
        });

        // Add campaign details area
        TextArea detailsArea = new TextArea();
        detailsArea.setEditable(false);
        detailsArea.setPrefRowCount(5);
        detailsArea.setPromptText("Select a campaign to view details");

        // Add listener for selection changes
        campaignListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                StringBuilder details = new StringBuilder();
                details.append("Campaign Name: ").append(newVal.getCampaignName()).append("\n");
                details.append("Campaign ID: ").append(newVal.getCampaignId()).append("\n");

                if (newVal.getCreationDate() != null) {
                    details.append("Created: ").append(newVal.getCreationDate().format(formatter)).append("\n");
                }

                if (newVal.getStartDate() != null && newVal.getEndDate() != null) {
                    details.append("Date Range: ").append(newVal.getStartDate().format(formatter))
                        .append(" to ").append(newVal.getEndDate().format(formatter)).append("\n");
                }

                List<UserDatabase.User> allUsers = UserDatabase.getAllUsers();
                for (UserDatabase.User user : allUsers) {
                    if (user.getId() == newVal.getUserId()) {
                        details.append("Created by: ").append(user.getUsername()).append(" (").append(user.getRole()).append(")");
                        break;
                    }
                }


                detailsArea.setText(details.toString());
            } else {
                detailsArea.setText("");
            }
        });

        // Create filter combo box for admins and editors
        ComboBox<String> filterComboBox = null;
        if (isAdmin || isEditor) {
            filterComboBox = new ComboBox<>();
            filterComboBox.getItems().add("All Accessible Campaigns");

            if (isAdmin) {
                filterComboBox.getItems().add("All Campaigns");
            }

            if (isEditor || isAdmin) {
                filterComboBox.getItems().add("My Campaigns");
            }

            filterComboBox.setValue("All Accessible Campaigns");

            ComboBox<String> finalFilterComboBox = filterComboBox; // Final copy for use in lambda
            filterComboBox.setOnAction(e -> {
                if (finalFilterComboBox.getValue().equals("All Campaigns") && isAdmin) {
                    campaignListView.setItems(FXCollections.observableArrayList(
                        CampaignDatabase.getAllCampaigns()));
                } else if (finalFilterComboBox.getValue().equals("My Campaigns")) {
                    campaignListView.setItems(FXCollections.observableArrayList(
                        CampaignDatabase.getUserCampaigns(userId)));
                } else { // "All Accessible Campaigns"
                    campaignListView.setItems(FXCollections.observableArrayList(
                        CampaignDatabase.getAccessibleCampaigns(userId)));
                }
            });

            vbox.getChildren().add(filterComboBox);
        }

        // Add label for empty list
        if (campaignList.isEmpty()) {
            Label emptyLabel = new Label("No saved campaigns found.");
            vbox.getChildren().add(emptyLabel);
        } else {
            vbox.getChildren().addAll(
                new Label("Select a campaign to load:"),
                campaignListView,
                new Label("Campaign Details:"),
                detailsArea
            );
        }

        dialog.getDialogPane().setContent(vbox);

        // Capture filter combo box state for use in button handler
        final ComboBox<String> finalFilterComboBox = filterComboBox;

        // Handle delete button action
        Button deleteButton = (Button) dialog.getDialogPane().lookupButton(deleteButtonType);

        // Only allow admins and owners to delete campaigns
        if (!isAdmin) {
            deleteButton.setDisable(true);
        }

        deleteButton.setOnAction(event -> {
            CampaignDatabase.CampaignInfo selectedCampaign =
                campaignListView.getSelectionModel().getSelectedItem();

            if (selectedCampaign != null) {
                // Only admins or campaign creators can delete campaigns
                if (!isAdmin && selectedCampaign.getUserId() != userId) {
                    showInfoDialog("Permission Denied",
                        "You don't have permission to delete this campaign. Only admins or the campaign creator can delete campaigns.");
                    event.consume();
                    return;
                }

                boolean confirmed = showConfirmationDialog(
                    "Delete Campaign",
                    "Are you sure you want to delete campaign \"" +
                        selectedCampaign.getCampaignName() + "\"?",
                    "This action cannot be undone."
                );

                if (confirmed) {
                    boolean deleted = CampaignDatabase.deleteCampaign(selectedCampaign.getCampaignId());

                    if (deleted) {
                        // Refresh the list - check if filter is active
                        if (isAdmin && finalFilterComboBox != null &&
                            finalFilterComboBox.getValue().equals("All Campaigns")) {
                            campaignListView.setItems(FXCollections.observableArrayList(
                                CampaignDatabase.getAllCampaigns()));
                        } else if (finalFilterComboBox != null &&
                            finalFilterComboBox.getValue().equals("My Campaigns")) {
                            campaignListView.setItems(FXCollections.observableArrayList(
                                CampaignDatabase.getUserCampaigns(userId)));
                        } else {
                            campaignListView.setItems(FXCollections.observableArrayList(
                                CampaignDatabase.getAccessibleCampaigns(userId)));
                        }

                        showInfoDialog("Campaign Deleted",
                            "Campaign \"" + selectedCampaign.getCampaignName() + "\" was deleted successfully.");
                    } else {
                        showErrorDialog("Failed to delete campaign. Please try again.");
                    }
                }
            } else {
                showErrorDialog("Please select a campaign to delete.");
            }

            // Prevent dialog from closing
            event.consume();
        });

        // Enable/disable load and delete buttons based on selection
        Button loadButton = (Button) dialog.getDialogPane().lookupButton(loadButtonType);
        loadButton.setDisable(campaignList.isEmpty());
        deleteButton.setDisable(campaignList.isEmpty() || !isAdmin);

        // Set result converter
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loadButtonType) {
                return campaignListView.getSelectionModel().getSelectedItem();
            }
            return null;
        });

        // Show dialog and handle result
        Optional<CampaignDatabase.CampaignInfo> result = dialog.showAndWait();

        if (result.isPresent()) {
            CampaignDatabase.CampaignInfo selectedCampaign = result.get();

            try {
                // Check access permissions
                if (isAdmin ||
                    selectedCampaign.getUserId() == userId ||
                    CampaignDatabase.canUserAccessCampaign(userId, selectedCampaign.getCampaignId())) {

                    // Load campaign from database
                    Campaign campaign = CampaignDatabase.loadCampaign(selectedCampaign.getCampaignId());

                    if (campaign != null) {
                        return campaign;
                    } else {
                        showErrorDialog("Failed to load campaign data. Please try again.");
                    }
                } else {
                    showErrorDialog("You don't have permission to access this campaign.");
                }
            } catch (Exception e) {
                showErrorDialog("Error loading campaign: " + e.getMessage());
                e.printStackTrace();
            }
        }

        if (result.isPresent()) {
            CampaignDatabase.CampaignInfo selectedCampaign = result.get();

            // Show loading dialog
            Dialog<Campaign> loadingDialog = new Dialog<>();
            loadingDialog.setTitle("Loading Campaign");
            loadingDialog.setHeaderText("Loading campaign data...");
            loadingDialog.initOwner(owner);
            loadingDialog.initModality(Modality.WINDOW_MODAL);

            // Add progress indicator
            ProgressIndicator progress = new ProgressIndicator();
            progress.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);

            Label statusLabel = new Label("Loading campaign. Please wait...");

            VBox content = new VBox(10, progress, statusLabel);
            content.setAlignment(Pos.CENTER);
            content.setPadding(new Insets(20));

            loadingDialog.getDialogPane().setContent(content);
            loadingDialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

            // Create loading task
            Task<Campaign> loadTask = new Task<Campaign>() {
                @Override
                protected Campaign call() throws Exception {
                    updateMessage("Checking campaign...");

                    // Load campaign in background
                    Campaign campaign = CampaignDatabase.loadCampaign(selectedCampaign.getCampaignId());

                    updateMessage("Campaign loaded successfully!");
                    return campaign;
                }
            };

            // Bind status updates
            statusLabel.textProperty().bind(loadTask.messageProperty());

            // Start task
            Thread loadThread = new Thread(loadTask);
            loadThread.setDaemon(true);
            loadThread.start();

            // Show dialog and wait for result or cancellation
            loadingDialog.setResultConverter(dialogButton -> {
                if (dialogButton == ButtonType.CANCEL) {
                    loadTask.cancel();
                    return null;
                }
                return null;
            });

            // Start showing dialog (non-blocking)
            loadingDialog.show();

            // Wait for task to complete
            try {
                Campaign campaign = loadTask.get();
                loadingDialog.close();
                return campaign;
            } catch (InterruptedException | ExecutionException e) {
                loadingDialog.close();
                showErrorDialog("Error loading campaign: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }

        return null;
    }

    private static boolean showConfirmationDialog(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private static void showErrorDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static void showInfoDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}