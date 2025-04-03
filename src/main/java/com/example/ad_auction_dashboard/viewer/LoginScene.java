package com.example.ad_auction_dashboard.viewer;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class LoginScene {
    private final int width;
    private final int height;
    private final String style;
    private final Stage stage;
    private Scene scene;

    public LoginScene(Stage stage, int width, int height, String style) {
        this.width = width;
        this.height = height;
        this.style = style;
        this.stage = stage;
        setupStage();
        setupDefaultScene();
    }

    public void setupStage() {
        stage.setTitle("Ad Auction Dashboard - Login");
        stage.setMinWidth(width);
        stage.setMinHeight(height);
    }

    public void setupDefaultScene() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/ad_auction_dashboard/fxml/LoginScene.fxml"));
            Parent root = loader.load();
            this.scene = new Scene(root, width, height);
            this.scene.getStylesheets().add(this.style);
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Scene getScene() {
        return scene;
    }
}