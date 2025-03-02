package com.example.ad_auction_dashboard.viewer;

import com.example.ad_auction_dashboard.controller.ChartSceneController;
import com.example.ad_auction_dashboard.controller.MetricSceneController;
import com.example.ad_auction_dashboard.logic.CampaignMetrics;
import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ChartScene {

    private final int width;
    private final int height;
    private final Stage stage;
    private Scene scene;
    private ChartSceneController controller;

    public ChartScene(Stage stage, int width, int height) {
        this.width = width;
        this.height = height;
        this.stage = stage;
        setupStage();
        setupDefaultScene();
    }

    public void setupStage() {
        stage.setTitle("Ad Auction Dashboard");
        stage.setMinWidth(width);
        stage.setMinHeight(height + 20);
    }

    public void setupDefaultScene() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/ad_auction_dashboard/fxml/ChartScene.fxml"));
            Parent root = loader.load();
            // Retrieve the controller so we can set campaign metrics later
            controller = loader.getController();
            this.scene = new Scene(root, width, height);
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Scene getScene() {
        return scene;
    }

}