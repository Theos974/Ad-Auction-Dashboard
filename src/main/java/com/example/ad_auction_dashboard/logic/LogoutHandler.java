package com.example.ad_auction_dashboard.logic;

import com.example.ad_auction_dashboard.logic.UserSession;
import com.example.ad_auction_dashboard.viewer.LoginScene;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.stage.Stage;

/**
 * Utility class to handle logout functionality across different scenes
 */
public class LogoutHandler {

    /**
     * Common logout handler that can be attached to any logout button
     *
     * @param event The ActionEvent from the button click
     */
    public static void handleLogout(ActionEvent event) {
        // Clear the user session
        UserSession.getInstance().logout();

        // Get the current stage from the event source
        Button sourceButton = (Button) event.getSource();
        Stage currentStage = (Stage) sourceButton.getScene().getWindow();

        // Create a new login scene and show it
        new LoginScene(currentStage, 930, 692);
        currentStage.show();
    }
}