package com.example.ad_auction_dashboard.logic;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class CampaignMetrics {
    // Cached metric fields
    private int numberOfImpressions;
    private int numberOfClicks;
    private int numberOfUniques;
    private int numberOfBounces;
    private int numberOfConversions;
    private double totalCost;
    private double ctr;   // click-through-rate
    private double cpc;   // cost-per-click
    private double cpa;   // cost-per-acquisition
    private double cpm;   // cost-per-thousand impressions
    private double bounceRate;


    // Possibly store references if needed
    private Campaign campaign;

    public CampaignMetrics(Campaign campaign) {
        this.campaign = campaign;
        computeAllMetrics();
    }

    private void computeAllMetrics() {

        ImpressionLog[] imps = campaign.getImpressionLogs();
        ServerLog[] srv = campaign.getServerLogs();
        ClickLog[] cls = campaign.getClickLogs();

        this.numberOfImpressions = calculateImpressions(imps);
        this.numberOfClicks = calculateClicks(cls);
        this.numberOfUniques = calculateUniques(cls);
        calculateNumBounces(srv);
        calculateConversion(srv);
        this.totalCost = calculateTotalCost(imps,cls);
        this.ctr = calculateCTR(numberOfImpressions,numberOfClicks);
        this.cpc = calculateCPC((int) totalCost,numberOfClicks);
        this.cpa = calculateCPA(numberOfConversions, totalCost);
        this.cpm = calculateCPM(totalCost,numberOfImpressions);
        this.bounceRate = calculateBounceRate(numberOfClicks,numberOfBounces);

    }

    private int calculateImpressions(ImpressionLog[] imps){
        return imps.length;
    }

    private int calculateClicks(ClickLog[] cls){
        return cls.length;
    }

    private int calculateUniques(ClickLog[] cls){
        Set<String> uniqueUserIds = new HashSet<>();
        for (ClickLog c : cls){
            uniqueUserIds.add(c.getId());
        }

        return  uniqueUserIds.size();
    }


    private void calculateNumBounces(ServerLog[] srv ){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


        for (ServerLog s: srv){
            LogDate entryLd = s.getEntryDate();
            LogDate exitLd  = s.getExitDate();
         //  entryLd.
            //if (s.getPagesViewed() == 1 || s.getExitDate().getTime() - )
        }
    }
    private void calculateConversion(ServerLog[] srv){

        for (ServerLog s: srv){
            if (s.getConversion()){
                this.numberOfConversions++;
            }
        }
    }

    private double calculateTotalCost(ImpressionLog[] imps, ClickLog[] cls){
        double totalCost=0;

        for (ImpressionLog i: imps){
            totalCost += i.getImpressionCost();
        }
        for (ClickLog c : cls){
            totalCost += c.getClickCost();
        }

        return totalCost ;
    }

    private double calculateCTR(int imps, int cls){
        if (imps ==0) return 0;

        return (double) cls /imps ;
    }


    private double calculateCPA(int conv, double tCost){
        if (conv==0) return 0;
        return tCost/conv;
    }


    private double calculateCPC(double tCost,int cls){
        if (cls == 0) return 0;
        return tCost/cls;
    }

    private double calculateCPM(double tCost,int imps){

        if (imps == 0) return 0;
        return  (tCost/imps)*100;

    }

    private double calculateBounceRate(int cls, int bounces){

        if (cls==0)return 0;
        return (double) bounces/cls;
    }




    // Getters for the metrics
    public int getNumberOfImpressions() {
        return numberOfImpressions; }
    // ... etc.
}
