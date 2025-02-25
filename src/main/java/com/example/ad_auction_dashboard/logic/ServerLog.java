package com.example.ad_auction_dashboard.logic;

public class ServerLog implements Log{
    private LogDate entryDate;
    private String id;
    private LogDate exitDate;
    private Integer pagesViewed;
    private Boolean conversion;

    public ServerLog(String entryDate, String id, String exitDate, String pagesViewed, String conversion){

    }

    public void setEntryDate(LogDate entryDate) {
        this.entryDate = entryDate;
    }

    public LogDate getEntryDate() {
        return entryDate;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setExitDate(LogDate exitDate) {
        this.exitDate = exitDate;
    }

    public LogDate getExitDate() {
        return exitDate;
    }

    public void setPagesViewed(Integer pagesViewed) {
        this.pagesViewed = pagesViewed;
    }

    public Integer getPagesViewed() {
        return pagesViewed;
    }

    public void setConversion(Boolean conversion) {
        this.conversion = conversion;
    }

    public Boolean getConversion() {
        return conversion;
    }
}
