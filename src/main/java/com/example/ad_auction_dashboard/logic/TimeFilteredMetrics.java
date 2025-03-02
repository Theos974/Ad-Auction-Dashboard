package com.example.ad_auction_dashboard.logic;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * TimeFilteredMetrics computes campaign metrics (impressions, clicks, uniques, etc.)
 * for a given time frame based on the static log arrays.
 * This class is intended for use in the time-chart scene,
 * where the user can filter by a start and end date.
 */
public class TimeFilteredMetrics {
    // Filtered metric fields
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
    private final Map<String, ComputedMetrics> cache = new HashMap<>();


    // Bounce criteria thresholds
    private int bouncePagesThreshold;
    private int bounceSecondsThreshold;

    // Cached logs (from the campaign; assumed static)
    private final ImpressionLog[] imps;
    private final ServerLog[] srv;
    private final ClickLog[] cls;

    /**
     * Constructor accepts the cached logs and bounce criteria.
     */
    public TimeFilteredMetrics(ImpressionLog[] imps, ServerLog[] srv, ClickLog[] cls,
                               int bouncePagesThreshold, int bounceSecondsThreshold) {
        this.imps = imps;
        this.srv = srv;
        this.cls = cls;
        this.bouncePagesThreshold = bouncePagesThreshold;
        this.bounceSecondsThreshold = bounceSecondsThreshold;
    }

    /**
     * Computes and updates all metric fields for logs whose timestamps
     * (for impressions and clicks, using getDate(); for server logs, using entry date)
     * fall between start and end (inclusive).
     *
     * @param start the start date/time (inclusive)
     * @param end   the end date/time (inclusive)
     */
    public void computeForTimeFrame(LocalDateTime start, LocalDateTime end, String granularity) {
        String cacheKey = generateCacheKey(start, end, granularity);

        // Check Cache First
        if (cache.containsKey(cacheKey)) {
            currentMetrics = cache.get(cacheKey);
            return; // Use cached data
        }

        // Compute Fresh Metrics If Not Cached
        ComputedMetrics computed = new ComputedMetrics();
        computed.numberOfImpressions = filterImpressions(start, end);
        computed.numberOfClicks = filterClicks(start, end);
        computed.numberOfUniques = filterUniques(start, end);
        computed.numberOfBounces = filterBounces(start, end);
        computed.numberOfConversions = filterConversions(start, end);
        computed.totalCost = filterTotalCost(start, end);

        // Derived Metrics
        computed.ctr = computed.numberOfImpressions == 0 ? 0 : (double) computed.numberOfClicks / computed.numberOfImpressions;
        computed.cpc = computed.numberOfClicks == 0 ? 0 : computed.totalCost / computed.numberOfClicks;
        computed.cpa = computed.numberOfConversions == 0 ? 0 : computed.totalCost / computed.numberOfConversions;
        computed.cpm = computed.numberOfImpressions == 0 ? 0 : (computed.totalCost / computed.numberOfImpressions) * 1000;
        computed.bounceRate = computed.numberOfClicks == 0 ? 0 : (double) computed.numberOfBounces / computed.numberOfClicks;

        // Store Computed Metrics in Cache
        cache.put(cacheKey, computed);
        currentMetrics = computed;
    }

    private String generateCacheKey(LocalDateTime start, LocalDateTime end, String granularity) {
        return start.toString() + "_" + end.toString() + "_" + granularity;
    }

    public double computeNewCTR(LocalDateTime start, LocalDateTime end){
        numberOfImpressions = filterImpressions(start, end);
        numberOfClicks = filterClicks(start, end);
        ctr = (numberOfImpressions == 0) ? 0 : (double) numberOfClicks / numberOfImpressions;

        return 0;
    }
    public void computeNewCPC(LocalDateTime start, LocalDateTime end){
        totalCost = filterTotalCost(start, end);
        numberOfClicks = filterClicks(start, end);
        cpc = (numberOfClicks == 0) ? 0 : totalCost / numberOfClicks;

    }
    public void computeNewCPA(LocalDateTime start, LocalDateTime end){
        totalCost = filterTotalCost(start, end);
        numberOfConversions = filterConversions(start, end);
        cpa = (numberOfConversions == 0) ? 0 : totalCost / numberOfConversions;

    }
    public void computeNewCPM(LocalDateTime start, LocalDateTime end){
        totalCost = filterTotalCost(start, end);
        numberOfImpressions = filterImpressions(start, end);

        cpm = (numberOfImpressions == 0) ? 0 : (totalCost / numberOfImpressions) * 1000;

    }
    public void computeNewBounce(LocalDateTime start, LocalDateTime end){
        numberOfBounces = filterBounces(start, end);
        numberOfClicks = filterClicks(start, end);

        bounceRate = (numberOfClicks == 0) ? 0 : (double) numberOfBounces / numberOfClicks;

    }


    // ---------- Filtering Helper Methods ----------

