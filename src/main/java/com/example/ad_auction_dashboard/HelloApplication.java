package com.example.ad_auction_dashboard;

import com.example.ad_auction_dashboard.ui.MainWindow;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;


public class HelloApplication extends Application {

    private static HelloApplication instance;
    private Stage stage;
    @Override
    public void start(Stage stage) throws IOException {
        instance = this;
        this.stage = stage;
        var mainWindow = new MainWindow(stage,800,600);

        //Display the GameWindow
        stage.show();
    }

    //TESTER COMMENT
    public static void main(String[] args) {
        launch();
    }
}