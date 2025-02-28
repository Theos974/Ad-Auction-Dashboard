package com.example.ad_auction_dashboard.logic;

public class Campaign {

    private ImpressionLog[] impressionLogs;
    private ClickLog[] clickLogs;
    private ServerLog[] serverLogs;

    public Campaign(ImpressionLog[] impressionLogs, ClickLog[] clickLogs, ServerLog[] serverLogs){
        this.setImpressionLogs(impressionLogs);
        this.setClickLogs(clickLogs);
        this.setServerLogs(serverLogs);
    }

    public void setImpressionLogs(ImpressionLog[] impressionLogs) {
        this.impressionLogs = impressionLogs;
    }

    public ImpressionLog[] getImpressionLogs() {
        return impressionLogs;
    }

    public void setClickLogs(ClickLog[] clickLogs) {
        this.clickLogs = clickLogs;
    }

    public ClickLog[] getClickLogs() {
        return clickLogs;
    }

    public void setServerLogs(ServerLog[] serverLogs) {
        this.serverLogs = serverLogs;
    }

    public ServerLog[] getServerLogs() {
        return serverLogs;
    }
}
