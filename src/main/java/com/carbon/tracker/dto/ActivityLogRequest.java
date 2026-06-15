package com.carbon.tracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class ActivityLogRequest {

    @NotBlank(message = "Category is required")
    private String category;

    @NotBlank(message = "Sub-type is required")
    private String subType;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Amount must be zero or positive")
    private Double amount;

    @NotNull(message = "Log date is required")
    private LocalDate logDate;

    private String notes;

    public ActivityLogRequest() {}

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSubType() { return subType; }
    public void setSubType(String subType) { this.subType = subType; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public LocalDate getLogDate() { return logDate; }
    public void setLogDate(LocalDate logDate) { this.logDate = logDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
