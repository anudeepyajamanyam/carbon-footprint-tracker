package com.carbon.tracker.dto;

import java.util.List;
import java.util.Map;

public class DashboardResponse {
    private Double currentMonthEmissions;
    private Double previousMonthEmissions;
    private Double totalSavedCo2;
    private Double currentMonthGoal;
    private Map<String, Double> categoryBreakdown;
    private List<ActivityLogResponse> recentLogs;
    private List<String> recommendationTips;

    public DashboardResponse(Double currentMonthEmissions, Double previousMonthEmissions, Double totalSavedCo2,
                             Double currentMonthGoal, Map<String, Double> categoryBreakdown,
                             List<ActivityLogResponse> recentLogs, List<String> recommendationTips) {
        this.currentMonthEmissions = currentMonthEmissions;
        this.previousMonthEmissions = previousMonthEmissions;
        this.totalSavedCo2 = totalSavedCo2;
        this.currentMonthGoal = currentMonthGoal;
        this.categoryBreakdown = categoryBreakdown;
        this.recentLogs = recentLogs;
        this.recommendationTips = recommendationTips;
    }

    public Double getCurrentMonthEmissions() { return currentMonthEmissions; }
    public void setCurrentMonthEmissions(Double currentMonthEmissions) { this.currentMonthEmissions = currentMonthEmissions; }

    public Double getPreviousMonthEmissions() { return previousMonthEmissions; }
    public void setPreviousMonthEmissions(Double previousMonthEmissions) { this.previousMonthEmissions = previousMonthEmissions; }

    public Double getTotalSavedCo2() { return totalSavedCo2; }
    public void setTotalSavedCo2(Double totalSavedCo2) { this.totalSavedCo2 = totalSavedCo2; }

    public Double getCurrentMonthGoal() { return currentMonthGoal; }
    public void setCurrentMonthGoal(Double currentMonthGoal) { this.currentMonthGoal = currentMonthGoal; }

    public Map<String, Double> getCategoryBreakdown() { return categoryBreakdown; }
    public void setCategoryBreakdown(Map<String, Double> categoryBreakdown) { this.categoryBreakdown = categoryBreakdown; }

    public List<ActivityLogResponse> getRecentLogs() { return recentLogs; }
    public void setRecentLogs(List<ActivityLogResponse> recentLogs) { this.recentLogs = recentLogs; }

    public List<String> getRecommendationTips() { return recommendationTips; }
    public void setRecommendationTips(List<String> recommendationTips) { this.recommendationTips = recommendationTips; }
}
