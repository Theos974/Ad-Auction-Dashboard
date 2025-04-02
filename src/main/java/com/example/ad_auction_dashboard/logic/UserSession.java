package com.example.ad_auction_dashboard.logic;

import java.util.HashMap;
import java.util.Map;

public class UserSession {
    private static UserSession instance;
    private UserDatabase.User currentUser;
    private String previousScene;
    private CampaignMetrics currentCampaignMetrics;
    private String currentStyle;
    private Map<String, String> filterSettings = new HashMap<>();

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public void setUser(UserDatabase.User user) {
        this.currentUser = user;
    }

    public UserDatabase.User getUser() {
        return currentUser;
    }

    public String getRole() {
        return currentUser != null ? currentUser.getRole() : null;
    }

    // Permission helper methods
    public boolean isAdmin() {
        return currentUser != null && "admin".equals(currentUser.getRole());
    }

    public boolean isEditor() {
        return currentUser != null &&
            ("admin".equals(currentUser.getRole()) || "editor".equals(currentUser.getRole()));
    }

    public boolean isViewer() {
        return currentUser != null; // Everyone including admin/editor has at least viewer permissions
    }

    // New method to set previous scene
    public void setPreviousScene(String sceneName) {
        this.previousScene = sceneName;
    }

    // New method to get previous scene
    public String getPreviousScene() {
        return this.previousScene;
    }

    // Method to clear previous scene if needed
    public void clearPreviousScene() {
        this.previousScene = null;
    }

    // New method to store current campaign metrics
    public void setCurrentCampaignMetrics(CampaignMetrics metrics) {
        this.currentCampaignMetrics = metrics;
    }

    // New method to retrieve current campaign metrics
    public CampaignMetrics getCurrentCampaignMetrics() {
        return this.currentCampaignMetrics;
    }

    public void setFilterSetting(String key, String value) {
        filterSettings.put(key, value);
    }

    public String getFilterSetting(String key) {
        return filterSettings.get(key);
    }

    public void clearFilterSettings() {
        filterSettings.clear();
    }

    public void setCurrentStyle(String style){
        this.currentStyle = style;
    }
    public String getCurrentStyle() {
        return this.currentStyle;
    }

    // Modify the logout method to clear filters as well
    public void logout() {
        currentUser = null;
        previousScene = null;
        currentCampaignMetrics = null;
        filterSettings.clear();
    }
}