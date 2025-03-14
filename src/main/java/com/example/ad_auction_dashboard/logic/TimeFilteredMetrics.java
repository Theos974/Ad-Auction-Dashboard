package com.example.ad_auction_dashboard.logic;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TimeFilteredMetrics computes campaign metrics (impressions, clicks, uniques, etc.)
 * for a given time frame based on the static log arrays.
 * This class is intended for use in the time-chart scene,
 * where the user can filter by a start and end date, audience segments, and context.
 */
public class TimeFilteredMetrics {
    private final Map<String, ComputedMetrics> cache = new HashMap<>();
    private final Map<String, Map<String, ComputedMetrics>> granularCache = new HashMap<>();
    private ComputedMetrics currentMetrics = new ComputedMetrics(); // Initialize to avoid NPE

    // Filter parameters
    private String genderFilter = null; // null means no filter applied
    private String ageFilter = null;
    private String incomeFilter = null;
    private String contextFilter = null;

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

    // User-impressions mapping for efficient filtering
    private final Map<String, Set<ImpressionLog>> userImpressionsMap = new HashMap<>();

    // Flag to track if cache is initialized
    private boolean hourlyDataCached = false;
    private boolean userImpressionsMapBuilt = false;

    public TimeFilteredMetrics(ImpressionLog[] imps, ServerLog[] srv, ClickLog[] cls,
                               int bouncePagesThreshold, int bounceSecondsThreshold) {
        this.imps = imps;
        this.srv = srv;
        this.cls = cls;
        this.bouncePagesThreshold = bouncePagesThreshold;
        this.bounceSecondsThreshold = bounceSecondsThreshold;

        // Pre-cache hourly data for faster lookups
        initializeHourlyCaches();
        buildUserImpressionsMap();
    }

    /**
     * Build a mapping of user IDs to their impression logs for efficient filtering
     */
    private void buildUserImpressionsMap() {
        if (userImpressionsMapBuilt) return;

        userImpressionsMap.clear();
        if (imps != null) {
            for (ImpressionLog imp : imps) {
                if (imp == null || imp.getId() == null) continue;

                String userId = imp.getId();
                if (!userImpressionsMap.containsKey(userId)) {
                    userImpressionsMap.put(userId, new HashSet<>());
                }
                userImpressionsMap.get(userId).add(imp);
            }
        }

        userImpressionsMapBuilt = true;
    }

    /**
     * Set a filter for gender
     * @param gender The gender to filter by, or null to clear the filter
     */
    public void setGenderFilter(String gender) {
        this.genderFilter = gender;
        clearCaches();
    }

    /**
     * Set a filter for age
     * @param age The age range to filter by, or null to clear the filter
     */
    public void setAgeFilter(String age) {
        this.ageFilter = age;
        clearCaches();
    }

    /**
     * Set a filter for income
     * @param income The income level to filter by, or null to clear the filter
     */
    public void setIncomeFilter(String income) {
        this.incomeFilter = income;
        clearCaches();
    }

    /**
     * Set a filter for context
     * @param context The context to filter by, or null to clear the filter
     */
    public void setContextFilter(String context) {
        this.contextFilter = context;
        clearCaches();
    }


    /**
     * Check if an impression log passes all current filters
     */
    public boolean passesFilters(ImpressionLog imp) {
        if (imp == null) return false;

        if (genderFilter != null && !genderFilter.equals(imp.getGender())) {
            return false;
        }
        if (ageFilter != null && !ageFilter.equals(imp.getAge())) {
            return false;
        }
        if (incomeFilter != null && !incomeFilter.equals(imp.getIncome())) {
            return false;
        }
        if (contextFilter != null && !contextFilter.equals(imp.getContext())) {
            return false;
        }
        return true;
    }

