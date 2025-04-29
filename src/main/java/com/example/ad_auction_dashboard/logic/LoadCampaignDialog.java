package com.example.ad_auction_dashboard.logic;

import com.example.ad_auction_dashboard.controller.StartSceneController;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

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
    public static void showDialog(Stage owner, StartSceneController startSceneController) {
        // Create dialog
        Dialog<CampaignDatabase.CampaignInfo> dialog = new Dialog<>();
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setTitle("Load Campaign");
        dialog.setHeaderText("Select a campaign to load");
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.getDialogPane().getStylesheets().add(UserSession.getInstance().getCurrentStyle());
        dialog.getDialogPane().getStyleClass().add("load-dialog");


        // Set buttons
        ButtonType loadButtonType = new ButtonType("Load", ButtonBar.ButtonData.OK_DONE);
        ButtonType deleteButtonType = new ButtonType("Delete Selected", ButtonBar.ButtonData.OTHER);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(loadButtonType, deleteButtonType, cancelButtonType);

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
        campaignListView.getStyleClass().add("load-list");
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

        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(cancelButtonType);
        cancelButton.setOnAction(event -> {
            startSceneController.toggleControls(false);
            startSceneController.statusText.setText("No Campaign Loaded.");
        });

        // Handle delete button action
        Button deleteButton = (Button) dialog.getDialogPane().lookupButton(deleteButtonType);

        // Only allow admins and owners to delete campaigns
        if (!isAdmin) {
            deleteButton.setDisable(true);
        }

        deleteButton.setOnAction(event -> {
            startSceneController.toggleControls(true);
            CampaignDatabase.CampaignInfo selectedCampaign =
                campaignListView.getSelectionModel().getSelectedItem();

            if (selectedCampaign != null) {
                // Only admins or campaign creators can delete campaigns
                if (!isAdmin && selectedCampaign.getUserId() != userId) {
                    showInfoDialog("Permission Denied",
                        "You don't have permission to delete this campaign. Only admins or the campaign creator can delete campaigns.");
                    event.consume();
                    Platform.runLater(() -> startSceneController.toggleControls(false));
                    System.out.println("206");
                    return;
                }

                boolean confirmed = showConfirmationDialog(
                    "Delete Campaign",
                    "Are you sure you want to delete campaign \"" +
                        selectedCampaign.getCampaignName() + "\"?",
                    "This action cannot be undone."
                );

                if (confirmed) {
                    startSceneController.toggleControls(true);
                    startSceneController.statusText.setText("Deleting Campaign");
                    startSceneController.playDeleteAnimation();
                    new Thread(() -> {

                        boolean deleted = (CampaignDatabase.deleteCampaign(selectedCampaign.getCampaignId()));
                        Platform.runLater(() -> {
                            startSceneController.stopDeleteAnimation();
                            startSceneController.toggleControls(false);
                            System.out.println("227");
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
                                startSceneController.statusText.setText("Campaign Deleted");
                            } else {
                                showErrorDialog("Failed to delete campaign. Please try again.");
                                startSceneController.statusText.setText("Campaign Deletion Error!");
                            }
                        });
                        return;
                    }).start();
                    return;
                } else {
                    startSceneController.toggleControls(false);
                }
            } else {
                startSceneController.toggleControls(false);
                showErrorDialog("Please select a campaign to delete.");
                return;
            }


            return;
            //event.consume();
            //return;
        });

        // Enable/disable load and delete buttons based on selection
        Button loadButton = (Button) dialog.getDialogPane().lookupButton(loadButtonType);
        loadButton.setDisable(campaignList.isEmpty());
        deleteButton.setDisable(campaignList.isEmpty() || !isAdmin);

        // Set result converter
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loadButtonType) {
                if (campaignListView.getSelectionModel().getSelectedItem() == null){
                    return new CampaignDatabase.CampaignInfo(-1,null,null,null,null,-1);
                } else{
                    return campaignListView.getSelectionModel().getSelectedItem();
                }
            } else if (dialogButton == deleteButtonType || dialogButton == cancelButtonType){
                return null;
            }
            return null;
        });

        // Show dialog and handle result
        Optional<CampaignDatabase.CampaignInfo> result = dialog.showAndWait();
        System.out.println("RES: " + result);
        if (result.isPresent() && result.get().getCampaignId() == -1 && result.get().getCampaignName() == null){
            Platform.runLater(() -> {startSceneController.toggleControls(false);startSceneController.statusText.setText("No Campaign Loaded");});
            showErrorDialog("No Campaign Selected");
        }else if (result.isPresent()) {
            CampaignDatabase.CampaignInfo selectedCampaign = result.get();

            try {
                // Check access permissions
                if (isAdmin ||
                    selectedCampaign.getUserId() == userId ||
                    CampaignDatabase.canUserAccessCampaign(userId, selectedCampaign.getCampaignId())) {
                    startSceneController.startLoadAnimation();
                    new Thread(() -> {
                        Campaign campaign = CampaignDatabase.loadCampaign(selectedCampaign.getCampaignId(), startSceneController);
                    }).start();
                    return;
                } else {
                    Platform.runLater(() -> startSceneController.toggleControls(false));
                    showErrorDialog("You don't have permission to access this campaign.");
                }
            } catch (Exception e) {
                Platform.runLater(() -> startSceneController.toggleControls(false));
                showErrorDialog("Error loading campaign: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static boolean showConfirmationDialog(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.getDialogPane().getStylesheets().add(UserSession.getInstance().getCurrentStyle());
        alert.getDialogPane().getStyleClass().add("alert");
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private static void showErrorDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.getDialogPane().getStylesheets().add(UserSession.getInstance().getCurrentStyle());
        alert.getDialogPane().getStyleClass().add("alert");
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static void showInfoDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.getDialogPane().getStylesheets().add(UserSession.getInstance().getCurrentStyle());
        alert.getDialogPane().getStyleClass().add("alert");
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}