package com.carbon.tracker.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(
    name = "activity_log",
    indexes = {
        @Index(name = "idx_activity_log_user_date", columnList = "user_id, log_date"),
        @Index(name = "idx_activity_log_user_category_date", columnList = "user_id, category, log_date")
    }
)
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user;

    @Column(nullable = false)
    private String category; // TRANSPORT, ENERGY, FOOD, WASTE

    @Column(name = "sub_type", nullable = false)
    private String subType; // e.g. "Petrol Car", "Bus", "Electricity", "Meat Meal"

    @Column(nullable = false)
    private Double amount; // distance (km), power (kWh), meal count, weight (kg)

    @Column(nullable = false)
    private Double emission; // Calculated CO2 in kg

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(length = 500)
    private String notes;

    public ActivityLog() {}

    public ActivityLog(User user, String category, String subType, Double amount, Double emission, LocalDate logDate, String notes) {
        this.user = user;
        this.category = category;
        this.subType = subType;
        this.amount = amount;
        this.emission = emission;
        this.logDate = logDate;
        this.notes = notes;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSubType() { return subType; }
    public void setSubType(String subType) { this.subType = subType; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public Double getEmission() { return emission; }
    public void setEmission(Double emission) { this.emission = emission; }

    public LocalDate getLogDate() { return logDate; }
    public void setLogDate(LocalDate logDate) { this.logDate = logDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
