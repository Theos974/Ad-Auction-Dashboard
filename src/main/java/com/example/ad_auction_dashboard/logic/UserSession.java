package com.example.ad_auction_dashboard.logic;

public class UserSession {
    private static UserSession instance;
    private UserDatabase.User currentUser;
    private String previousScene;
    private CampaignMetrics currentCampaignMetrics;
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

    // Modify logout to clear metrics
    public void logout() {
        currentUser = null;
        previousScene = null;
        currentCampaignMetrics = null;
    }
}