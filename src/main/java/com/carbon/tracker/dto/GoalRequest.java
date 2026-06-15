package com.carbon.tracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class GoalRequest {

    @NotNull(message = "Target emission is required")
    @DecimalMin(value = "0.1", message = "Target emission must be greater than zero")
    private Double targetEmission;

    @NotNull(message = "Month is required")
    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    private Integer month;

    @NotNull(message = "Year is required")
    @Min(value = 2020, message = "Year must be valid")
    private Integer year;

    public GoalRequest() {}

    public Double getTargetEmission() { return targetEmission; }
    public void setTargetEmission(Double targetEmission) { this.targetEmission = targetEmission; }

    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
}
