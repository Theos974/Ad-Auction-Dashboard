package com.example.ad_auction_dashboard.logic;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Dialog for selecting a campaign to compare with
 */
public class CampaignComparisonDialog {

    /**
     * Show dialog to select a campaign for comparison
     *
     * @param owner The owner window
     * @param currentCampaignName The name of the current campaign (to exclude from selection)
     * @return The selected campaign info, or null if canceled
     */
    public static CampaignDatabase.CampaignInfo showDialog(Stage owner, String currentCampaignName) {
        // Initialize database if needed
        if (!CampaignDatabase.ensureDatabaseInitialized()) {
            showErrorDialog(owner, "Database Error",
                "Could not initialize the database. Please try again later.");
            return null;
        }

        // Create dialog
        javafx.scene.control.Dialog<CampaignDatabase.CampaignInfo> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Compare Campaign");
        dialog.setHeaderText("Select a campaign to compare with");
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);

        // Set buttons
        ButtonType selectButtonType = new ButtonType("Compare", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(selectButtonType, ButtonType.CANCEL);

        // Create grid for UI elements
        VBox content = new VBox(10);
        content.setPadding(new Insets(20, 20, 10, 20));

        // Add information about current campaign
        content.getChildren().add(new Label("Current Campaign: " + currentCampaignName));
        content.getChildren().add(new Separator());

        Label selectLabel = new Label("Select Campaign to Compare:");
        selectLabel.setPadding(new Insets(10, 0, 5, 0));
        content.getChildren().add(selectLabel);

        // Create campaign ListView
        ListView<CampaignDatabase.CampaignInfo> campaignListView = new ListView<>();

        try {
            // Get accessible campaigns for current user
            UserSession session = UserSession.getInstance();
            int userId = session.getUser() != null ? session.getUser().getId() : 0;

            List<CampaignDatabase.CampaignInfo> accessibleCampaigns =
                CampaignDatabase.getAccessibleCampaigns(userId);

            // Filter out the current campaign
            accessibleCampaigns.removeIf(c -> c.getCampaignName().equals(currentCampaignName));

            // Set cell factory to display campaign details nicely
            campaignListView.setCellFactory(param -> new ListCell<CampaignDatabase.CampaignInfo>() {
                @Override
                protected void updateItem(CampaignDatabase.CampaignInfo item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setText(null);
                    } else {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                        String dateRange = "";

                        if (item.getStartDate() != null && item.getEndDate() != null) {
                            dateRange = " (" + item.getStartDate().format(formatter) +
                                " to " + item.getEndDate().format(formatter) + ")";
                        }

                        setText(item.getCampaignName() + dateRange);
                    }
                }
            });

            campaignListView.getItems().addAll(accessibleCampaigns);
            campaignListView.setPrefHeight(200);

            if (!accessibleCampaigns.isEmpty()) {
                campaignListView.getSelectionModel().select(0);
            }

            content.getChildren().add(campaignListView);

            // Disable compare button if no campaigns are available
            Button compareButton = (Button) dialog.getDialogPane().lookupButton(selectButtonType);
            compareButton.setDisable(accessibleCampaigns.isEmpty());
        } catch (Exception e) {
            content.getChildren().add(new Label("Error loading campaigns: " + e.getMessage()));
            Button compareButton = (Button) dialog.getDialogPane().lookupButton(selectButtonType);
            compareButton.setDisable(true);
            e.printStackTrace();
        }

        dialog.getDialogPane().setContent(content);

        // Convert the result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == selectButtonType) {
                return campaignListView.getSelectionModel().getSelectedItem();
            }
            return null;
        });

        // Show dialog and return result
        Optional<CampaignDatabase.CampaignInfo> result = dialog.showAndWait();
        return result.orElse(null);
    }

    /**
     * Displays an error dialog
     */
    private static void showErrorDialog(Stage owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(owner);
        alert.showAndWait();
    }
}