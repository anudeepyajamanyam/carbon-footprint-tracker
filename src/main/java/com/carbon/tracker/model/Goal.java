package com.carbon.tracker.model;

import jakarta.persistence.*;

@Entity
@Table(
    name = "goals",
    indexes = {
        @Index(name = "idx_goals_user_month_year", columnList = "user_id, goal_month, goal_year")
    }
)
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user;

    @Column(name = "target_emission", nullable = false)
    private Double targetEmission; // target max CO2 emission in kg for the month

    @Column(name = "goal_month", nullable = false)
    private Integer month; // 1 - 12

    @Column(name = "goal_year", nullable = false)
    private Integer year;

    @Column(nullable = false)
    private String status; // IN_PROGRESS, ACHIEVED, EXCEEDED

    public Goal() {}

    public Goal(User user, Double targetEmission, Integer month, Integer year, String status) {
        this.user = user;
        this.targetEmission = targetEmission;
        this.month = month;
        this.year = year;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Double getTargetEmission() { return targetEmission; }
    public void setTargetEmission(Double targetEmission) { this.targetEmission = targetEmission; }

    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
