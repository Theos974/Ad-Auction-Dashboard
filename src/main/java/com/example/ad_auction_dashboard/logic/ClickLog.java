package com.example.ad_auction_dashboard.logic;

public class ClickLog implements LogFile {
    private LogDate date;
    private String id;
    private Float clickCost;

    public ClickLog(String date, String id, String clickCost){

    }

    public void setDate(String date) {
        this.date = LogFile.convertDate(date);
    }

    public LogDate getDate() {
        return date;
    }

    public void setId(String id) {
        if (id.matches("[0-9]*")){
            this.id = id;
        }
    }

    public String getId() {
        return id;
    }

    public void setClickCost(String clickCost) {
        if (clickCost.matches("[0.9]+\\.[0.9]{6}")){
            this.clickCost = Float.parseFloat(clickCost);
        }
    }

    public Float getClickCost() {
        return clickCost;
    }
}
