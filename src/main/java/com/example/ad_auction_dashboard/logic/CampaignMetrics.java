package com.example.ad_auction_dashboard.logic;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
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
        this.cpc = calculateCPC(totalCost,numberOfClicks);
        this.cpa = calculateCPA(numberOfConversions, totalCost);
        this.cpm = calculateCPM(totalCost,numberOfImpressions);
        this.bounceRate = calculateBounceRate(numberOfClicks,numberOfBounces);

    }

    private int calculateImpressions(ImpressionLog[] imps){
        if (imps == null) return 0;
        return imps.length;

    }

    private int calculateClicks(ClickLog[] cls){
        if (cls == null) return 0;
        return cls.length;    }

    private int calculateUniques(ClickLog[] cls){
        if (cls==null) return 0;

        Set<String> uniqueUserIds = new HashSet<>();
        for (ClickLog c : cls){
            uniqueUserIds.add(c.getId());
        }

        return  uniqueUserIds.size();
    }


    private void calculateNumBounces(ServerLog[] srv ){

        if (srv==null) return;

        for (ServerLog s: srv){
            LogDate entryLd = s.getEntryDate();
            LogDate exitLd  = s.getExitDate();

            if (entryLd == null || exitLd == null || !entryLd.getExists() || !exitLd.getExists()) {
                continue;
            }

            // Convert LogDate objects to LocalDateTime
            LocalDateTime entry = LocalDateTime.of(
                entryLd.getYear(), entryLd.getMonth(), entryLd.getDay(),
                entryLd.getHour(), entryLd.getMinute(), entryLd.getSecond()
            );
            LocalDateTime exit = LocalDateTime.of(
                exitLd.getYear(), exitLd.getMonth(), exitLd.getDay(),
                exitLd.getHour(), exitLd.getMinute(), exitLd.getSecond()
            );

            // Calculate the difference in seconds
            long diffSeconds = Duration.between(entry, exit).getSeconds();


            if (s.getPagesViewed() == 1 || diffSeconds <= 4 ){
                this.numberOfBounces++;
            }
        }
    }

    private void calculateConversion(ServerLog[] srv){

        if (srv == null) return;
        for (ServerLog s: srv){
            if (s.getConversion()){
                this.numberOfConversions++;
            }
        }
    }

    private double calculateTotalCost(ImpressionLog[] imps, ClickLog[] cls){
        double totalCost=0;

        if (imps == null && cls ==null) return 0;

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


    public int getNumberOfImpressions() {
        return numberOfImpressions;
    }

    public int getNumberOfClicks() {
        return numberOfClicks;
    }

    public int getNumberOfUniques() {
        return numberOfUniques;
    }

    public int getNumberOfBounces() {
        return numberOfBounces;
    }

    public int getNumberOfConversions() {
        return numberOfConversions;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public double getCTR() {
        return ctr;
    }

    public double getCPC() {
        return cpc;
    }

    public double getCPA() {
        return cpa;
    }

    public double getCPM() {
        return cpm;
    }

    public double getBounceRate() {
        return bounceRate;
    }

}
