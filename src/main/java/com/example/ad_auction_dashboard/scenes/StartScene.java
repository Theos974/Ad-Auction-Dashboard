package com.example.ad_auction_dashboard.scenes;

import com.example.ad_auction_dashboard.ui.MainWindow;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

import java.io.File;

public class StartScene {

    StackPane root;
    MainWindow mainWindow;

    Text output;

    Scene scene;
    public StartScene(MainWindow mainWindow){
        this.mainWindow = mainWindow;
        this.build();
    }

    public Scene setScene(){
        Scene scene =new Scene(root, 800,600);
        this.scene = scene;
        return scene;
    }

    public void build() {
        root = new StackPane();
        var startPane = new StackPane();
        startPane.setMaxWidth(mainWindow.getWidth());
        startPane.setMaxHeight(mainWindow.getHeight());
        startPane.setStyle("-fx-background-color: beige");

        root.getChildren().add(startPane);

        var mainPane = new BorderPane();
        startPane.getChildren().add(mainPane);

        VBox mainColumn = new VBox();
        mainPane.setCenter(mainColumn);
        mainColumn.setAlignment(Pos.CENTER);

        Button tempButton = new Button("Click Me!");
        tempButton.setMinHeight(100);
        tempButton.setMaxWidth(100);
        tempButton.setOnAction(this::tempButtonPressed);

        mainColumn.getChildren().add(tempButton);

        var output = new Text();
        mainColumn.getChildren().add(output);
        this.output = output;
    }

    private void tempButtonPressed(ActionEvent actionEvent) {
        //nothing
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Data Files");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("*.csv", "*.csv"),
                new FileChooser.ExtensionFilter("*.zip", "*.zip")
        );
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home") + System.getProperty("file.separator") + "Documents"));
        File selected = fileChooser.showOpenDialog(this.scene.getWindow());
        this.output.setText(selected.toString());
    }
}
