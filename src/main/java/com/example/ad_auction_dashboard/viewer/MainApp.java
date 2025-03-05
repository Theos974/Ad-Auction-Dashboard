package com.example.ad_auction_dashboard.viewer;

import com.example.ad_auction_dashboard.logic.DatabaseInitializer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        try {
            // Initialize the database on startup
            DatabaseInitializer.initializeDatabase();

            // Load the login scene
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/ad_auction_dashboard/fxml/LoginScene.fxml"));
            Parent root = loader.load();

            // Set the scene
            Scene scene = new Scene(root, 930, 692);
            primaryStage.setTitle("Ad Auction Dashboard");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(930);
            primaryStage.setMinHeight(712); // Adding extra height for window decorations
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}