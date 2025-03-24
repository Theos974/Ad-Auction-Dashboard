package com.example.ad_auction_dashboard.logic;

import java.util.ArrayList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
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
        // Create dialog
        Dialog<CampaignDatabase.CampaignInfo> dialog = new Dialog<>();
        dialog.setTitle("Compare Campaign");
        dialog.setHeaderText("Select a campaign to compare with");
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);

        // Set buttons
        ButtonType selectButtonType = new ButtonType("Compare", ButtonBar.ButtonData.OK_DONE);
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

        // Get accessible campaigns for current user
        UserSession session = UserSession.getInstance();
        int userId = session.getUser() != null ? session.getUser().getId() : 0;

        // Use try-with-resources to ensure connections get closed
        List<CampaignDatabase.CampaignInfo> accessibleCampaigns = new ArrayList<>();
        try {
            accessibleCampaigns = CampaignDatabase.getAccessibleCampaigns(userId);
        } catch (Exception e) {
            System.err.println("Error loading campaigns: " + e.getMessage());
        }

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
}