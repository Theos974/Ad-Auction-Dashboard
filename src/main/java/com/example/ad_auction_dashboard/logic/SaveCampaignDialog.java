package com.example.ad_auction_dashboard.logic;

import com.example.ad_auction_dashboard.controller.MetricSceneController;
import com.example.ad_auction_dashboard.logic.CampaignDatabase;
import com.example.ad_auction_dashboard.logic.CampaignMetrics;
import com.example.ad_auction_dashboard.logic.UserSession;
import javafx.application.Platform;
import javafx.concurrent.Task;
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
    public static void showDialog(Stage owner, CampaignMetrics campaignMetrics, MetricSceneController metricSceneController) {
        // Create dialog
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Save Campaign");
        dialog.setHeaderText("Save your campaign to the database for future use");
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.getDialogPane().getStylesheets().add(UserSession.getInstance().getCurrentStyle());
        dialog.getDialogPane().getStyleClass().add("save-dialog");

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
                return;
            }

            // Get user ID from session
            int userId = getUserId();

            metricSceneController.startSaveAnimation();
            // Save campaign to database
            new Thread(() -> {
                int campaignId = CampaignDatabase.saveCampaign(campaignMetrics, campaignName, userId);
                Platform.runLater(() -> {metricSceneController.toggleControls(false);
                    metricSceneController.stopSaveAnimation();});
                if (campaignId != -1) {
                    Platform.runLater(() -> showInfoDialog("Campaign Saved",
                            "Campaign \"" + campaignName + "\" saved successfully with ID: " + campaignId));
                } else {
                    Platform.runLater(() -> showErrorDialog("Failed to save campaign. Please try again."));
                }
            }).start();
            return;
        }

        metricSceneController.toggleControls(false);
        return;
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