package com.example.ad_auction_dashboard.logic;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Dialog for displaying comparison between two campaign metrics
 */
public class MetricComparisonView {

    /**
     * Show comparison view for a specific metric
     *
     * @param owner The owner window
     * @param metricName The name of the metric being compared
     * @param currentCampaignName Name of the current campaign
     * @param compareCampaignName Name of the campaign being compared with
     * @param currentValue The metric value from the current campaign
     * @param compareValue The metric value from the comparison campaign
     * @param formattedCurrentValue The formatted string of current value
     * @param formattedCompareValue The formatted string of compare value
     * @param metricUnit Optional unit (%, $, etc.) or null
     */
    public static void show(Stage owner, String metricName,
                            String currentCampaignName, String compareCampaignName,
                            double currentValue, double compareValue,
                            String formattedCurrentValue, String formattedCompareValue,
                            String metricUnit) {

        // Create stage
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle("Metric Comparison: " + metricName);

        // Create layout
        BorderPane layout = new BorderPane();
        layout.getStyleClass().add("metric-comparison-view");
        layout.setStyle("-fx-background-color: #2d2d3b;");
        layout.setPadding(new Insets(20));

        // Header
        Text header = new Text("Comparing " + metricName);
        header.setFont(Font.font("System", FontWeight.BOLD, 20));
        header.setStyle("-fx-fill: white;");

        HBox headerBox = new HBox(header);
        headerBox.setAlignment(Pos.CENTER);
        layout.setTop(headerBox);

        // Comparison area
        GridPane grid = new GridPane();
        grid.setHgap(30);
        grid.setVgap(15);
        grid.setPadding(new Insets(30, 10, 30, 10));
        grid.setAlignment(Pos.CENTER);

        // Column headers
        Label currentCampaignLabel = new Label(currentCampaignName);
        currentCampaignLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        currentCampaignLabel.setStyle("-fx-text-fill: white;");

        Label compareCampaignLabel = new Label(compareCampaignName);
        compareCampaignLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        compareCampaignLabel.setStyle("-fx-text-fill: white;");

        grid.add(currentCampaignLabel, 0, 0);
        grid.add(compareCampaignLabel, 1, 0);

        // Values
        Label currentValueLabel = new Label(formattedCurrentValue);
        currentValueLabel.setFont(Font.font("System", FontWeight.NORMAL, 24));
        currentValueLabel.setStyle("-fx-text-fill: #a365f5;");

        Label compareValueLabel = new Label(formattedCompareValue);
        compareValueLabel.setFont(Font.font("System", FontWeight.NORMAL, 24));
        compareValueLabel.setStyle("-fx-text-fill: #65a3f5;");

        grid.add(currentValueLabel, 0, 1);
        grid.add(compareValueLabel, 1, 1);

        // Calculate difference and percentage
        double absoluteDiff = compareValue - currentValue;
        double percentageDiff = 0;

        if (currentValue != 0) {
            percentageDiff = (absoluteDiff / Math.abs(currentValue)) * 100;
        }

        // Comparison stats
        VBox statsBox = new VBox(10);
        statsBox.setAlignment(Pos.CENTER);
        statsBox.setPadding(new Insets(20, 0, 0, 0));

        // Absolute difference
        String sign = absoluteDiff >= 0 ? "+" : "";
        String diffFormatted;

        if (metricUnit != null && !metricUnit.isEmpty()) {
            diffFormatted = String.format("%s%.4f %s", sign, absoluteDiff, metricUnit);
        } else {
            diffFormatted = String.format("%s%.4f", sign, absoluteDiff);
        }

        Label diffLabel = new Label("Absolute difference: " + diffFormatted);
        diffLabel.setStyle("-fx-text-fill: white;");

        // Percentage difference
        Label percentLabel;
        if (currentValue == 0 && compareValue == 0) {
            percentLabel = new Label("Both values are zero");
        } else if (currentValue == 0) {
            percentLabel = new Label("Cannot calculate percentage (division by zero)");
        } else {
            String percentSign = percentageDiff >= 0 ? "+" : "";
            percentLabel = new Label(String.format("Relative difference: %s%.2f%%", percentSign, percentageDiff));
        }
        percentLabel.setStyle("-fx-text-fill: white;");

        // Interpretation
        Label interpretationLabel = new Label(getInterpretation(metricName, percentageDiff, absoluteDiff));
        interpretationLabel.setStyle("-fx-text-fill: #ffcc66; -fx-font-style: italic;");
        interpretationLabel.setWrapText(true);

        statsBox.getChildren().addAll(
            new Separator(),
            diffLabel,
            percentLabel,
            new Separator(),
            interpretationLabel
        );

        // Add grid to a VBox so we can add the stats below
        VBox centerBox = new VBox(10);
        centerBox.getChildren().addAll(grid, statsBox);
        centerBox.setAlignment(Pos.CENTER);

        layout.setCenter(centerBox);

        // Close button
        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> dialog.close());
        closeButton.setPrefWidth(100);
        closeButton.setStyle("-fx-background-color: #363642; -fx-text-fill: white; -fx-background-radius: 5;");

