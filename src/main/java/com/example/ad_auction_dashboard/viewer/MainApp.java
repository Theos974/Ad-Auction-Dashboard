package com.example.ad_auction_dashboard.viewer;

import com.example.ad_auction_dashboard.logic.CampaignDatabase;
import java.sql.DriverManager;
import java.sql.SQLException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        try {

            // Create LoginScene as the entry point

            new LoginScene(primaryStage, 930, 692);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/ad_auction_dashboard/fxml/LoginScene.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 930, 692);
            scene.getStylesheets().add(this.getClass().getClassLoader().getResource("styles/style.css").toString());
            primaryStage.setScene(scene);

            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}