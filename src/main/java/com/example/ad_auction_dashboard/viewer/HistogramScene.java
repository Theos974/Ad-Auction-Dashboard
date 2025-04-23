package com.example.ad_auction_dashboard.viewer;

import com.example.ad_auction_dashboard.controller.HistogramController;
import com.example.ad_auction_dashboard.logic.CampaignMetrics;
import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HistogramScene {
    private final int width;
    private final int height;
    private final String style;
    private final Stage stage;
    private Scene scene;
    private HistogramController controller;

    public HistogramScene(Stage stage, int width, int height, String style) {
        this.width = width;
        this.height = height;
        this.stage = stage;
        this.style = style;
        setupStage();
        setupDefaultScene();
    }

    public void setupStage() {
        stage.setTitle("Ad Auction Dashboard - Data Distribution");
        stage.setMinWidth(width);
        stage.setMinHeight(height + 20);
    }

    public void setupDefaultScene() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/ad_auction_dashboard/fxml/HistogramScene.fxml"));
            Parent root = loader.load();
            controller = loader.getController();
            this.scene = new Scene(root, width, height);
            this.scene.getStylesheets().add(style);
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setCampaignMetrics(CampaignMetrics metrics) {
        if (controller != null) {
            controller.setCampaignMetrics(metrics);
        }
    }

    public void setHistogramType(String histogramType) {
        if (controller != null) {
            controller.setHistogramType(histogramType);
        }
    }

    public Scene getScene() {
        return scene;
    }
}