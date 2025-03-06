package com.example.ad_auction_dashboard.viewer;

import com.example.ad_auction_dashboard.logic.UserDatabase; // Import UserDatabase
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        try {
            // Access UserDatabase to trigger static initializer
            UserDatabase.getUserRole("admin");

            // Create LoginScene as the entry point
            new LoginScene(primaryStage, 930, 692);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}