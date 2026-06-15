package com.carbon.tracker.model;

import jakarta.persistence.*;

@Entity
@Table(name = "action")
public class Action {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false)
    private String category; // TRANSPORT, ENERGY, FOOD, WASTE

    @Column(name = "co2_savings", nullable = false)
    private Double co2Savings; // kg of CO2 saved per day/event

    @Column(nullable = false)
    private String difficulty; // EASY, MEDIUM, HARD

    public Action() {}

    public Action(String title, String description, String category, Double co2Savings, String difficulty) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.co2Savings = co2Savings;
        this.difficulty = difficulty;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Double getCo2Savings() { return co2Savings; }
    public void setCo2Savings(Double co2Savings) { this.co2Savings = co2Savings; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
}
