package com.example.ad_auction_dashboard.logic;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final Map<String, ComputedMetrics> cache = new HashMap<>();
    private final Map<String, Map<String, ComputedMetrics>> granularCache = new HashMap<>();
    private ComputedMetrics currentMetrics = new ComputedMetrics(); // Initialize to avoid NPE

    // Inner class for cached metric values
    public static class ComputedMetrics {
        int numberOfImpressions, numberOfClicks, numberOfUniques, numberOfBounces, numberOfConversions;
        double totalCost, ctr, cpc, cpa, cpm, bounceRate;
        public int getNumberOfImpressions() { return numberOfImpressions; }
        public int getNumberOfClicks() { return numberOfClicks; }
        public int getNumberOfUniques() { return numberOfUniques; }
        public int getNumberOfBounces() { return numberOfBounces; }
        public int getNumberOfConversions() { return numberOfConversions; }
        public double getTotalCost() { return totalCost; }
        public double getCtr() { return ctr; }
        public double getCpc() { return cpc; }
        public double getCpa() { return cpa; }
        public double getCpm() { return cpm; }
        public double getBounceRate() { return bounceRate; }
    }

    // Bounce criteria thresholds
    private final int bouncePagesThreshold;
    private final int bounceSecondsThreshold;

    // Cached logs (from the campaign; assumed static)
    private final ImpressionLog[] imps;
    private final ServerLog[] srv;
    private final ClickLog[] cls;

    // Hourly caches
    private final Map<String, Integer> hourlyImpressionCache = new HashMap<>();
    private final Map<String, Integer> hourlyClickCache = new HashMap<>();
    private final Map<String, Set<String>> hourlyUniqueIdsCache = new HashMap<>();
    private final Map<String, Integer> hourlyBounceCache = new HashMap<>();
    private final Map<String, Integer> hourlyConversionCache = new HashMap<>();
    private final Map<String, Double> hourlyCostCache = new HashMap<>();

    // Flag to track if cache is initialized
    private boolean hourlyDataCached = false;

    public TimeFilteredMetrics(ImpressionLog[] imps, ServerLog[] srv, ClickLog[] cls,
                               int bouncePagesThreshold, int bounceSecondsThreshold) {
        this.imps = imps;
        this.srv = srv;
        this.cls = cls;
        this.bouncePagesThreshold = bouncePagesThreshold;
        this.bounceSecondsThreshold = bounceSecondsThreshold;

        // Pre-cache hourly data for faster lookups
        initializeHourlyCaches();
    }

    /**
     * Initializes hourly caches for all metrics
     */
    private void initializeHourlyCaches() {
        if (hourlyDataCached) return;

        // Process impression logs
        if (imps != null) {
            for (ImpressionLog imp : imps) {
                LogDate ld = imp.getDate();
                if (ld != null && ld.getExists()) {
                    LocalDateTime time = toLocalDateTime(ld);
                    String hourKey = time.truncatedTo(ChronoUnit.HOURS).toString();

                    // Update impression count
                    hourlyImpressionCache.put(hourKey,
                        hourlyImpressionCache.getOrDefault(hourKey, 0) + 1);

                    // Update cost
                    hourlyCostCache.put(hourKey,
                        hourlyCostCache.getOrDefault(hourKey, 0.0) + imp.getImpressionCost());
                }
            }
        }

        // Process click logs
        if (cls != null) {
            for (ClickLog click : cls) {
                LogDate ld = click.getDate();
                if (ld != null && ld.getExists()) {
                    LocalDateTime time = toLocalDateTime(ld);
                    String hourKey = time.truncatedTo(ChronoUnit.HOURS).toString();

                    // Update click count
                    hourlyClickCache.put(hourKey,
                        hourlyClickCache.getOrDefault(hourKey, 0) + 1);

                    // Track unique IDs
                    hourlyUniqueIdsCache.putIfAbsent(hourKey, new HashSet<>());
                    hourlyUniqueIdsCache.get(hourKey).add(click.getId());

                    // Update cost
                    hourlyCostCache.put(hourKey,
                        hourlyCostCache.getOrDefault(hourKey, 0.0) + click.getClickCost());
                }
            }
        }

        // Process server logs
        if (srv != null) {
            for (ServerLog server : srv) {
                if (!isValidLog(server)) continue;

                LocalDateTime entryTime = toLocalDateTime(server.getEntryDate());
                String hourKey = entryTime.truncatedTo(ChronoUnit.HOURS).toString();

                // Check for bounce
                LocalDateTime exitTime = toLocalDateTime(server.getExitDate());
                long diffSeconds = Duration.between(entryTime, exitTime).getSeconds();

                if (server.getPagesViewed() <= bouncePagesThreshold || diffSeconds <= bounceSecondsThreshold) {
                    hourlyBounceCache.put(hourKey,
                        hourlyBounceCache.getOrDefault(hourKey, 0) + 1);
                }

                // Check for conversion
                if (server.getConversion()) {
                    hourlyConversionCache.put(hourKey,
                        hourlyConversionCache.getOrDefault(hourKey, 0) + 1);
                }
            }
        }

        hourlyDataCached = true;
    }
    // Add this method to TimeFilteredMetrics to fill in missing hours
    private Map<String, TimeFilteredMetrics.ComputedMetrics> ensureCompleteHourlyData(
        Map<String, TimeFilteredMetrics.ComputedMetrics> metricsByTime,
        LocalDateTime start,
        LocalDateTime end) {

        Map<String, TimeFilteredMetrics.ComputedMetrics> completeData = new HashMap<>(metricsByTime);

        // Truncate to the hour to ensure consistent formatting
        LocalDateTime current = start.truncatedTo(ChronoUnit.HOURS);
        end = end.truncatedTo(ChronoUnit.HOURS);

        // Create a date formatter that matches your existing labels
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:00"); // Adjust pattern to match your labels

        // For each hour in the range, ensure there's a data point
        while (!current.isAfter(end)) {
            String timeLabel = formatter.format(current);

            // If this hour doesn't exist in the data, add it with zeros
            if (!metricsByTime.containsKey(timeLabel)) {
                TimeFilteredMetrics.ComputedMetrics emptyMetrics = new TimeFilteredMetrics.ComputedMetrics();
                // All fields default to 0
                completeData.put(timeLabel, emptyMetrics);
            }

            // Move to next hour
            current = current.plusHours(1);
        }

        return completeData;
    }

    /**
     * Computes metrics for a time frame and updates currentMetrics.
     * Uses caching to avoid recomputing previously requested data.
     */
    public void computeForTimeFrame(LocalDateTime start, LocalDateTime end, String granularity) {
        String cacheKey = generateCacheKey(start, end, granularity);

        if (cache.containsKey(cacheKey)) {
            currentMetrics = cache.get(cacheKey);
            return;
        }

        ComputedMetrics computed = new ComputedMetrics();
        computed.numberOfImpressions = filterImpressions(start, end);
        computed.numberOfClicks = filterClicks(start, end);
        computed.numberOfUniques = filterUniques(start, end);
        computed.numberOfBounces = filterBounces(start, end);
        computed.numberOfConversions = filterConversions(start, end);
        computed.totalCost = filterTotalCost(start, end);

        // Compute Derived Metrics
        computed.ctr = (computed.numberOfImpressions == 0) ? 0 :
            (double) computed.numberOfClicks / computed.numberOfImpressions;
        computed.cpc = (computed.numberOfClicks == 0) ? 0 :
            computed.totalCost / computed.numberOfClicks;
        computed.cpa = (computed.numberOfConversions == 0) ? 0 :
            computed.totalCost / computed.numberOfConversions;
        computed.cpm = (computed.numberOfImpressions == 0) ? 0 :
            (computed.totalCost / computed.numberOfImpressions) * 1000;
        computed.bounceRate = (computed.numberOfClicks == 0) ? 0 :
            (double) computed.numberOfBounces / computed.numberOfClicks;

        cache.put(cacheKey, computed);
        currentMetrics = computed;
    }

    /**
     * Computes metrics for time buckets based on the specified granularity.
     * Uses caching to avoid recomputing previously requested data.
     */
    public Map<String, ComputedMetrics> computeForTimeFrameWithGranularity(LocalDateTime start, LocalDateTime end, String granularity) {
        String cacheKey = generateCacheKey(start, end, granularity);

        // Check if we already have this result cached
        if (granularCache.containsKey(cacheKey)) {
            return granularCache.get(cacheKey);
        }

        Map<String, ComputedMetrics> results = new HashMap<>();
        LocalDateTime pointer = start;

        while (!pointer.isAfter(end)) {
            LocalDateTime bucketStart = pointer;
            LocalDateTime bucketEnd;
            String timeLabel;

            // Determine bucket duration based on granularity
            switch (granularity) {
                case "Hourly":
                    bucketEnd = pointer.plusHours(1).minusSeconds(1);
                    timeLabel = bucketStart.getHour() + ":00"; // "9:00", "10:00", etc.
                    break;
                case "Daily":
                    bucketEnd = pointer.plusDays(1).minusSeconds(1);
                    timeLabel = bucketStart.toLocalDate().toString(); // "2025-03-02"
                    break;
                case "Weekly":
                    bucketEnd = pointer.plusWeeks(1).minusSeconds(1);
                    timeLabel = "Week " + bucketStart.getDayOfYear() / 7; // "Week 10"
                    break;
                default:
                    bucketEnd = pointer.plusDays(1).minusSeconds(1);
                    timeLabel = bucketStart.toLocalDate().toString();
            }

            // Ensure bucketEnd does not exceed campaign end date
            if (bucketEnd.isAfter(end)) {
                bucketEnd = end;
            }

            // Compute metrics for this time interval
            ComputedMetrics computed = new ComputedMetrics();
            computed.numberOfImpressions = filterImpressions(bucketStart, bucketEnd);
            computed.numberOfClicks = filterClicks(bucketStart, bucketEnd);
            computed.numberOfUniques = filterUniques(bucketStart, bucketEnd);
            computed.numberOfBounces = filterBounces(bucketStart, bucketEnd);
            computed.numberOfConversions = filterConversions(bucketStart, bucketEnd);
            computed.totalCost = filterTotalCost(bucketStart, bucketEnd);

            // Compute Derived Metrics
            computed.ctr = computed.numberOfImpressions == 0 ? 0 :
                (double) computed.numberOfClicks / computed.numberOfImpressions;
            computed.cpc = computed.numberOfClicks == 0 ? 0 :
                computed.totalCost / computed.numberOfClicks;
            computed.cpa = computed.numberOfConversions == 0 ? 0 :
                computed.totalCost / computed.numberOfConversions;
            computed.cpm = computed.numberOfImpressions == 0 ? 0 :
                (computed.totalCost / computed.numberOfImpressions) * 1000;
            computed.bounceRate = computed.numberOfClicks == 0 ? 0 :
                (double) computed.numberOfBounces / computed.numberOfClicks;

            // Store the computed metrics
            results.put(timeLabel, computed);

            // Move pointer to the next time bucket
            pointer = bucketEnd.plusSeconds(1);
        }

        if (granularity.equals("Hourly")) {
            results = ensureCompleteHourlyData(results, start, end);
        }

        // Also calculate overall metrics and update currentMetrics
        ComputedMetrics overall = new ComputedMetrics();
        overall.numberOfImpressions = filterImpressions(start, end);
        overall.numberOfClicks = filterClicks(start, end);
        overall.numberOfUniques = filterUniques(start, end);
        overall.numberOfBounces = filterBounces(start, end);
        overall.numberOfConversions = filterConversions(start, end);
        overall.totalCost = filterTotalCost(start, end);

        // Compute overall derived metrics
        overall.ctr = overall.numberOfImpressions == 0 ? 0 :
            (double) overall.numberOfClicks / overall.numberOfImpressions;
        overall.cpc = overall.numberOfClicks == 0 ? 0 :
            overall.totalCost / overall.numberOfClicks;
        overall.cpa = overall.numberOfConversions == 0 ? 0 :
            overall.totalCost / overall.numberOfConversions;
        overall.cpm = overall.numberOfImpressions == 0 ? 0 :
            (overall.totalCost / overall.numberOfImpressions) * 1000;
        overall.bounceRate = overall.numberOfClicks == 0 ? 0 :
            (double) overall.numberOfBounces / overall.numberOfClicks;

        // Cache the results
        granularCache.put(cacheKey, results);
        currentMetrics = overall;

        return results;
    }

    // Cache key generation based on time range and granularity
    private String generateCacheKey(LocalDateTime start, LocalDateTime end, String granularity) {
        return start.toString() + "_" + end.toString() + "_" + granularity;
    }



    // ---------- Optimized Filtering Methods with Cached Data ----------

    public int filterImpressions(LocalDateTime start, LocalDateTime end) {
        if (hourlyDataCached) {
            return getCountFromHourlyCache(start, end, hourlyImpressionCache);
        }

        int count = 0;
        if (imps != null) {
            for (ImpressionLog i : imps) {
                LogDate ld = i.getDate();
                if (ld != null && ld.getExists()) {
                    LocalDateTime time = toLocalDateTime(ld);
                    if (!time.isBefore(start) && !time.isAfter(end)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public int filterClicks(LocalDateTime start, LocalDateTime end) {
        if (hourlyDataCached) {
            return getCountFromHourlyCache(start, end, hourlyClickCache);
        }
        return processClickLogs(start, end, cls);
    }

    private int processClickLogs(LocalDateTime start, LocalDateTime end, ClickLog[] cls) {
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
        if (hourlyDataCached) {
            Set<String> uniqueIds = new HashSet<>();

            // Gather all unique IDs from relevant hours
            LocalDateTime current = start.truncatedTo(ChronoUnit.HOURS);
            LocalDateTime endHour = end.truncatedTo(ChronoUnit.HOURS);

            while (!current.isAfter(endHour)) {
                String hourKey = current.toString();
                Set<String> hourlyIds = hourlyUniqueIdsCache.get(hourKey);
                if (hourlyIds != null) {
                    uniqueIds.addAll(hourlyIds);
                }
                current = current.plusHours(1);
            }

            return uniqueIds.size();
        }

        return calculateUniquesDirectly(start, end);
    }

    private int calculateUniquesDirectly(LocalDateTime start, LocalDateTime end) {
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
        if (hourlyDataCached) {
            return getCountFromHourlyCache(start, end, hourlyBounceCache);
        }
        return processBounces(start, end, srv, bouncePagesThreshold, bounceSecondsThreshold);
    }

    private int processBounces(LocalDateTime start, LocalDateTime end, ServerLog[] srvLogs, int pageThreshold, int timeThreshold) {
        if (srvLogs == null) return 0;

        int bounces = 0;
        for (ServerLog s : srvLogs) {
            if (!isValidLog(s)) continue;

            LocalDateTime entry = toLocalDateTime(s.getEntryDate());
            if (entry.isBefore(start) || entry.isAfter(end)) continue;

            LocalDateTime exit = toLocalDateTime(s.getExitDate());
            long diffSeconds = Duration.between(entry, exit).getSeconds();

            if (s.getPagesViewed() <= pageThreshold || diffSeconds <= timeThreshold) {
                bounces++;
            }
        }
        return bounces;
    }

    public int filterConversions(LocalDateTime start, LocalDateTime end) {
        if (hourlyDataCached) {
            return getCountFromHourlyCache(start, end, hourlyConversionCache);
        }
        return processConversions(start, end, srv);
    }

    private int processConversions(LocalDateTime start, LocalDateTime end, ServerLog[] srvLogs) {
        if (srvLogs == null) return 0;

        int conversions = 0;
        for (ServerLog s : srvLogs) {
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
        if (hourlyDataCached) {
            double totalCost = 0.0;

            LocalDateTime current = start.truncatedTo(ChronoUnit.HOURS);
            LocalDateTime endHour = end.truncatedTo(ChronoUnit.HOURS);

            while (!current.isAfter(endHour)) {
                String hourKey = current.toString();
                totalCost += hourlyCostCache.getOrDefault(hourKey, 0.0);
                current = current.plusHours(1);
            }

            return totalCost;
        }
        return processTotalCost(start, end, imps, cls);
    }

    private double processTotalCost(LocalDateTime start, LocalDateTime end, ImpressionLog[] imps, ClickLog[] cls) {
        double totalCost = 0;

        if (imps != null) {
            for (ImpressionLog i : imps) {
                LogDate ld = i.getDate();
                if (ld != null && ld.getExists()) {
                    LocalDateTime time = toLocalDateTime(ld);
                    if (!time.isBefore(start) && !time.isAfter(end)) {
                        totalCost += i.getImpressionCost();
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
                        totalCost += c.getClickCost();
                    }
                }
            }
        }

        return totalCost;
    }

    // Helper method to get count from hourly cache
    private int getCountFromHourlyCache(LocalDateTime start, LocalDateTime end, Map<String, Integer> cache) {
        int count = 0;

        LocalDateTime current = start.truncatedTo(ChronoUnit.HOURS);
        LocalDateTime endHour = end.truncatedTo(ChronoUnit.HOURS);

        while (!current.isAfter(endHour)) {
            String hourKey = current.toString();
            count += cache.getOrDefault(hourKey, 0);
            current = current.plusHours(1);
        }

        return count;
    }

    // Utility Methods
    public LocalDateTime toLocalDateTime(LogDate ld) {
        return LocalDateTime.of(ld.getYear(), ld.getMonth(), ld.getDay(), ld.getHour(), ld.getMinute(), ld.getSecond());
    }

    public boolean isValidLog(ServerLog s) {
        return s.getEntryDate() != null && s.getExitDate() != null &&
            s.getEntryDate().getExists() && s.getExitDate().getExists();
    }

    // Getters for Cached Metrics
    public int getNumberOfImpressions() { return currentMetrics.numberOfImpressions; }
    public int getNumberOfClicks() { return currentMetrics.numberOfClicks; }
    public int getNumberOfUniques() { return currentMetrics.numberOfUniques; }
    public int getNumberOfBounces() { return currentMetrics.numberOfBounces; }
    public int getNumberOfConversions() { return currentMetrics.numberOfConversions; }
    public double getTotalCost() { return currentMetrics.totalCost; }
    public double getCTR() { return currentMetrics.ctr; }
    public double getCPC() { return currentMetrics.cpc; }
    public double getCPA() { return currentMetrics.cpa; }
    public double getCPM() { return currentMetrics.cpm; }
    public double getBounceRate() { return currentMetrics.bounceRate; }

    // Helper method to clear caches if needed
    public void clearCaches() {
        cache.clear();
        granularCache.clear();
        hourlyImpressionCache.clear();
        hourlyClickCache.clear();
        hourlyUniqueIdsCache.clear();
        hourlyBounceCache.clear();
        hourlyConversionCache.clear();
        hourlyCostCache.clear();
        hourlyDataCached = false;

        // Reinitialize hourly caches
        initializeHourlyCaches();
    }
}