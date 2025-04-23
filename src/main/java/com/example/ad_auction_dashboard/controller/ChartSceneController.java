package com.example.ad_auction_dashboard.controller;

import com.example.ad_auction_dashboard.charts.Chart;
import com.example.ad_auction_dashboard.charts.BounceRateChart;
import com.example.ad_auction_dashboard.charts.CPAChart;
import com.example.ad_auction_dashboard.charts.CPCChart;
import com.example.ad_auction_dashboard.charts.CTRChart;
import com.example.ad_auction_dashboard.charts.CPMChart;
import com.example.ad_auction_dashboard.charts.ConversionsChart;
import com.example.ad_auction_dashboard.charts.ClicksChart;
import com.example.ad_auction_dashboard.charts.ImpressionsChart;
import com.example.ad_auction_dashboard.charts.TotalCostChart;
import com.example.ad_auction_dashboard.charts.UniquesChart;
import com.example.ad_auction_dashboard.charts.BounceChart;
import com.example.ad_auction_dashboard.logic.CampaignMetrics;
import com.example.ad_auction_dashboard.logic.TimeFilteredMetrics;

import com.example.ad_auction_dashboard.logic.UserSession;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfWriter;
import com.opencsv.CSVWriter;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.XYChart;
import javafx.scene.image.WritableImage;
import javafx.scene.shape.Circle;
import org.apache.commons.io.FilenameUtils;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

public class ChartSceneController {

    @FXML
    private ComboBox<String> timeGranularityComboBox;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private Label statusLabel; // Add this to your FXML if not already present

    @FXML
    private Label printLabel;

    @FXML
    private ComboBox<String> primaryChartTypeComboBox;

    @FXML
    private ComboBox<String> secondaryChartTypeComboBox;

    @FXML
    private ComboBox<String> genderFilterComboBox; // Gender filter dropdown

    @FXML
    private ComboBox<String> contextFilterComboBox; // Context filter dropdown

    @FXML
    private ComboBox<String> ageFilterComboBox; // Age filter dropdown

    @FXML
    private ComboBox<String> incomeFilterComboBox; // Income filter dropdown

    @FXML
    private ComboBox<String> exportComboBox;

    @FXML
    private Button resetFiltersButton; // Reset filters button

    @FXML
    private ToggleButton compareToggleButton;

    @FXML
    private VBox primaryChartContainer;

    @FXML
    private VBox secondaryChartContainer;

    @FXML
    private Button backButton;
    @FXML
    private Button printButton;
    @FXML
    private Button exportButton;

    @FXML
    private LineChart primaryChart;

    @FXML
    private LineChart secondaryChart;

    @FXML
    private HBox chartsContainer;

    @FXML
    private ToggleButton colourSwitch;

    private String currentStyle;

    private CampaignMetrics campaignMetrics;
    private TimeFilteredMetrics timeFilteredMetrics;
    private final Map<String, Chart> chartRegistry = new LinkedHashMap<>(); // LinkedHashMap to maintain insertion order

    // Current granularity selection
    private String currentGranularity = "Daily"; // Default

