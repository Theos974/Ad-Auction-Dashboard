package com.example.ad_auction_dashboard.logic;

public class ClickLog implements Log{
    private LogDate date;
    private String id;
    private Float clickCost;

    public ClickLog(String date, String id, String clickCost){

    }

    public void setDate(LogDate date) {
        this.date = date;
    }

    public LogDate getDate() {
        return date;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setClickCost(Float clickCost) {
        this.clickCost = clickCost;
    }

    public Float getClickCost() {
        return clickCost;
    }
}
