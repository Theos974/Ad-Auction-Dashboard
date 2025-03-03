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
    private int bouncePagesThreshold = 1;
    private int bounceSecondsThreshold = 4;
    private final ImpressionLog[] imps;
    private final ServerLog[] srv;
    private final ClickLog[] cls;

    private final LocalDateTime campaignStart;
    private final LocalDateTime getCampaignEnd;


    // Possibly store references if needed
    private Campaign campaign;

    public CampaignMetrics(Campaign campaign) {
        this.campaign = campaign;
         this.imps = campaign.getImpressionLogs();
         this.srv = campaign.getServerLogs();
         this.cls = campaign.getClickLogs();
         this.campaignStart = getCampaignStartDate();
         this.getCampaignEnd = getCampaignEndDate();
         computeAllMetrics();
    }

    private void computeAllMetrics() {


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


            if (s.getPagesViewed() <= bouncePagesThreshold || diffSeconds <= bounceSecondsThreshold ){
                this.numberOfBounces++;
            }
        }
    }

    // Recompute only the bounce-related metrics
    public void recomputeBounceMetrics() {
        // Reset bounce count
        this.numberOfBounces = 0;
        if (srv != null) {
            for (ServerLog s : srv) {
                LogDate entryLd = s.getEntryDate();
                LogDate exitLd  = s.getExitDate();
                if (entryLd == null || exitLd == null || !entryLd.getExists() || !exitLd.getExists()) {
                    continue;
                }
                LocalDateTime entry = LocalDateTime.of(
                    entryLd.getYear(), entryLd.getMonth(), entryLd.getDay(),
                    entryLd.getHour(), entryLd.getMinute(), entryLd.getSecond()
                );
                LocalDateTime exit = LocalDateTime.of(
                    exitLd.getYear(), exitLd.getMonth(), exitLd.getDay(),
                    exitLd.getHour(), exitLd.getMinute(), exitLd.getSecond()
                );
                long diffSeconds = Duration.between(entry, exit).getSeconds();
                if (s.getPagesViewed() <= bouncePagesThreshold || diffSeconds <= bounceSecondsThreshold) {
                    this.numberOfBounces++;
                }
            }
        }
        // Update bounce rate (assuming bounce rate = bounces / clicks)
        if (this.numberOfClicks != 0) {
            this.bounceRate = (double) this.numberOfBounces / this.numberOfClicks;

        } else {
            this.bounceRate = 0;
        }
    }

    public void setBounceCriteria(int pagesThreshold, int secondsThreshold) {
        if (pagesThreshold < 0 || secondsThreshold < 0) {
            throw new IllegalArgumentException("Bounce criteria must be non-negative.");
        }
        this.bouncePagesThreshold = pagesThreshold;
        this.bounceSecondsThreshold = secondsThreshold;
        // If desired, recompute bounce metrics immediately:
        recomputeBounceMetrics();
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

    public LocalDateTime getCampaignStartDate() {
        LocalDateTime earliest = null;

        // Check impressions
        if (imps != null) {
            for (ImpressionLog imp : imps) {
                LogDate ld = imp.getDate();
                if (ld != null && ld.getExists()) {
                    LocalDateTime dt = LocalDateTime.of(ld.getYear(), ld.getMonth(), ld.getDay(),
                        ld.getHour(), ld.getMinute(), ld.getSecond());
                    if (earliest == null || dt.isBefore(earliest)) {
                        earliest = dt;
                    }
                }
            }
        }

        // Check clicks
        if (cls != null) {
            for (ClickLog cl : cls) {
                LogDate ld = cl.getDate();
                if (ld != null && ld.getExists()) {
                    LocalDateTime dt = LocalDateTime.of(ld.getYear(), ld.getMonth(), ld.getDay(),
                        ld.getHour(), ld.getMinute(), ld.getSecond());
                    if (earliest == null || dt.isBefore(earliest)) {
                        earliest = dt;
                    }
                }
            }
        }

        // Check server logs (using entry date)
        if (srv != null) {
            for (ServerLog s : srv) {
                LogDate ld = s.getEntryDate();
                if (ld != null && ld.getExists()) {
                    LocalDateTime dt = LocalDateTime.of(ld.getYear(), ld.getMonth(), ld.getDay(),
                        ld.getHour(), ld.getMinute(), ld.getSecond());
                    if (earliest == null || dt.isBefore(earliest)) {
                        earliest = dt;
                    }
                }
            }
        }
        return earliest;
    }

    public LocalDateTime getCampaignEndDate() {
        LocalDateTime latest = null;

        // Check impressions
        if (imps != null) {
            for (ImpressionLog imp : imps) {
                LogDate ld = imp.getDate();
                if (ld != null && ld.getExists()) {
                    LocalDateTime dt = LocalDateTime.of(ld.getYear(), ld.getMonth(), ld.getDay(),
                        ld.getHour(), ld.getMinute(), ld.getSecond());
                    if (latest == null || dt.isAfter(latest)) {
                        latest = dt;
                    }
                }
            }
        }

        // Check clicks
        if (cls != null) {
            for (ClickLog cl : cls) {
                LogDate ld = cl.getDate();
                if (ld != null && ld.getExists()) {
                    LocalDateTime dt = LocalDateTime.of(ld.getYear(), ld.getMonth(), ld.getDay(),
                        ld.getHour(), ld.getMinute(), ld.getSecond());
                    if (latest == null || dt.isAfter(latest)) {
                        latest = dt;
                    }
                }
            }
        }

        // Check server logs (using entry date)
        if (srv != null) {
            for (ServerLog s : srv) {
                LogDate ld = s.getEntryDate();
                if (ld != null && ld.getExists()) {
                    LocalDateTime dt = LocalDateTime.of(ld.getYear(), ld.getMonth(), ld.getDay(),
                        ld.getHour(), ld.getMinute(), ld.getSecond());
                    if (latest == null || dt.isAfter(latest)) {
                        latest = dt;
                    }
                }
            }
        }
        return latest;
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

    public ImpressionLog[] getImpressionLogs() {
        return imps == null ? null : imps.clone();
    }
    public ClickLog[] getClickLogs() {
        return cls == null ? null : cls.clone();
    }

    public ServerLog[] getServerLogs() {
        return srv == null ? null : srv.clone();
    }

    public int getBouncePagesThreshold(){
        return bouncePagesThreshold;
    }

    public int getBounceSecondsThreshold(){
        return bounceSecondsThreshold;
    }

}
