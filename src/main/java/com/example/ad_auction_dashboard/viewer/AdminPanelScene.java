package com.example.ad_auction_dashboard.viewer;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AdminPanelScene {
    private final int width;
    private final int height;
    private final String style;
    private final Stage stage;
    private Scene scene;

    public AdminPanelScene(Stage stage, int width, int height, String style) {
        this.width = width;
        this.height = height;
        this.stage = stage;
        this.style = style;
        setupStage();
        setupDefaultScene();
    }

    public void setupStage() {
        stage.setTitle("Ad Auction Dashboard - Admin Panel");
        stage.setMinWidth(width);
        stage.setMinHeight(height + 20);
    }

    public void setupDefaultScene() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/ad_auction_dashboard/fxml/AdminPanelScene.fxml"));
            Parent root = loader.load();
            this.scene = new Scene(root, width, height);
            this.scene.getStylesheets().add(style);
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Scene getScene() {
        return scene;
    }
}