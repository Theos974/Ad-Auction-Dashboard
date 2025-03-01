package com.example.ad_auction_dashboard.viewer;

import com.example.ad_auction_dashboard.viewer.StartScene;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        // Create your StartScene using the primary stage
        new StartScene(primaryStage, 1400, 800);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
