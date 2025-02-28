package com.example.ad_auction_dashboard.controller;

import com.example.ad_auction_dashboard.logic.Campaign;
import com.example.ad_auction_dashboard.logic.FileHandler;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import java.io.File;

public class StartSceneController {

    @FXML
    private Button loadZipBtn;
    @FXML
    private Button showCampaignBtn;
    @FXML
    private Text statusText;

    // Field to store the created campaign
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
    private void handleShowCampaign(ActionEvent event) {
        if (campaign == null) {
            statusText.setText("Please load a ZIP file first.");
            return;
        }
        statusText.setText("Campaign created. Switching scene...");
        // TODO: Switch to the MetricScene (or another campaign view scene)
        // For example, if you have a SceneManager:
        // SceneManager.getInstance().switchScene(new MetricScene(campaign).getScene());
    }
}
