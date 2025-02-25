package com.example.ad_auction_dashboard.logic;

public class FileHandler {


    public Campaign openZip(String filePath){
        return null;
    }
    public Log openIndividualCSV(String filePath){
        return null;
    }

    public String[] readFromZip(String filePath){
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
