package com.example.ad_auction_dashboard.logic;

import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileHandler {


    public Campaign openZip(String filePath){
        return null;
    }
    public LogFile openIndividualCSV(String filePath){
        return null;
    }

    public String[] readFromZip(String filePath){
//        try{
//            ZipFile zipFile = new ZipFile(filePath);
//            Enumeration<? extends ZipEntry> entries = zipFile.entries();
//
//            while (entries.hasMoreElements()){
//                ZipEntry entry = entries.nextElement();
//                InputStream stream = zipFile.getInputStream(entry);
//
//            }
//        } catch (Exception e){
//            System.err.println(e);
//        }
        return null;
    }

    public String readFromCsv(String filePath){
        return null;
    }
    public String[] splitCsv(String csvFile){
        return null;
    }

    public String[][] splitImpressions(String[] splitCsv){
        return null;
    }
    public String[][] splitClick(String[] splitCsv){
        return null;
    }
    public String[][] splitServer(String[] splitCsv){
        return null;
    }
    public ImpressionLog[] formatImpressions(String[][] splitImpression){
        return null;
    }
    public ClickLog[] formatClick(String[][] splitClick){
        return null;
    }
    public ServerLog[] formatServer(String[][] splitServer){
        return null;
    }
}
