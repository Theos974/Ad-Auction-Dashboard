package com.example.ad_auction_dashboard.controller;

import com.example.ad_auction_dashboard.logic.Campaign;
import com.example.ad_auction_dashboard.logic.CampaignMetrics;
import com.example.ad_auction_dashboard.logic.FileHandler;
import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import java.io.File;
import javafx.stage.Stage;

public class StartSceneController {

    @FXML
    private Button loadZipBtn;
    @FXML
    private Button createCampaignBtn;
    @FXML
    private Text statusText;

    private Campaign campaign;

    // Event handler for loading a ZIP file and creating the campaign
    @FXML
    private void handleLoadZip(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load ZIP File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP Files", "*.zip"));
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home") + File.separator + "Documents"));
        File selected = fileChooser.showOpenDialog(loadZipBtn.getScene().getWindow());
        if (selected != null) {
            FileHandler fileHandler = new FileHandler();
            campaign = fileHandler.openZip(selected.getAbsolutePath());
            if (campaign != null) {
                statusText.setText("Campaign loaded from: " + selected.getName());
            } else {
                statusText.setText("Error loading campaign from ZIP.");
            }
        }
    }

    // Event handler for switching to the campaign screen
    @FXML
    private void handleCreateCampaign(ActionEvent event) {
        if (campaign == null) {
            statusText.setText("Please load a ZIP file first.");
            return;
        }
        statusText.setText("Campaign created. Switching scene...");
        CampaignMetrics metrics = new CampaignMetrics(campaign);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/ad_auction_dashboard/fxml/MetricScene.fxml"));
            Parent root = loader.load();
            MetricSceneController controller = loader.getController();
            controller.setMetrics(metrics);
            Stage stage = (Stage) createCampaignBtn.getScene().getWindow();
            Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
            statusText.setText("Error switching scene.");
        }
    }

}
