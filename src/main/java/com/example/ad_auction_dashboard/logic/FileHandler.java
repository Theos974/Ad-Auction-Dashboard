package com.example.ad_auction_dashboard.logic;

import com.example.ad_auction_dashboard.controller.StartSceneController;

import java.io.*;
import java.nio.Buffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileHandler {

    private com.example.ad_auction_dashboard.controller.StartSceneController startScene;

    public static void main(String[] args) {
        FileHandler fileHandler = new FileHandler();
        String temp = fileHandler.readFromCsv("src/main/test.csv");

    }

   //process each file line by line rather than entire
    public Campaign openZip(String filePath) {
        List<ImpressionLog> impressionLogs = new ArrayList<>();
        List<ClickLog> clickLogs = new ArrayList<>();
        List<ServerLog> serverLogs = new ArrayList<>();
        int count = 0;

        try (ZipFile zipFile = new ZipFile(filePath)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(zipFile.getInputStream(entry), StandardCharsets.UTF_8))) {

                    // Read header to determine file type
                    String header = reader.readLine();
                    if (header == null) continue;
                    String line;
                    switch (header) {
                        case "Date,ID,Gender,Age,Income,Context,Impression Cost":
                            // Process impression logs line by line
                            while ((line = reader.readLine()) != null) {
                                String[] parts = line.split(",");
                                if (parts.length == 7) {
                                    impressionLogs.add(new ImpressionLog(
                                        parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6]));
                                }
                            }
                            break;

                        case "Entry Date,ID,Exit Date,Pages Viewed,Conversion":
                            // Process server logs line by line
                            while ((line = reader.readLine()) != null) {
                                String[] parts = line.split(",");
                                if (parts.length == 5) {
                                    serverLogs.add(new ServerLog(
                                        parts[0], parts[1], parts[2], parts[3], parts[4]));
                                }
                            }
                            break;

                        case "Date,ID,Click Cost":
                            // Process click logs line by line
                            while ((line = reader.readLine()) != null) {
                                String[] parts = line.split(",");
                                if (parts.length == 3) {
                                    clickLogs.add(new ClickLog(
                                        parts[0], parts[1], parts[2]));
                                }
                            }
                            break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing zip file: " + e.getMessage());
            e.printStackTrace();
            return null;
        }

        return new Campaign(
            impressionLogs.toArray(new ImpressionLog[0]),
            clickLogs.toArray(new ClickLog[0]),
            serverLogs.toArray(new ServerLog[0]));
    }
    public LogFile[] openIndividualCSV(String filePath){
        String file = readFromCsv(filePath);
        LogFile[] logs = new LogFile[0];
        String[] splitFile = splitCsv(file);
        switch (splitFile[0]){
            case "Date,ID,Gender,Age,Income,Context,Impression Cost":
                logs = formatImpressions(splitImpressions(splitFile));
                break;
            case "Entry Date,ID,Exit Date,Pages Viewed,Conversion":
                logs = formatServer(splitServer(splitFile));
                break;
            case "Date,ID,Click Cost":
                logs = formatClick(splitClick(splitFile));
                break;
            default:
                break;
        }
        return logs;
    }

    public String[] readFromZip(String filePath){
        List<String> output = new ArrayList<>();
        try{
            ZipFile zipFile = new ZipFile(filePath);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()){
                ZipEntry entry = entries.nextElement();
                InputStream stream = zipFile.getInputStream(entry);
                ByteArrayOutputStream result = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                for (int length; (length = stream.read(buffer)) != -1; ) {
                    result.write(buffer, 0, length);
                }
                output.add(result.toString(StandardCharsets.UTF_8));
            }
        } catch (Exception e){
            System.err.println(e);
        }
        if ((long) output.size() == 3){
            return output.toArray(new String[0]);
        } else {
            return null;
        }
    }

    public String readFromCsv(String filePath){
        StringBuilder output = new StringBuilder();
        try {
            File file = new File(filePath);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null){
                output.append(line).append("\n");
            }
            output.replace(output.length()-1, output.length(), "");
        } catch (Exception e){
            System.err.println(e);
        }
        return output.toString();
    }
    public String[] splitCsv(String csvFile){
        return csvFile.split("\n");
    }

    public String[][] splitImpressions(String[] splitCsv){
        return genericSplit(splitCsv, 7);
    }
    public String[][] splitClick(String[] splitCsv){
        return genericSplit(splitCsv, 3);
    }
    public String[][] splitServer(String[] splitCsv){
        return genericSplit(splitCsv, 5);
    }

    private String[][] genericSplit(String[] split, Integer splitSize){
        List<String> tempList = Arrays.asList(split);
        List<String> trimmedInput = tempList.subList(1, tempList.size());
        List<String[]> output = new ArrayList<>();
        for (String line: trimmedInput) {
            String[] lineSplit = line.split(",");
            if (Arrays.stream(lineSplit).count() == splitSize){
                output.add(lineSplit);
            }
        }
        if ((long) output.size() == 0){
            return null;
        } else {
            return output.toArray(new String[0][0]);
        }
    }
    public ImpressionLog[] formatImpressions(String[][] splitImpression){
        List<ImpressionLog> output = new ArrayList<>();
        for (String[] log: splitImpression) {
            output.add(new ImpressionLog(log[0], log[1], log[2], log[3], log[4], log[5], log[6]));
        }
        return output.toArray(new ImpressionLog[0]);
    }
    public ClickLog[] formatClick(String[][] splitClick){
        List<ClickLog> output = new ArrayList<>();
        for (String[] log: splitClick) {
            output.add(new ClickLog(log[0], log[1], log[2]));
        }
        return output.toArray(new ClickLog[0]);
    }
    public ServerLog[] formatServer(String[][] splitServer){
        List<ServerLog> output = new ArrayList<>();
        for (String[] log: splitServer) {
            output.add(new ServerLog(log[0], log[1], log[2], log[3], log[4]));
        }
        return output.toArray(new ServerLog[0]);
    }

    public void setStartScene(StartSceneController startScene) {
        this.startScene = startScene;
    }
}