    /**
     * Check if a user ID passes the current filters by checking their impressions
     */
    public boolean userPassesFilters(String userId) {
        if (genderFilter == null && ageFilter == null &&
            incomeFilter == null && contextFilter == null) {
            return true; // No filters active
        }

        Set<ImpressionLog> userImps = userImpressionsMap.get(userId);
        if (userImps == null || userImps.isEmpty()) {
            return false; // No impressions found for this user
        }

        // Check if any impression for this user passes the filters
        for (ImpressionLog imp : userImps) {
            if (passesFilters(imp)) {
                return true;
            }
        }
        return false;
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

    /**
     * Adds empty data points for any missing hours in the time range
     * to ensure complete and consistent chart display
     */
    private Map<String, TimeFilteredMetrics.ComputedMetrics> ensureCompleteHourlyData(
        Map<String, TimeFilteredMetrics.ComputedMetrics> metricsByTime,
        LocalDateTime start,
        LocalDateTime end) {

        Map<String, TimeFilteredMetrics.ComputedMetrics> completeData = new HashMap<>(metricsByTime);

        // Truncate to the hour to ensure consistent formatting
        LocalDateTime current = start.truncatedTo(ChronoUnit.HOURS);
        end = end.truncatedTo(ChronoUnit.HOURS);

        // For each hour in the range, ensure there's a data point
        while (!current.isAfter(end)) {
            // Use the standardized formatter for consistency
            String timeLabel = formatTimeLabel(current, "Hourly");

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
     * Helper method for consistent time formatting across the class
     * @param dateTime The datetime to format
     * @param granularity The time granularity level (Hourly, Daily, Weekly)
     * @return A formatted string representation of the time
     */
    private String formatTimeLabel(LocalDateTime dateTime, String granularity) {
        switch (granularity) {
            case "Hourly":
                return String.format("%02d:00", dateTime.getHour());
            case "Daily":
                return dateTime.toLocalDate().toString();
            case "Weekly":
                return "Week " + dateTime.getDayOfYear() / 7;
            default:
                return dateTime.toLocalDate().toString();
        }
    }

    /**
     * Computes metrics for time buckets based on the specified granularity.
     * Uses caching to avoid recomputing previously requested data.
     */
    public Map<String, ComputedMetrics> computeForTimeFrameWithGranularity(
        LocalDateTime start,
        LocalDateTime end,
        String granularity) {

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
                    timeLabel = formatTimeLabel(bucketStart, granularity);
                    break;
                case "Daily":
                    bucketEnd = pointer.plusDays(1).minusSeconds(1);
                    timeLabel = formatTimeLabel(bucketStart, granularity);
                    break;
                case "Weekly":
                    bucketEnd = pointer.plusWeeks(1).minusSeconds(1);
                    timeLabel = formatTimeLabel(bucketStart, granularity);
                    break;
                default:
                    bucketEnd = pointer.plusDays(1).minusSeconds(1);
                    timeLabel = formatTimeLabel(bucketStart, granularity);
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

    // Cache key generation based on time range, granularity, and filters
    private String generateCacheKey(LocalDateTime start, LocalDateTime end, String granularity) {
        return start.toString() + "_" + end.toString() + "_" + granularity + "_" +
            genderFilter + "_" + ageFilter + "_" + incomeFilter + "_" + contextFilter;
    }

    // FILTERING METHODS - UPDATED TO INCLUDE AUDIENCE AND CONTEXT FILTERS

    public int filterImpressions(LocalDateTime start, LocalDateTime end) {
        // If no audience or context filters, use cached hourly data for speed
        if (genderFilter == null && ageFilter == null &&
            incomeFilter == null && contextFilter == null) {
            return getCountFromHourlyCache(start, end, hourlyImpressionCache);
        }

        int count = 0;
        if (imps != null) {
            for (ImpressionLog i : imps) {
                LogDate ld = i.getDate();
                if (ld != null && ld.getExists()) {
                    LocalDateTime time = toLocalDateTime(ld);
                    if (!time.isBefore(start) && !time.isAfter(end) && passesFilters(i)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public int filterClicks(LocalDateTime start, LocalDateTime end) {
        // If no audience or context filters, use cached hourly data for speed
        if (genderFilter == null && ageFilter == null &&
            incomeFilter == null && contextFilter == null) {
            return getCountFromHourlyCache(start, end, hourlyClickCache);
        }

        int count = 0;
        if (cls != null) {
            for (ClickLog c : cls) {
                LogDate ld = c.getDate();
                if (ld != null && ld.getExists()) {
                    LocalDateTime time = toLocalDateTime(ld);
                    if (!time.isBefore(start) && !time.isAfter(end) && userPassesFilters(c.getId())) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public int filterUniques(LocalDateTime start, LocalDateTime end) {
        // If no audience or context filters, use hourly caches
        if (genderFilter == null && ageFilter == null &&
            incomeFilter == null && contextFilter == null) {
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

        // With filters, we need to check each click
        Set<String> uniqueIds = new HashSet<>();
        if (cls != null) {
            for (ClickLog c : cls) {
                LogDate ld = c.getDate();
                if (ld != null && ld.getExists()) {
                    LocalDateTime time = toLocalDateTime(ld);
                    if (!time.isBefore(start) && !time.isAfter(end) && userPassesFilters(c.getId())) {
                        uniqueIds.add(c.getId());
                    }
                }
            }
        }

        return uniqueIds.size();
    }

    public int filterBounces(LocalDateTime start, LocalDateTime end) {
        // If no audience or context filters, use cached hourly data for speed
        if (genderFilter == null && ageFilter == null &&
            incomeFilter == null && contextFilter == null) {
            return getCountFromHourlyCache(start, end, hourlyBounceCache);
        }

        int bounces = 0;
        if (srv != null) {
            for (ServerLog s : srv) {
                if (!isValidLog(s)) continue;

                LocalDateTime entry = toLocalDateTime(s.getEntryDate());
                if (entry.isBefore(start) || entry.isAfter(end)) continue;

                if (!userPassesFilters(s.getId())) continue;

                LocalDateTime exit = toLocalDateTime(s.getExitDate());
                long diffSeconds = Duration.between(entry, exit).getSeconds();

                if (s.getPagesViewed() <= bouncePagesThreshold || diffSeconds <= bounceSecondsThreshold) {
                    bounces++;
                }
            }
        }

        return bounces;
    }

    public int filterConversions(LocalDateTime start, LocalDateTime end) {
        // If no audience or context filters, use cached hourly data for speed
        if (genderFilter == null && ageFilter == null &&
            incomeFilter == null && contextFilter == null) {
            return getCountFromHourlyCache(start, end, hourlyConversionCache);
        }

        int conversions = 0;
        if (srv != null) {
            for (ServerLog s : srv) {
                LogDate ld = s.getEntryDate();
                if (ld != null && ld.getExists()) {
                    LocalDateTime entry = toLocalDateTime(ld);
                    if (!entry.isBefore(start) && !entry.isAfter(end) &&
                        s.getConversion() && userPassesFilters(s.getId())) {
                        conversions++;
                    }
                }
            }
        }

        return conversions;
    }

    public double filterTotalCost(LocalDateTime start, LocalDateTime end) {
        // If no audience or context filters, use cached hourly data for speed
        if (genderFilter == null && ageFilter == null &&
            incomeFilter == null && contextFilter == null) {
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

        // With filters, we need to compute costs directly
        double totalCost = 0;

        // Impression costs
        if (imps != null) {
            for (ImpressionLog i : imps) {
                LogDate ld = i.getDate();
                if (ld != null && ld.getExists()) {
                    LocalDateTime time = toLocalDateTime(ld);
                    if (!time.isBefore(start) && !time.isAfter(end) && passesFilters(i)) {
                        totalCost += i.getImpressionCost();
                    }
                }
            }
        }

        // Click costs
        if (cls != null) {
            for (ClickLog c : cls) {
                LogDate ld = c.getDate();
                if (ld != null && ld.getExists()) {
                    LocalDateTime time = toLocalDateTime(ld);
                    if (!time.isBefore(start) && !time.isAfter(end) && userPassesFilters(c.getId())) {
                        totalCost += c.getClickCost();
                    }
                }
            }
        }

        return totalCost;
    }

    /**
     * Helper method to get count from hourly cache between specified dates
     */
    private int getCountFromHourlyCache(LocalDateTime start, LocalDateTime end, Map<String, Integer> cache) {
        int count = 0;

        LocalDateTime current = start.truncatedTo(ChronoUnit.HOURS);
        LocalDateTime endHour = end.truncatedTo(ChronoUnit.HOURS);

        while (!current.isAfter(endHour)) {
            // Important: this must match how the cache was populated in initializeHourlyCaches
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