package com.example.ad_auction_dashboard.controller;

import com.example.ad_auction_dashboard.charts.ClickCostHistogramGenerator;
import com.example.ad_auction_dashboard.charts.HistogramGenerator;
import com.example.ad_auction_dashboard.logic.CampaignMetrics;
import com.example.ad_auction_dashboard.logic.LogoutHandler;
import com.example.ad_auction_dashboard.logic.TimeFilteredMetrics;
import com.example.ad_auction_dashboard.logic.UserSession;

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
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfWriter;
import com.opencsv.CSVWriter;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;

public class HistogramController {

    @FXML
    private ComboBox<String> histogramTypeComboBox;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private Slider binSizeSlider;

    @FXML
    private Label binSizeLabel;

    @FXML
    private BarChart<String, Number> histogramChart;

    @FXML
    private ComboBox<String> exportComboBox;

    @FXML
    private Label printLabel;

    @FXML
    private CategoryAxis xAxis;

    @FXML
    private NumberAxis yAxis;

    @FXML
    private TextArea descriptionTextArea;

    @FXML
    private Label statusLabel; // Optional: can add this to your FXML for status messages

    private CampaignMetrics campaignMetrics;
    private final Map<String, HistogramGenerator> histogramGenerators = new HashMap<>();

    @FXML
    private Label histogramTitleLabel;

    @FXML
    private Button logoutBtn;
    @FXML
    private ComboBox<String> genderFilterComboBox;

    @FXML
    private ComboBox<String> contextFilterComboBox;

    @FXML
    private Button resetFiltersButton;

    // Add a field for TimeFilteredMetrics
    private TimeFilteredMetrics timeFilteredMetrics;

