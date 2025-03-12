package com.example.ad_auction_dashboard.viewer;

import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        try {

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