    private final Map<LocalDateTime, Integer> hourlyImpressionCache = new HashMap<>();

    public int filterImpressions(LocalDateTime start, LocalDateTime end) {
        int count = 0;

        // Use Cached Buckets
        for (LocalDateTime time : hourlyImpressionCache.keySet()) {
            if (!time.isBefore(start) && !time.isAfter(end)) {
                count += hourlyImpressionCache.get(time);
            }
        }

        // If no cache, compute fresh
        if (count == 0 && imps != null) {
            for (ImpressionLog i : imps) {
                LogDate ld = i.getDate();
                if (ld != null && ld.getExists()) {
                    LocalDateTime time = toLocalDateTime(ld);
                    if (!time.isBefore(start) && !time.isAfter(end)) {
                        count++;
                        hourlyImpressionCache.put(time.truncatedTo(ChronoUnit.HOURS), count);
                    }
                }
            }
        }
        return count;
    }


    public int filterClicks(LocalDateTime start, LocalDateTime end) {
        if (cls == null) return 0;
        int count = 0;
        for (ClickLog c : cls) {
            LogDate ld = c.getDate();
            if (ld != null && ld.getExists()) {
                LocalDateTime time = toLocalDateTime(ld);
                if (!time.isBefore(start) && !time.isAfter(end)) {
                    count++;
                }
            }
        }
        return count;
    }

    public int filterUniques(LocalDateTime start, LocalDateTime end) {
        if (cls == null) return 0;
        Set<String> uniqueIds = new HashSet<>();
        for (ClickLog c : cls) {
            LogDate ld = c.getDate();
            if (ld != null && ld.getExists()) {
                LocalDateTime time = toLocalDateTime(ld);
                if (!time.isBefore(start) && !time.isAfter(end)) {
                    uniqueIds.add(c.getId());
                }
            }
        }
        return uniqueIds.size();
    }

    public int filterBounces(LocalDateTime start, LocalDateTime end) {
        int bounces = 0;
        if (srv == null) return 0;
        for (ServerLog s : srv) {
            if (!isValidLog(s)) continue;
            LocalDateTime entry = toLocalDateTime(s.getEntryDate());
            if (entry.isBefore(start) || entry.isAfter(end)) continue;
            LocalDateTime exit = toLocalDateTime(s.getExitDate());
            long diffSeconds = Duration.between(entry, exit).getSeconds();
            if (s.getPagesViewed() <= bouncePagesThreshold || diffSeconds <= bounceSecondsThreshold) {
                bounces++;
            }
        }
        return bounces;
    }

    public int filterConversions(LocalDateTime start, LocalDateTime end) {
        int conversions = 0;
        if (srv == null) return 0;
        for (ServerLog s : srv) {
            LogDate ld = s.getEntryDate();
            if (ld != null && ld.getExists()) {
                LocalDateTime entry = toLocalDateTime(ld);
                if (!entry.isBefore(start) && !entry.isAfter(end) && s.getConversion()) {
                    conversions++;
                }
            }
        }
        return conversions;
    }

    public double filterTotalCost(LocalDateTime start, LocalDateTime end) {
        double cost = 0;
        if (imps != null) {
            for (ImpressionLog i : imps) {
                LogDate ld = i.getDate();
                if (ld != null && ld.getExists()) {
                    LocalDateTime time = toLocalDateTime(ld);
                    if (!time.isBefore(start) && !time.isAfter(end)) {
                        cost += i.getImpressionCost();
                    }
                }
            }
        }
        if (cls != null) {
            for (ClickLog c : cls) {
                LogDate ld = c.getDate();
                if (ld != null && ld.getExists()) {
                    LocalDateTime time = toLocalDateTime(ld);
                    if (!time.isBefore(start) && !time.isAfter(end)) {
                        cost += c.getClickCost();
                    }
                }
            }
        }
        return cost;
    }

    // ---------- Utility Methods ----------

    public LocalDateTime toLocalDateTime(LogDate ld) {
        return LocalDateTime.of(
            ld.getYear(), ld.getMonth(), ld.getDay(),
            ld.getHour(), ld.getMinute(), ld.getSecond()
        );
    }

    public boolean isValidLog(ServerLog s) {
        return s.getEntryDate() != null && s.getExitDate() != null &&
            s.getEntryDate().getExists() && s.getExitDate().getExists();
    }

    // ---------- Getters for Filtered Metrics ----------

    public int getNumberOfImpressions() { return numberOfImpressions; }
    public int getNumberOfClicks() { return numberOfClicks; }
    public int getNumberOfUniques() { return numberOfUniques; }
    public int getNumberOfBounces() { return numberOfBounces; }
    public int getNumberOfConversions() { return numberOfConversions; }
    public double getTotalCost() { return totalCost; }
    public double getCTR() { return ctr; }
    public double getCPC() { return cpc; }
    public double getCPA() { return cpa; }
    public double getCPM() { return cpm; }
    public double getBounceRate() { return bounceRate; }
}
