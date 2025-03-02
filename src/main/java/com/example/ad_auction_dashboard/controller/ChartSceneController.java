package com.example.ad_auction_dashboard.controller;

import com.example.ad_auction_dashboard.charts.Chart;
import com.example.ad_auction_dashboard.charts.BounceRateChart;
import com.example.ad_auction_dashboard.charts.CPCChart;
import com.example.ad_auction_dashboard.charts.CTRChart;
import com.example.ad_auction_dashboard.charts.CPMChart;
import com.example.ad_auction_dashboard.charts.ConversionsChart;
import com.example.ad_auction_dashboard.charts.ClicksChart;
import com.example.ad_auction_dashboard.charts.ClicksChart;
import com.example.ad_auction_dashboard.charts.ImpressionsChart;
import com.example.ad_auction_dashboard.logic.CampaignMetrics;
import com.example.ad_auction_dashboard.logic.TimeFilteredMetrics;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.layout.HBox;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ChartSceneController {
    // Suppose you have a ComboBox to select which chart the user wants to see:
    @FXML private ComboBox<String> chartTypeComboBox;
    @FXML private VBox chartPlaceholder;

    private CampaignMetrics campaignMetrics;
    private TimeFilteredMetrics timeFilteredMetrics;

    private final Map<String, Chart> chartRegistry = new HashMap<>();

    @FXML
    public void initialize() {
        chartRegistry.put("Clicks", new ClicksChart());
        chartRegistry.put("Impressions", new ImpressionsChart());
        chartRegistry.put("CTR", new CTRChart());
        // ... add more ...

        chartTypeComboBox.getItems().addAll(chartRegistry.keySet());
        chartTypeComboBox.setOnAction(e -> updateChart());
    }

    public void setCampaignMetrics(CampaignMetrics metrics) {
        this.campaignMetrics = metrics;
        // create the TimeFilteredMetrics once
        this.timeFilteredMetrics = new TimeFilteredMetrics(
            metrics.getImpressionLogs(),
            metrics.getServerLogs(),
            metrics.getClickLogs(),
            metrics.getBouncePagesThreshold(),
            metrics.getBounceSecondsThreshold()
        );
    }

    private void updateChart() {
        String selected = chartTypeComboBox.getValue();
        if (selected == null) return;
        Chart chartImpl = chartRegistry.get(selected);
        if (chartImpl == null) return;

        // define start/end from DatePickers or from your form
        LocalDateTime start = ... // your code
        LocalDateTime end   = ... // your code

        // create the chart Node
        VBox chartNode = chartImpl.createChart(timeFilteredMetrics, start, end);

        // place in UI
        chartPlaceholder.getChildren().clear();
        chartPlaceholder.getChildren().add(chartNode);
    }
}