        HBox buttonBox = new HBox(closeButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(20, 0, 0, 0));
        layout.setBottom(buttonBox);

        // Create scene and show
        Scene scene = new Scene(layout, 500, 400);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    /**
     * Get an interpretation of the difference based on the metric type
     */
    private static String getInterpretation(String metricName, double percentageDiff, double absoluteDiff) {
        String better = absoluteDiff > 0 ? "higher" : "lower";
        String worse = absoluteDiff > 0 ? "lower" : "higher";

        switch (metricName.toLowerCase()) {
            case "impressions":
            case "clicks":
            case "unique users":
            case "conversions":
                return String.format("The comparison campaign has %.2f%% %s %s, which generally indicates %s performance.",
                    Math.abs(percentageDiff), better, metricName.toLowerCase(), absoluteDiff > 0 ? "better" : "worse");

            case "total cost":
                return String.format("The comparison campaign costs %.2f%% %s, which is %s if other metrics are comparable.",
                    Math.abs(percentageDiff), absoluteDiff > 0 ? "more" : "less", absoluteDiff > 0 ? "worse" : "better");

            case "ctr":
            case "click-through rate":
                return String.format("The comparison campaign has a %.2f%% %s CTR, which indicates %s ad relevance or targeting.",
                    Math.abs(percentageDiff), better, absoluteDiff > 0 ? "better" : "worse");

            case "cpc":
            case "cost-per-click":
                return String.format("The comparison campaign has a %.2f%% %s CPC, which is %s for cost efficiency.",
                    Math.abs(percentageDiff), better, absoluteDiff < 0 ? "better" : "worse");

            case "cpa":
            case "cost-per-acquisition":
                return String.format("The comparison campaign has a %.2f%% %s CPA, which is %s for ROI.",
                    Math.abs(percentageDiff), better, absoluteDiff < 0 ? "better" : "worse");

            case "cpm":
            case "cost-per-thousand impressions":
                return String.format("The comparison campaign has a %.2f%% %s CPM, which is %s for impression cost efficiency.",
                    Math.abs(percentageDiff), better, absoluteDiff < 0 ? "better" : "worse");

            case "bounces":
            case "bounce rate":
                return String.format("The comparison campaign has a %.2f%% %s bounce rate, which indicates %s user engagement.",
                    Math.abs(percentageDiff), better, absoluteDiff < 0 ? "better" : "worse");

            default:
                return String.format("The comparison campaign's %s is %.2f%% %s than the current campaign.",
                    metricName.toLowerCase(), Math.abs(percentageDiff), absoluteDiff > 0 ? "higher" : "lower");
        }
    }
}