package com.carbon.tracker.service;

import com.carbon.tracker.dto.ActivityLogRequest;
import com.carbon.tracker.dto.ActivityLogResponse;
import com.carbon.tracker.model.ActivityLog;
import com.carbon.tracker.model.User;
import com.carbon.tracker.repository.ActivityLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ActivityLogService {

    @Autowired
    private ActivityLogRepository activityLogRepository;

    public Double calculateEmissions(String category, String subType, Double amount) {
        if (amount == null || amount < 0) {
            return 0.0;
        }

        switch (category.toUpperCase()) {
            case "TRANSPORT":
                switch (subType) {
                    case "Petrol Car": return amount * 0.18;
                    case "Diesel Car": return amount * 0.17;
                    case "Electric Car": return amount * 0.05;
                    case "Bus": return amount * 0.08;
                    case "Train": return amount * 0.04;
                    case "Flight": return amount * 0.22;
                    case "Bicycle/Walking": return 0.0;
                    case "Walking Step Offset": return - (amount * 0.75 / 1000.0) * 0.18;
                    case "Walk Distance Offset": return -(amount * 0.18); // km * 0.18 kg/km saved
                    default: return amount * 0.10;
                }
            case "ENERGY":
                switch (subType) {
                    case "Electricity": return amount * 0.45;
                    case "Natural Gas": return amount * 0.18;
                    case "LPG / Propane": return amount * 3.0;
                    case "Coal": return amount * 2.42;
                    default: return amount * 0.30;
                }
            case "FOOD":
                switch (subType) {
                    case "Meat-Heavy Meal": return amount * 2.5;
                    case "Average Meal": return amount * 1.5;
                    case "Vegetarian Meal": return amount * 0.80;
                    case "Vegan Meal": return amount * 0.50;
                    default: return amount * 1.2;
                }
            case "WASTE":
                switch (subType) {
                    case "General Waste / Landfill": return amount * 1.5;
                    case "Recyclable Waste": return amount * 0.2;
                    case "Organic Waste": return amount * 0.1;
                    default: return amount * 0.8;
                }
            default:
                return amount * 0.1;
        }
    }

    @Transactional
    public ActivityLogResponse logActivity(User user, ActivityLogRequest request) {
        // Duplicate-date guard for walking offset logs (prevents gaming)
        if ("Walk Distance Offset".equals(request.getSubType()) || "Walking Step Offset".equals(request.getSubType())) {
            LocalDate logDate = request.getLogDate() != null ? request.getLogDate() : LocalDate.now();
            boolean alreadyLoggedToday = activityLogRepository
                    .findByUserOrderByLogDateDesc(user)
                    .stream()
                    .anyMatch(l -> ("Walk Distance Offset".equals(l.getSubType()) || "Walking Step Offset".equals(l.getSubType()))
                            && logDate.equals(l.getLogDate()));
            if (alreadyLoggedToday) {
                throw new IllegalArgumentException("You have already logged a walking offset for today. Come back tomorrow!");
            }
        }

        Double emission = calculateEmissions(request.getCategory(), request.getSubType(), request.getAmount());

        ActivityLog log = new ActivityLog(
                user,
                request.getCategory().toUpperCase(),
                request.getSubType(),
                request.getAmount(),
                emission,
                request.getLogDate(),
                request.getNotes()
        );

        ActivityLog savedLog = activityLogRepository.save(log);
        return mapToResponse(savedLog);
    }

    public List<ActivityLogResponse> getUserLogs(User user) {
        return activityLogRepository.findByUserOrderByLogDateDesc(user)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteLog(User user, Long logId) {
        ActivityLog log = activityLogRepository.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("Activity log not found"));

        if (!log.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized access to delete this log");
        }

        activityLogRepository.delete(log);
    }

    public Double getEmissionsInPeriod(User user, LocalDate start, LocalDate end) {
        return activityLogRepository.sumEmissionsByUserAndDateRange(user, start, end);
    }

    public List<Object[]> getEmissionsByCategoryInPeriod(User user, LocalDate start, LocalDate end) {
        return activityLogRepository.sumEmissionsByCategoryAndDateRange(user, start, end);
    }

    private ActivityLogResponse mapToResponse(ActivityLog log) {
        return new ActivityLogResponse(
                log.getId(),
                log.getCategory(),
                log.getSubType(),
                log.getAmount(),
                log.getEmission(),
                log.getLogDate(),
                log.getNotes()
        );
    }
}
