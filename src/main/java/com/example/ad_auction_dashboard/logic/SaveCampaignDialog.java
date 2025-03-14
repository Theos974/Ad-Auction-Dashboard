package com.example.ad_auction_dashboard.logic;

import com.example.ad_auction_dashboard.logic.CampaignDatabase;
import com.example.ad_auction_dashboard.logic.CampaignMetrics;
import com.example.ad_auction_dashboard.logic.UserSession;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.util.Optional;

/**
 * Dialog to save campaign to database
 */
public class SaveCampaignDialog {

    /**
     * Show dialog to save campaign to database
     *
     * @param owner The owner window
     * @param campaignMetrics The campaign metrics to save
     * @return true if campaign was saved successfully, false otherwise
     */
    public static boolean showDialog(Stage owner, CampaignMetrics campaignMetrics) {
        // Create dialog
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Save Campaign");
        dialog.setHeaderText("Save your campaign to the database for future use");
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);

        // Set buttons
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create grid for input fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Campaign name field
        TextField campaignNameField = new TextField();
        campaignNameField.setPromptText("Enter Campaign Name");

        // Description field (optional)
        TextArea descriptionField = new TextArea();
        descriptionField.setPromptText("Enter Description (Optional)");
        descriptionField.setPrefRowCount(3);

        grid.add(new Label("Campaign Name:"), 0, 0);
        grid.add(campaignNameField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descriptionField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        // Request focus on the campaign name field
        campaignNameField.requestFocus();

        // Convert the result to a name-description pair when the save button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new Pair<>(campaignNameField.getText(), descriptionField.getText());
            }
            return null;
        });

        // Show dialog and wait for response
        Optional<Pair<String, String>> result = dialog.showAndWait();

        // Process result
        if (result.isPresent()) {
            Pair<String, String> nameDescription = result.get();
            String campaignName = nameDescription.getKey();

            // Validate campaign name
            if (campaignName == null || campaignName.trim().isEmpty()) {
                showErrorDialog("Campaign name cannot be empty");
                return false;
            }

            // Get user ID from session
            int userId = getUserId();

            // Save campaign to database
            int campaignId = CampaignDatabase.saveCampaign(campaignMetrics, campaignName, userId);

            if (campaignId != -1) {
                showInfoDialog("Campaign Saved",
                    "Campaign \"" + campaignName + "\" saved successfully with ID: " + campaignId);
                return true;
            } else {
                showErrorDialog("Failed to save campaign. Please try again.");
                return false;
            }
        }

        return false;
    }

    private static int getUserId() {
        // Get user ID from session if available
        if (UserSession.getInstance().getUser() != null) {
            return UserSession.getInstance().getUser().getId();
        }
        return 0; // Default to 0 if no user session
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