    @FXML
    public void initialize() {
        // Register all available chart types - all 11 metrics
        chartRegistry.put("Impressions", new ImpressionsChart());
        chartRegistry.put("Clicks", new ClicksChart());
        chartRegistry.put("Unique Users", new UniquesChart());
        chartRegistry.put("Bounces", new BounceChart());
        chartRegistry.put("Conversions", new ConversionsChart());
        chartRegistry.put("Total Cost", new TotalCostChart());
        chartRegistry.put("CTR", new CTRChart());
        chartRegistry.put("CPC", new CPCChart());
        chartRegistry.put("CPA", new CPAChart());
        chartRegistry.put("CPM", new CPMChart());
        chartRegistry.put("Bounce Rate", new BounceRateChart());

        // Add chart types to combo boxes
        primaryChartTypeComboBox.getItems().addAll(chartRegistry.keySet());
        primaryChartTypeComboBox.setValue("Impressions"); // Default selection

        secondaryChartTypeComboBox.getItems().addAll(chartRegistry.keySet());
        secondaryChartTypeComboBox.setValue("CPC"); // Default selection

        exportComboBox.getItems().addAll("Chart 1 PNG", "Chart 2 PNG", "Chart 1 CSV", "Chart 2 CSV", "Chart 1 PDF", "Chart 2 PDF", "Combined PDF");
        exportComboBox.setValue("Chart 1 PNG");

        // Setup time granularity options
        timeGranularityComboBox.getItems().addAll("Hourly", "Daily", "Weekly");
        timeGranularityComboBox.setValue("Daily"); // Default

        // Setup audience filter options - gender
        genderFilterComboBox.getItems().addAll("All", "Male", "Female");
        genderFilterComboBox.setValue("All"); // Default

        // Setup context filter options
        contextFilterComboBox.getItems().addAll("All", "News", "Shopping", "Social Media",
            "Blog", "Hobbies", "Travel");
        contextFilterComboBox.setValue("All"); // Default

        // Setup age filter options (new)
        ageFilterComboBox.getItems().addAll("All", "<25", "25-34", "35-44", "45-54", ">54");
        ageFilterComboBox.setValue("All"); // Default

        // Setup income filter options (new)
        incomeFilterComboBox.getItems().addAll("All", "Low", "Medium", "High");
        incomeFilterComboBox.setValue("All"); // Default

        // Setup toggle button for comparison
        compareToggleButton.setOnAction(e -> {
            boolean showComparison = compareToggleButton.isSelected();
            secondaryChartContainer.setVisible(showComparison);
            secondaryChartContainer.setManaged(showComparison);
            updateCharts();
        });

        // Setup reset filters button
        if (resetFiltersButton != null) {
            resetFiltersButton.setOnAction(e -> resetFilters());
        }

        // Setup event listeners
        primaryChartTypeComboBox.setOnAction(e -> updateCharts());
        secondaryChartTypeComboBox.setOnAction(e -> {
            if (compareToggleButton.isSelected()) {
                updateCharts();
            }
        });

        // Add filter change listeners
        genderFilterComboBox.setOnAction(e -> applyFilters());
        contextFilterComboBox.setOnAction(e -> applyFilters());
        ageFilterComboBox.setOnAction(e -> applyFilters());
        incomeFilterComboBox.setOnAction(e -> applyFilters());

        timeGranularityComboBox.setOnAction(e -> {
            currentGranularity = timeGranularityComboBox.getValue();
            updateCharts();

            // Provide feedback about hourly view for long time periods
            if (currentGranularity.equals("Hourly")) {
                long days = ChronoUnit.DAYS.between(startDatePicker.getValue(), endDatePicker.getValue()) + 1;
                if (days > 7) {
                    statusLabel.setText("Note: Hourly view with " + days +
                        " days. X-axis labels may be compressed for readability.");
                }
            } else {
                statusLabel.setText("");
            }
        });

        // Date picker event listeners
        startDatePicker.setOnAction(e -> {
            validateDateRange();
            updateCharts();
        });

        endDatePicker.setOnAction(e -> {
            validateDateRange();
            updateCharts();
        });

        // Initialize status label if present
        if (statusLabel != null) {
            statusLabel.setText("");
        }

        Circle thumb = new Circle(12);
        thumb.getStyleClass().add("thumb");
        colourSwitch.setGraphic(thumb);

        currentStyle = UserSession.getInstance().getCurrentStyle();
        System.out.println("StyleChart: " + currentStyle);
        if (Objects.equals(currentStyle, this.getClass().getClassLoader().getResource("styles/lightStyle.css").toString())){
            colourSwitch.setSelected(true);
            System.out.println("Switched");
        }
        this.setCampaignMetrics(UserSession.getInstance().getCurrentCampaignMetrics());
    }

