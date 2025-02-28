package com.example.ad_auction_dashboard.logic;

public class ServerLog implements LogFile {
    private LogDate entryDate;
    private String id;
    private LogDate exitDate;
    private Integer pagesViewed;
    private Boolean conversion;

    public ServerLog(String entryDate, String id, String exitDate, String pagesViewed, String conversion){
        this.setEntryDate(entryDate);
        this.setId(id);
        this.setExitDate(exitDate);
        this.setPagesViewed(pagesViewed);
        this.setConversion(conversion);
    }

    public void setEntryDate(String entryDate) {
        this.entryDate = LogFile.convertDate(entryDate);
    }

    public LogDate getEntryDate() {
        return entryDate;
    }

    public void setId(String id) {
        if (id.matches("[0-9]*")){
            this.id = id;
        }
    }

    public String getId() {
        return id;
    }

    public void setExitDate(String exitDate) {
        this.entryDate = LogFile.convertDate(exitDate);
    }

    public LogDate getExitDate() {
        return exitDate;
    }

    public void setPagesViewed(String pagesViewed) {
        if (pagesViewed.matches("[0-9]+")){
            this.pagesViewed = Integer.parseInt(pagesViewed);
        }
    }

    public Integer getPagesViewed() {
        return pagesViewed;
    }

    public void setConversion(String conversion) {
        switch (conversion){
            case "Yes":
                this.conversion = Boolean.TRUE;
                break;
            case "No":
                this.conversion = Boolean.FALSE;
                break;
            default:
                break;
        }
    }

    public Boolean getConversion() {
        return conversion;
    }
}