    @FXML
    public void initialize() {
        // Register histogram generators
        registerHistogramGenerators();

        // Fill histogram type combo box
        histogramTypeComboBox.getItems().addAll(histogramGenerators.keySet());

        // Set default to Click Cost (currently the only required histogram)
        histogramTypeComboBox.setValue("Click Cost");

        // Configure bin size slider
        binSizeSlider.setMin(5);
        binSizeSlider.setMax(20);
        binSizeSlider.setValue(10);

        // Set the initial bin size label
        updateBinSizeLabel((int)binSizeSlider.getValue());

        // Add listener for when the slider value changes
        binSizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            // Ensure we're working with integers and update the label properly
            int binCount = (int)Math.round(newVal.doubleValue());
            updateBinSizeLabel(binCount);

            // Debug output to trace value changes
            System.out.println("Slider raw value: " + newVal + ", Rounded bin count: " + binCount);

            updateHistogram();
        });

        // Set up event handlers with date validation
        histogramTypeComboBox.setOnAction(e -> updateHistogram());

        // Add validation when changing dates
        startDatePicker.setOnAction(e -> {
            validateDateRange();
            updateHistogram();
        });

        endDatePicker.setOnAction(e -> {
            validateDateRange();
            updateHistogram();
        });

        // Ensure the text area is initialized with empty text instead of placeholder
        if (descriptionTextArea != null) {
            descriptionTextArea.setText("");
        }
        if (genderFilterComboBox != null) {
            genderFilterComboBox.getItems().addAll("All", "Male", "Female");
            genderFilterComboBox.setValue("All");
            genderFilterComboBox.setOnAction(e -> updateHistogram());
        }

        if (contextFilterComboBox != null) {
            contextFilterComboBox.getItems().addAll("All", "News", "Shopping", "Social Media",
                "Blog", "Hobbies", "Travel");
            contextFilterComboBox.setValue("All");
            contextFilterComboBox.setOnAction(e -> updateHistogram());
        }
        if (exportComboBox != null){
            exportComboBox.getItems().addAll("PNG", "CSV", "PDF");
            exportComboBox.setValue("PNG");
        }
    }

    /**
     * Validates and corrects the date range if needed
     */
    private void validateDateRange() {
        if (startDatePicker.getValue() != null && endDatePicker.getValue() != null) {
            if (endDatePicker.getValue().isBefore(startDatePicker.getValue())) {
                // End date is before start date, correct it
                endDatePicker.setValue(startDatePicker.getValue());

                // Show alert to user
                showAlert("Invalid date range detected. End date has been adjusted to match start date.");
            }
        }
    }

    /**
     * Shows an alert to the user
     */
    private void showAlert(String message) {
        // First check if there's a status label to display the message
        if (statusLabel != null) {
            statusLabel.setText(message);
        } else {
            // Otherwise show an alert dialog
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        }
    }

    /**
     * Helper method to update the bin size label with consistent formatting
     */
    private void updateBinSizeLabel(int binCount) {
        if (binSizeLabel != null) {
            binSizeLabel.setText(String.format("Num: %d", binCount));
        }
    }

    /**
     * Register all available histogram generators
     */
    private void registerHistogramGenerators() {
        // Register ClickCost histogram generator (the only one required for now)
        histogramGenerators.put("Click Cost", new ClickCostHistogramGenerator());

        // For future expansion, add more histogram generators here
    }

    public void setCampaignMetrics(CampaignMetrics metrics) {
        this.campaignMetrics = metrics;

        // Create TimeFilteredMetrics for filtering
        this.timeFilteredMetrics = new TimeFilteredMetrics(
            metrics.getImpressionLogs(),
            metrics.getServerLogs(),
            metrics.getClickLogs(),
            metrics.getBouncePagesThreshold(),
            metrics.getBounceSecondsThreshold()
        );

        // Apply filter settings from UserSession
        applyFilterSettingsFromSession();

        // Get campaign date boundaries
        LocalDateTime campaignStart = metrics.getCampaignStartDate();
        LocalDateTime campaignEnd = metrics.getCampaignEndDate();

        if (campaignStart != null && campaignEnd != null) {
            LocalDate startDate = campaignStart.toLocalDate();
            LocalDate endDate = campaignEnd.toLocalDate();

            // Set default values to campaign start and end dates
            startDatePicker.setValue(startDate);
            endDatePicker.setValue(endDate);

            // Set date range constraints for start date picker
            startDatePicker.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    setDisable(empty || date.isBefore(startDate) || date.isAfter(endDate));
                }
            });

            // Set date range constraints for end date picker
            endDatePicker.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    setDisable(empty || date.isBefore(startDate) || date.isAfter(endDate));
                }
            });
        }

        // Generate initial histogram - done after all setup
        updateHistogram();
    }

    @FXML
    private void handleResetFilters() {
        if (genderFilterComboBox != null) genderFilterComboBox.setValue("All");
        if (contextFilterComboBox != null) contextFilterComboBox.setValue("All");

        if (timeFilteredMetrics != null) {
            timeFilteredMetrics.setGenderFilter(null);
            timeFilteredMetrics.setContextFilter(null);

            // Clear filters in session
            UserSession.getInstance().clearFilterSettings();

            // Update histogram with reset filters
            updateHistogram();
        }
    }

    public void setHistogramType(String histogramType) {
        if (histogramGenerators.containsKey(histogramType)) {
            histogramTypeComboBox.setValue(histogramType);
            updateHistogram();
        }
    }

    private void updateHistogram() {
        if (campaignMetrics == null) return;

        if (timeFilteredMetrics != null) {
            // Get filter values
            String gender =
                (genderFilterComboBox != null) ? genderFilterComboBox.getValue() : "All";
            String context =
                (contextFilterComboBox != null) ? contextFilterComboBox.getValue() : "All";

            // Apply filters
            timeFilteredMetrics.setGenderFilter(gender.equals("All") ? null : gender);
            timeFilteredMetrics.setContextFilter(context.equals("All") ? null : context);

            // Save filter settings to UserSession
            saveFilterSettingsToSession();
        }

        // Get date range
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        if (startDate == null || endDate == null) return;

        // Additional validation to ensure start date is not after end date
        if (startDate.isAfter(endDate)) {
            endDatePicker.setValue(startDate);
            endDate = startDate;
            showAlert(
                "Start date cannot be after end date. Both dates have been set to the same day.");
            return; // Skip processing to let the user see the message
        }

        // Validate date range against campaign boundaries
        if (campaignMetrics.getCampaignStartDate() != null &&
            campaignMetrics.getCampaignEndDate() != null) {
            LocalDate campaignStartDate = campaignMetrics.getCampaignStartDate().toLocalDate();
            LocalDate campaignEndDate = campaignMetrics.getCampaignEndDate().toLocalDate();

            // Ensure dates are within campaign range
            if (startDate.isBefore(campaignStartDate)) {
                startDate = campaignStartDate;
                startDatePicker.setValue(startDate);
                showAlert("Start date adjusted to campaign start date.");
            }

            if (endDate.isAfter(campaignEndDate)) {
                endDate = campaignEndDate;
                endDatePicker.setValue(endDate);
                showAlert("End date adjusted to campaign end date.");
            }
        }

        // Convert to LocalDateTime
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        // Get selected histogram type
        String selectedType = histogramTypeComboBox.getValue();
        HistogramGenerator generator = histogramGenerators.get(selectedType);

        if (generator != null) {
            // Update chart labels - ensure they're checked for null
            if (histogramTitleLabel != null) {
                histogramTitleLabel.setText(generator.getTitle());
                // Ensure the title is visible
                histogramTitleLabel.setVisible(true);
            }

            if (xAxis != null) {
                xAxis.setLabel(generator.getXAxisLabel());
            }

            if (yAxis != null) {
                yAxis.setLabel(generator.getYAxisLabel());
            }

            // Ensure description is visible and set
            if (descriptionTextArea != null) {
                descriptionTextArea.setText(generator.getDescription());
                descriptionTextArea.setVisible(true);
            }

            // Get bin count from slider with proper rounding
            int binCount = (int) Math.round(binSizeSlider.getValue());

            try {
                // Calculate histogram data using the FILTERED method
                Map<String, Integer> histogramData;

                // Check if filters are applied
                if (timeFilteredMetrics != null &&
                    (!genderFilterComboBox.getValue().equals("All") ||
                        !contextFilterComboBox.getValue().equals("All"))) {

                    // Use the filtered method when filters are active
                    histogramData =
                        ((ClickCostHistogramGenerator) generator).generateFilteredHistogramData(
                            campaignMetrics, timeFilteredMetrics, start, end, binCount);
                } else {
                    // Use standard method when no filters are active
                    histogramData = generator.generateHistogramData(
                        campaignMetrics, start, end, binCount);
                }

                // Update the chart
                histogramChart.getData().clear();
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName("Frequency");

                // Check if there's actual data
                if (histogramData.isEmpty() ||
                    (histogramData.size() == 1 &&
                        (histogramData.containsKey("No data available") ||
                            histogramData.containsKey("No data in selected range") ||
                            histogramData.containsKey(
                                "No data in selected range or with selected filters")))) {
                    // Add placeholder for no data
                    series.getData().add(new XYChart.Data<>("No data available", 0));
                } else {
                    // Add real histogram data
                    for (Map.Entry<String, Integer> entry : histogramData.entrySet()) {
                        series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
                    }
                }

                histogramChart.getData().add(series);

                // Apply styling to the bars to make them green
                for (XYChart.Data<String, Number> data : series.getData()) {
                    if (data.getNode() != null) {
                        data.getNode().setStyle("-fx-bar-fill: #4CAF50;");
                    }
                }

                // Set y-axis to start at 0
                if (yAxis != null) {
                    yAxis.setForceZeroInRange(true);
                }

                // Improve x-axis label display
                if (xAxis != null) {
                    xAxis.setTickLabelRotation(45);
                }

            } catch (Exception e) {
                // Handle errors gracefully
                e.printStackTrace();
                if (descriptionTextArea != null) {
                    descriptionTextArea.setText("Error generating histogram: " + e.getMessage() +
                        "\nPlease try different date ranges or bin sizes.");
                }
            }
        }
    }
    private void saveFilterSettingsToSession() {
        UserSession session = UserSession.getInstance();

        if (genderFilterComboBox != null) {
            session.setFilterSetting("gender", genderFilterComboBox.getValue());
        }

        if (contextFilterComboBox != null) {
            session.setFilterSetting("context", contextFilterComboBox.getValue());
        }
    }

    // Method to apply filter settings from UserSession
    private void applyFilterSettingsFromSession() {
        UserSession session = UserSession.getInstance();

        // Apply gender filter if saved
        String gender = session.getFilterSetting("gender");
        if (gender != null && genderFilterComboBox != null) {
            genderFilterComboBox.setValue(gender);
        }

        // Apply context filter if saved
        String context = session.getFilterSetting("context");
        if (context != null && contextFilterComboBox != null) {
            contextFilterComboBox.setValue(context);
        }
    }

    @FXML
    private void handleBackToMetrics(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/ad_auction_dashboard/fxml/MetricScene2.fxml"));
            Parent root = loader.load();

            // Get the controller and pass the campaign metrics
            MetricSceneController controller = loader.getController();
            controller.setMetrics(campaignMetrics);

            // Switch to the metrics scene
            Scene scene = new Scene(root);
            Stage stage = (Stage) histogramChart.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        if (logoutBtn != null) {
            LogoutHandler.handleLogout(event);
        }
    }

    @FXML
    private void exportData(ActionEvent event){
        if (exportComboBox.getValue().equals("PNG")){
            File selectedFile = getChosenFile("png");
            WritableImage image = histogramChart.snapshot(new SnapshotParameters(), null);

            try{
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", selectedFile);
            } catch (IOException e){
                printLabel.setText("Writing Error!");
                System.err.println(e);
            }
        }else if (exportComboBox.getValue().equals("CSV")){
            File selectedFile = getChosenFile("csv");
            try {
                FileWriter fileWriter = new FileWriter(selectedFile);
                CSVWriter writer = new CSVWriter(fileWriter);
                writer.writeNext(new String[]{"X","Y"});
                XYChart.Series<String, Number> series;
                for (int i = 0; i < histogramChart.getData().size(); i++) {
                    series = (XYChart.Series<String, Number>) histogramChart.getData().get(i);
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
        }else if (exportComboBox.getValue().equals("PDF")){
            File selectedFile = getChosenFile("pdf");
            try {
                WritableImage writableImage = histogramChart.snapshot(new SnapshotParameters(), null);
                ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
                ImageIO.write(SwingFXUtils.fromFXImage(writableImage, null), "png", byteOutput);
                com.itextpdf.text.Image graph = com.itextpdf.text.Image.getInstance(byteOutput.toByteArray());
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
        }
    }

    @FXML void printData(ActionEvent event){
        if (exportComboBox.getValue().equals("PNG") || exportComboBox.getValue().equals("PDF")){
            try {
                java.awt.Image graph = getPrintableImage(histogramChart.snapshot(new SnapshotParameters(), null));
                PrinterJob printJob = PrinterJob.getPrinterJob();
                printJob.setPrintable(new Printable() {
                    @Override
                    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
                        if (pageIndex != 0){return NO_SUCH_PAGE;}
                        graphics.drawImage(graph, 0, 0, (int) graph.getWidth(null), (int) ((int) graph.getHeight(null) * 0.8), null);
                        return PAGE_EXISTS;}});
                boolean doPrint = printJob.printDialog();
                if (doPrint){try {printJob.print();
                } catch (PrinterException e1) {printLabel.setText("Print Error! Please Try Again");e1.printStackTrace();}
                }
            } catch (Exception e){
                printLabel.setText("Print Error! Please Try Again");System.err.println(e);}
        } else if (exportComboBox.getValue().equals("CSV")){
            printLabel.setText("CSV cannot be printed!");
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
        File selectedFile = fileChooser.showSaveDialog(histogramChart.getScene().getWindow());
        if (!FilenameUtils.getExtension(selectedFile.getName()).equalsIgnoreCase(fileType)){
            selectedFile = new File(selectedFile.getParentFile(), FilenameUtils.getBaseName(selectedFile.getName())+"." + fileType);
        }
        if (selectedFile == null){printLabel.setText("No File Selected");}
        return selectedFile;
    }
}