    /**
     * Apply selected filters to the metrics
     */
    private void applyFilters() {
        if (timeFilteredMetrics == null) return;

        toggleFilters(true);
        // Get selected gender filter
        String gender = genderFilterComboBox.getValue();
        if (gender.equals("All")) gender = null;

        // Get selected context filter
        String context = contextFilterComboBox.getValue();
        if (context.equals("All")) context = null;

        // Get selected age filter (new)
        String age = ageFilterComboBox.getValue();
        if (age.equals("All")) age = null;

        // Get selected income filter (new)
        String income = incomeFilterComboBox.getValue();
        if (income.equals("All")) income = null;

        String finalGender = gender;
        String finalContext = context;
        String finalAge = age;
        String finalIncome = income;
        new Thread(() -> {
            // Apply filters to timeFilteredMetrics
            timeFilteredMetrics.setGenderFilter(finalGender);
            timeFilteredMetrics.setContextFilter(finalContext);
            timeFilteredMetrics.setAgeFilter(finalAge);
            timeFilteredMetrics.setIncomeFilter(finalIncome);

            // Update status label to show active filters
            updateFilterStatus();

            // Update charts with the new filters
            Platform.runLater(() -> {
                updateCharts();
                toggleFilters(false);
            });
        }).start();
    }

    @FXML
    /**
     * Reset all filters to their default values
     */
    private void resetFilters() {
        if (timeFilteredMetrics == null) return;

        toggleFilters(true);
        // Reset UI components
        genderFilterComboBox.setValue("All");
        contextFilterComboBox.setValue("All");
        ageFilterComboBox.setValue("All");
        incomeFilterComboBox.setValue("All");

        new Thread(() -> {
            // Reset filters in the metrics object
            timeFilteredMetrics.setGenderFilter(null);
            timeFilteredMetrics.setContextFilter(null);
            timeFilteredMetrics.setAgeFilter(null);
            timeFilteredMetrics.setIncomeFilter(null);

            // Update status label
            if (statusLabel != null) {
                Platform.runLater(() -> statusLabel.setText("Filters reset to default."));
            }

            // Update charts
            Platform.runLater(() -> {
                updateCharts();
                toggleFilters(false);
            });
        }).start();
    }

    /**
     * Update the status label to show currently active filters
     */
    private void updateFilterStatus() {
        if (statusLabel == null) return;

        StringBuilder status = new StringBuilder();

        // Add gender filter info if active
        if (!genderFilterComboBox.getValue().equals("All")) {
            status.append("Gender: ").append(genderFilterComboBox.getValue());
        }

        // Add context filter info if active
        if (!contextFilterComboBox.getValue().equals("All")) {
            if (status.length() > 0) status.append(" | ");
            status.append("Context: ").append(contextFilterComboBox.getValue());
        }

        // Add age filter info if active (new)
        if (!ageFilterComboBox.getValue().equals("All")) {
            if (status.length() > 0) status.append(" | ");
            status.append("Age: ").append(ageFilterComboBox.getValue());
        }

        // Add income filter info if active (new)
        if (!incomeFilterComboBox.getValue().equals("All")) {
            if (status.length() > 0) status.append(" | ");
            status.append("Income: ").append(incomeFilterComboBox.getValue());
        }

        // Update status label
        if (status.length() > 0) {
            Platform.runLater(() -> statusLabel.setText("Active filters: " + status));
        } else {
            Platform.runLater(() -> statusLabel.setText("No filters active"));
        }
    }

