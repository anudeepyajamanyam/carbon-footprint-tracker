package com.carbon.tracker.dto;

import java.time.LocalDate;

public class ActivityLogResponse {
    private Long id;
    private String category;
    private String subType;
    private Double amount;
    private Double emission;
    private LocalDate logDate;
    private String notes;

    public ActivityLogResponse(Long id, String category, String subType, Double amount, Double emission, LocalDate logDate, String notes) {
        this.id = id;
        this.category = category;
        this.subType = subType;
        this.amount = amount;
        this.emission = emission;
        this.logDate = logDate;
        this.notes = notes;
    }

    public Long getId() { return id; }
    public String getCategory() { return category; }
    public String getSubType() { return subType; }
    public Double getAmount() { return amount; }
    public Double getEmission() { return emission; }
    public LocalDate getLogDate() { return logDate; }
    public String getNotes() { return notes; }
}
