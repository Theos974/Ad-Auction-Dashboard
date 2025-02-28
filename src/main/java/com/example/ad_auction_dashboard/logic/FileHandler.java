package com.example.ad_auction_dashboard.logic;

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

    public static void main(String[] args) {
        FileHandler fileHandler = new FileHandler();
        Campaign test = fileHandler.openZip("src/main/test.zip");

    }

    public Campaign openZip(String filePath){
        String[] files = readFromZip(filePath);
        ImpressionLog[] impressionLogs = new ImpressionLog[0];
        ClickLog[] clickLogs = new ClickLog[0];
        ServerLog[] serverLogs = new ServerLog[0];
        for (String file: files) {
            String[] splitFile = splitCsv(file);
            switch (splitFile[0]){
                case "Date,ID,Gender,Age,Income,Context,Impression Cost":
                    impressionLogs = formatImpressions(splitImpressions(splitFile));
                    break;
                case "Entry Date,ID,Exit Date,Pages Viewed,Conversion":
                    serverLogs = formatServer(splitServer(splitFile));
                    break;
                case "Date,ID,Click Cost":
                    clickLogs = formatClick(splitClick(splitFile));
                    break;
                default:
                    break;
            }
        }
        return new Campaign(impressionLogs,clickLogs,serverLogs);
    }
    public LogFile openIndividualCSV(String filePath){
        return null;
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
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));

        } catch (Exception e){
            System.err.println(e);
        }
        return null;
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
}
