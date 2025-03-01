package com.example.ad_auction_dashboard.viewer;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class StartScene {
    private final int width;
    private final int height;
    private final Stage stage;
    private Scene scene;

    public StartScene(Stage stage, int width, int height) {
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/ad_auction_dashboard/fxml/StartScene.fxml"));
            Parent root = loader.load();
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
