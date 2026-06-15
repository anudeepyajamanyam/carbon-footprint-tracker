package com.carbon.tracker.dto;

public class LeaderboardEntry {
    private String username;
    private Double totalSavedCo2;
    private Integer rank;

    public LeaderboardEntry(String username, Double totalSavedCo2, Integer rank) {
        this.username = username;
        this.totalSavedCo2 = totalSavedCo2;
        this.rank = rank;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Double getTotalSavedCo2() { return totalSavedCo2; }
    public void setTotalSavedCo2(Double totalSavedCo2) { this.totalSavedCo2 = totalSavedCo2; }

    public Integer getRank() { return rank; }
    public void setRank(Integer rank) { this.rank = rank; }
}