    public void setCampaignMetrics(CampaignMetrics metrics) {
        this.campaignMetrics = metrics;

        // Create TimeFilteredMetrics instance
        this.timeFilteredMetrics = new TimeFilteredMetrics(
            metrics.getImpressionLogs(),
            metrics.getServerLogs(),
            metrics.getClickLogs(),
            metrics.getBouncePagesThreshold(),
            metrics.getBounceSecondsThreshold()
        );

        // Get campaign date boundaries
        LocalDateTime campaignStart = metrics.getCampaignStartDate();
        LocalDateTime campaignEnd = metrics.getCampaignEndDate();

        if (campaignStart != null && campaignEnd != null) {
            LocalDate startDate = campaignStart.toLocalDate();
            LocalDate endDate = campaignEnd.toLocalDate();

            // Set default values to campaign start and end dates
            startDatePicker.setValue(startDate);
            endDatePicker.setValue(endDate);

            // Set date range constraints for both pickers
            startDatePicker.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    setDisable(empty || date.isBefore(startDate) || date.isAfter(endDate));
                }
            });

            endDatePicker.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    setDisable(empty || date.isBefore(startDate) || date.isAfter(endDate));
                }
            });
        }

        // Show initial charts
        updateCharts();
    }

    /**
     * Validates and corrects the date range if needed
     */
    private void validateDateRange() {
        if (startDatePicker.getValue() != null && endDatePicker.getValue() != null) {
            if (endDatePicker.getValue().isBefore(startDatePicker.getValue())) {
                // End date is before start date, correct it
                endDatePicker.setValue(startDatePicker.getValue());
                showAlert("Invalid date range detected. End date has been adjusted to match start date.");
            }
        }
    }

    /**
     * Shows an alert to the user
     */
    private void showAlert(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        }
    }

    private void updateCharts() {
        if (campaignMetrics == null) return;

        // Get date range
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        if (startDate == null || endDate == null) return;

        // Convert to LocalDateTime
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        new Thread(() -> {
            try {
                // Update status if processing a lot of data
                if (statusLabel != null && currentGranularity.equals("Hourly") &&
                        ChronoUnit.DAYS.between(startDate, endDate) > 14) {
                    statusLabel.setText("Processing large amount of hourly data...");
                }

                // Compute metrics for the time frame with granularity
                timeFilteredMetrics.computeForTimeFrame(start, end, currentGranularity);

                // Create and display primary chart
                String primaryChartType = primaryChartTypeComboBox.getValue();
                Chart primaryChartImpl = chartRegistry.get(primaryChartType);
                if (primaryChartImpl != null) {
                    VBox primaryChartNode = primaryChartImpl.createChart(
                            timeFilteredMetrics, start, end, currentGranularity);
                    Platform.runLater(() -> {
                        primaryChartContainer.getChildren().clear();
                        primaryChartContainer.getChildren().add(primaryChartNode);
                        primaryChart = (LineChart) primaryChartNode.getChildren().get(0);
                    });
                }

                // Create and display secondary chart if comparison is enabled
                if (compareToggleButton.isSelected()) {
                    String secondaryChartType = secondaryChartTypeComboBox.getValue();
                    Chart secondaryChartImpl = chartRegistry.get(secondaryChartType);
                    if (secondaryChartImpl != null) {
                        VBox secondaryChartNode = secondaryChartImpl.createChart(
                                timeFilteredMetrics, start, end, currentGranularity);
                        Platform.runLater(() -> {
                            secondaryChartContainer.getChildren().clear();
                            secondaryChartContainer.getChildren().add(secondaryChartNode);
                            secondaryChart = (LineChart) secondaryChartNode.getChildren().get(0);
                        });
                    }
                }

                // Update filter status message
                Platform.runLater(() -> {
                    updateFilterStatus();
                    toggleFilters(false);
                });
            } catch (Exception e) {
                // Show error to user
                showAlert("Error creating chart: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void handleBackToMetrics(ActionEvent event) {
        UserSession.getInstance().setCurrentStyle(currentStyle);
            new Thread(() -> {
                try {
                // Load the metrics scene
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/ad_auction_dashboard/fxml/MetricScene2.fxml"));
                Parent root = loader.load();

                // Get the controller and pass the campaign metrics
                MetricSceneController controller = loader.getController();
                controller.setMetrics(campaignMetrics);

                // Switch to the metrics scene
                Scene scene = new Scene(root);
                // Use primaryChartContainer instead of chartContainer to get the scene
                Platform.runLater(() -> {
                    Stage stage = (Stage) primaryChartContainer.getScene().getWindow();
                    scene.getStylesheets().add(currentStyle);
                    stage.setScene(scene);
                    stage.show();
                });
            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Error returning to metrics view: " + e.getMessage());
            }
            }).start();
    }

    @FXML
    private void exportData(ActionEvent actionEvent){
        if (exportComboBox.getValue().equals("Chart 1 PNG")){
            File selectedFile = getChosenFile("png");
            WritableImage image = primaryChart.snapshot(new SnapshotParameters(), null);

            new Thread(() -> {
                try{
                    ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", selectedFile);
                } catch (IOException e){
                    printLabel.setText("Writing Error!");
                    System.err.println(e);
                }
            }).start();
        }
        else if (secondaryChart != null && exportComboBox.getValue().equals("Chart 2 PNG")){
            File selectedFile = getChosenFile("png");
            WritableImage image = secondaryChart.snapshot(new SnapshotParameters(), null);
            new Thread(() -> {
                try{
                    ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", selectedFile);
                } catch (IOException e){
                    printLabel.setText("Writing Error!");
                    System.err.println(e);
                }
            }).start();
        }
        else if (exportComboBox.getValue().equals("Chart 1 CSV")){
            File selectedFile = getChosenFile("csv");
            new Thread(() -> {
                try {
                    FileWriter fileWriter = new FileWriter(selectedFile);
                    CSVWriter writer = new CSVWriter(fileWriter);
                    writer.writeNext(new String[]{"X","Y"});
                    XYChart.Series<String, Number> series;
                    for (int i = 0; i < primaryChart.getData().size(); i++) {
                        series = (XYChart.Series<String, Number>) primaryChart.getData().get(i);
                        for (XYChart.Data<String, Number> dataPoint : series.getData()) {
                            String xValue = dataPoint.getXValue();
                            Number yValue = dataPoint.getYValue();
                            writer.writeNext(new String[]{xValue, yValue.toString()});
                        }
                    }
                    writer.close();
                } catch (Exception e){
                    printLabel.setText("Writing Error!");
                    System.err.println(e);
                }
            });
        }
        else if (secondaryChart != null && exportComboBox.getValue().equals("Chart 2 CSV")){
            File selectedFile = getChosenFile("csv");
            new Thread(() -> {
                try {
                    FileWriter fileWriter = new FileWriter(selectedFile);
                    CSVWriter writer = new CSVWriter(fileWriter);
                    writer.writeNext(new String[]{"X","Y"});
                    XYChart.Series<String, Number> series;
                    for (int i = 0; i < secondaryChart.getData().size(); i++) {
                        series = (XYChart.Series<String, Number>) secondaryChart.getData().get(i);
                        for (XYChart.Data<String, Number> dataPoint : series.getData()) {
                            String xValue = dataPoint.getXValue();
                            Number yValue = dataPoint.getYValue();
                            writer.writeNext(new String[]{xValue, yValue.toString()});
                        }
                    }
                    writer.close();
                } catch (Exception e){
                    printLabel.setText("Writing Error!");
                    System.err.println(e);
                }
            }).start();
        }
        else if (exportComboBox.getValue().equals("Chart 1 PDF")){
            File selectedFile = getChosenFile("pdf");
            WritableImage writableImage = primaryChart.snapshot(new SnapshotParameters(), null);
            new Thread(() -> {
                try {
                    ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
                    ImageIO.write(SwingFXUtils.fromFXImage(writableImage, null), "png", byteOutput);
                    Image graph = Image.getInstance(byteOutput.toByteArray());
                    graph.scaleToFit(PageSize.A4.getHeight(), PageSize.A4.getWidth());
                    Document document = new Document(PageSize.A4.rotate(), 0, 0, 0, 0);
                    PdfWriter.getInstance(document, new FileOutputStream(selectedFile));
                    document.open();
                    document.add(graph);
                    document.close();
                } catch (Exception e){
                    printLabel.setText("Writing Error!");
                    System.err.println(e);
                }
            }).start();
        }
        else if (secondaryChart != null && exportComboBox.getValue().equals("Chart 2 PDF")){
            File selectedFile = getChosenFile("pdf");
            WritableImage writableImage = secondaryChart.snapshot(new SnapshotParameters(), null);
            new Thread(() -> {
                try {
                    ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
                    ImageIO.write(SwingFXUtils.fromFXImage(writableImage, null), "png", byteOutput);
                    Image graph = Image.getInstance(byteOutput.toByteArray());
                    graph.scaleToFit(PageSize.A4.getHeight(), PageSize.A4.getWidth());
                    Document document = new Document(PageSize.A4.rotate(), 0, 0, 0, 0);
                    PdfWriter.getInstance(document, new FileOutputStream(selectedFile));
                    document.open();
                    document.add(graph);
                    document.close();
                } catch (Exception e){
                    printLabel.setText("Writing Error!");
                    System.err.println(e);
                }
            }).start();
        }
        else if (secondaryChart != null && exportComboBox.getValue().equals("Combined PDF")){
            File selectedFile = getChosenFile("pdf");
            WritableImage writableImage = primaryChart.snapshot(new SnapshotParameters(), null);
            WritableImage writableImage2 = secondaryChart.snapshot(new SnapshotParameters(), null);
            new Thread(() -> {
                try {
                    ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
                    ImageIO.write(SwingFXUtils.fromFXImage(writableImage, null), "png", byteOutput);
                    Image graph = Image.getInstance(byteOutput.toByteArray());
                    graph.scaleToFit(PageSize.A4.getHeight(), PageSize.A4.getWidth());
                    Document document = new Document(PageSize.A4.rotate(), 0, 0, 0, 0);
                    PdfWriter.getInstance(document, new FileOutputStream(selectedFile));
                    document.open();
                    document.add(graph);
                    byteOutput.reset();
                    ImageIO.write(SwingFXUtils.fromFXImage(writableImage2, null), "png", byteOutput);
                    graph = Image.getInstance(byteOutput.toByteArray());
                    graph.scaleToFit(PageSize.A4.getHeight(), PageSize.A4.getWidth());
                    document.add(graph);
                    document.close();
                } catch (Exception e){
                    printLabel.setText("Writing Error!");
                    System.err.println(e);
                }
            }).start();
        } else if (secondaryChart == null && (exportComboBox.getValue().equals("Chart 2 PNG") || exportComboBox.getValue().equals("Chart 2 PDF") || exportComboBox.getValue().equals("Combined PDF"))){
            printLabel.setText("Secondary Chart doesn't Exist!");
        } else {
            printLabel.setText("Unknown Error!");
        }
    }

    @FXML
    private void printData(ActionEvent actionEvent){
        if (exportComboBox.getValue().equals("Chart 1 PNG") || exportComboBox.getValue().equals("Chart 1 PDF")){
            java.awt.Image graph = getPrintableImage(primaryChart.snapshot(new SnapshotParameters(), null));
            new Thread(() -> {

                try {
                    PrinterJob printJob = PrinterJob.getPrinterJob();
                    printJob.setPrintable(new Printable() {
                        @Override
                        public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
                            if (pageIndex != 0){return NO_SUCH_PAGE;}
                            graphics.drawImage(graph, 0, 0, (int) graph.getWidth(null), (int) ((int) graph.getHeight(null) * 0.8), null);
                            return PAGE_EXISTS;}});
                    boolean doPrint = printJob.printDialog();
                    Platform.runLater(() -> {printLabel.setText("Trying to create Print Job");togglePrint(true);});
                    if (doPrint){try {printJob.print();
                        Platform.runLater(() -> {printLabel.setText("Successfully Printed");togglePrint(false);});
                    } catch (PrinterException e1) {Platform.runLater(() -> printLabel.setText("Print Error! Please Try Again"));e1.printStackTrace();togglePrint(false);}
                    }else {
                        Platform.runLater(() -> togglePrint(false));
                    }
                } catch (Exception e){
                    Platform.runLater(() -> printLabel.setText("Print Error! Please Try Again"));System.err.println(e);}
            }).start();
        }
        else if (secondaryChart != null && (exportComboBox.getValue().equals("Chart 2 PNG") || exportComboBox.getValue().equals("Chart 2 PDF"))){
            java.awt.Image graph = getPrintableImage(secondaryChart.snapshot(new SnapshotParameters(), null));
            new Thread(() -> {
                try {
                    PrinterJob printJob = PrinterJob.getPrinterJob();
                    printJob.setPrintable(new Printable() {
                        @Override
                        public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
                            if (pageIndex != 0){return NO_SUCH_PAGE;}
                            graphics.drawImage(graph, 0, 0, (int) graph.getWidth(null), (int) ((int) graph.getHeight(null) * 0.8), null);
                            return PAGE_EXISTS;}});
                    boolean doPrint = printJob.printDialog();
                    Platform.runLater(() -> {printLabel.setText("Trying to create Print Job");togglePrint(true);});
                    if (doPrint){try {printJob.print();
                        Platform.runLater(() -> {printLabel.setText("Successfully Printed");togglePrint(false);});
                    } catch (PrinterException e1) {Platform.runLater(() -> printLabel.setText("Print Error! Please Try Again"));e1.printStackTrace();togglePrint(false);}
                    }else {
                        Platform.runLater(() -> togglePrint(false));
                    }
                } catch (Exception e){
                    Platform.runLater(() -> printLabel.setText("Print Error! Please Try Again"));System.err.println(e);}
            }).start();
        }
        else if (secondaryChart != null && exportComboBox.getValue().equals("Combined PDF")){
            java.awt.Image graph1 = getPrintableImage(primaryChart.snapshot(new SnapshotParameters(), null));
            java.awt.Image graph2 = getPrintableImage(secondaryChart.snapshot(new SnapshotParameters(), null));
            new Thread(() -> {
                try {
                    java.awt.Image[] graphs = {graph1, graph2};
                    PrinterJob printJob = PrinterJob.getPrinterJob();
                    printJob.setPrintable(new Printable() {
                        @Override
                        public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
                            if (pageIndex >= graphs.length){return NO_SUCH_PAGE;}
                            graphics.drawImage(graphs[pageIndex], 0, 0, (int) graphs[pageIndex].getWidth(null), (int) ((int) graphs[pageIndex].getHeight(null) * 0.9), null);
                            return PAGE_EXISTS;}});
                    boolean doPrint = printJob.printDialog();
                    Platform.runLater(() -> {printLabel.setText("Trying to create Print Job");
                    togglePrint(true);});
                    if (doPrint){try {printJob.print();
                        Platform.runLater(() -> {printLabel.setText("Successfully Printed");
                        togglePrint(false);});
                    } catch (PrinterException e1) {Platform.runLater(() -> printLabel.setText("Print Error! Please Try Again"));e1.printStackTrace();togglePrint(false);}
                    }else {
                        Platform.runLater(() -> togglePrint(false));
                    }
                } catch (Exception e){
                    Platform.runLater(() -> printLabel.setText("Print Error! Please Try Again"));System.err.println(e);}
            }).start();
        } else if (secondaryChart == null && (exportComboBox.getValue().equals("Chart 2 PNG") || exportComboBox.getValue().equals("Chart 2 PDF"))){
            Platform.runLater(() -> printLabel.setText("Secondary Chart doesn't Exist!"));
        }else {
            Platform.runLater(() -> printLabel.setText("CSV cannot be printed!"));
        }
    }

    public BufferedImage rotateImage(java.awt.Image originalImage, double degrees) {
        // Convert to BufferedImage if it's not already one
        BufferedImage bufferedImage;
        if (originalImage instanceof BufferedImage) {
            bufferedImage = (BufferedImage) originalImage;
        } else {
            // Create a BufferedImage with transparency
            bufferedImage = new BufferedImage(
                originalImage.getWidth(null),
                originalImage.getHeight(null),
                BufferedImage.TYPE_INT_ARGB
            );

            // Draw the image on the BufferedImage
            Graphics2D bGr = bufferedImage.createGraphics();
            bGr.drawImage(originalImage, 0, 0, null);
            bGr.dispose();
        }

        // Calculate the new image size
        double radians = Math.toRadians(degrees);
        double sin = Math.abs(Math.sin(radians));
        double cos = Math.abs(Math.cos(radians));
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        int newWidth = (int) Math.floor(width * cos + height * sin);
        int newHeight = (int) Math.floor(height * cos + width * sin);

        // Create a new BufferedImage for the rotated image
        BufferedImage rotatedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = rotatedImage.createGraphics();

        // Transform to rotate around the center of the image
        AffineTransform at = new AffineTransform();
        at.translate((newWidth - width) / 2, (newHeight - height) / 2);
        at.rotate(radians, width / 2, height / 2);
        g2d.setTransform(at);

        // Draw the original image
        g2d.drawImage(bufferedImage, 0, 0, null);
        g2d.dispose();

        return rotatedImage;
    }

    private java.awt.Image getPrintableImage(WritableImage writableImage){
        try {
            ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
            ImageIO.write(SwingFXUtils.fromFXImage(writableImage, null), "png", byteOutput);
            InputStream inputStream = new ByteArrayInputStream(byteOutput.toByteArray());
            return rotateImage(ImageIO.read(inputStream).getScaledInstance(-1, -1, java.awt.Image.SCALE_SMOOTH),90);
        } catch (Exception e){
            printLabel.setText("Error Converting Image!");
            return null;
        }

    }

    private File getChosenFile(String fileType){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(fileType.toUpperCase() + " file", "*." + fileType));
        File selectedFile = fileChooser.showSaveDialog(primaryChart.getScene().getWindow());
        if (!FilenameUtils.getExtension(selectedFile.getName()).equalsIgnoreCase(fileType)){
            selectedFile = new File(selectedFile.getParentFile(), FilenameUtils.getBaseName(selectedFile.getName())+"." + fileType);
        }
        if (selectedFile == null){printLabel.setText("No File Selected");}
        return selectedFile;
    }

    @FXML
    private void toggleColour(ActionEvent event){
        if (colourSwitch.isSelected()){
            currentStyle = this.getClass().getClassLoader().getResource("styles/lightStyle.css").toString();
            colourSwitch.getScene().getStylesheets().clear();
            colourSwitch.getScene().getStylesheets().add(currentStyle);
        } else {
            currentStyle = this.getClass().getClassLoader().getResource("styles/style.css").toString();
            colourSwitch.getScene().getStylesheets().clear();
            colourSwitch.getScene().getStylesheets().add(currentStyle);
        }
    }

    private void toggleControls(Boolean bool){
        exportButton.setDisable(bool);
        printButton.setDisable(bool);
        backButton.setDisable(bool);
        primaryChartTypeComboBox.setDisable(bool);
        secondaryChartTypeComboBox.setDisable(bool);
    }

    public void toggleFilters(Boolean bool){
        contextFilterComboBox.setDisable(bool);
        genderFilterComboBox.setDisable(bool);
        ageFilterComboBox.setDisable(bool);
        incomeFilterComboBox.setDisable(bool);
        startDatePicker.setDisable(bool);
        endDatePicker.setDisable(bool);
        resetFiltersButton.setDisable(bool);
        timeGranularityComboBox.setDisable(bool);
    }

    public void togglePrint(Boolean bool){
        printButton.setDisable(bool);
        exportButton.setDisable(bool);
    }
}