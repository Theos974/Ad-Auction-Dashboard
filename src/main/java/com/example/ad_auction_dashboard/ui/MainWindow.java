package com.example.ad_auction_dashboard.ui;

import com.example.ad_auction_dashboard.scenes.StartScene;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class MainWindow {
    private final int width;
    private final int height;

    private final Stage stage;

    private Scene scene;


    public MainWindow(Stage stage, int width, int height) {
        this.width = width;
        this.height = height;

        this.stage = stage;

        //Setup window
        setupStage();

        //Setup default scene
        setupDefaultScene();
    }

    public void setupStage() {
        stage.setTitle("Tester");
        stage.setMinWidth(width);
        stage.setMinHeight(height + 20);
    }

    public void setupDefaultScene() {
        this.scene = new StartScene(this).setScene();
        stage.setScene(this.scene);
    }

    public int getWidth(){
        return this.width;
    }
    public int getHeight(){
        return this.height;
    }
}
