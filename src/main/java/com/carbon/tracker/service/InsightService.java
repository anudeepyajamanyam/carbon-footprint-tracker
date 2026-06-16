package com.carbon.tracker.service;

import com.carbon.tracker.dto.ActivityLogResponse;
import com.carbon.tracker.dto.DashboardResponse;
import com.carbon.tracker.model.Goal;
import com.carbon.tracker.model.User;
import com.carbon.tracker.repository.ActivityLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InsightService {

    private final ActivityLogRepository activityLogRepository;
    private final ActivityLogService activityLogService;
    private final GoalService goalService;
    private final ActionService actionService;

    public InsightService(ActivityLogRepository activityLogRepository, ActivityLogService activityLogService, GoalService goalService, ActionService actionService) {
        this.activityLogRepository = activityLogRepository;
        this.activityLogService = activityLogService;
        this.goalService = goalService;
        this.actionService = actionService;
    }

    @Cacheable(value = "dashboard", key = "#user.username")
    public DashboardResponse getDashboardData(User user) {
        LocalDate today = LocalDate.now();
        
        // Current Month range
        LocalDate currentMonthStart = today.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate currentMonthEnd = today.with(TemporalAdjusters.lastDayOfMonth());
        
        // Previous Month range
        LocalDate previousMonthStart = today.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth());
        LocalDate previousMonthEnd = today.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());

        // Sum emissions
        Double currentEmissions = activityLogRepository.sumEmissionsByUserAndDateRange(user, currentMonthStart, currentMonthEnd);
        Double previousEmissions = activityLogRepository.sumEmissionsByUserAndDateRange(user, previousMonthStart, previousMonthEnd);
        
        // Saved CO2 from completed challenges
        Double totalSaved = actionService.calculateTotalSavedCo2(user);

        // Current goal limit
        Double currentGoal = null;
        Goal goal = goalService.getGoalForMonth(user, today.getMonthValue(), today.getYear()).orElse(null);
        if (goal != null) {
            currentGoal = goal.getTargetEmission();
        }

        // Category breakdown
        List<Object[]> results = activityLogRepository.sumEmissionsByCategoryAndDateRange(user, currentMonthStart, currentMonthEnd);
        Map<String, Double> breakdown = new HashMap<>();
        breakdown.put("TRANSPORT", 0.0);
        breakdown.put("ENERGY", 0.0);
        breakdown.put("FOOD", 0.0);
        breakdown.put("WASTE", 0.0);
        
        for (Object[] result : results) {
            String category = (String) result[0];
            Double sum = (Double) result[1];
            if (category != null) {
                breakdown.put(category.toUpperCase(), sum);
            }
        }

        // Recent 5 logs
        List<ActivityLogResponse> recentLogs = activityLogService.getUserLogs(user)
                .stream()
                .limit(5)
                .collect(java.util.stream.Collectors.toList());

        // Generate recommendations
        List<String> tips = generateTips(breakdown, currentEmissions, currentGoal);

        return new DashboardResponse(
                currentEmissions,
                previousEmissions,
                totalSaved,
                currentGoal,
                breakdown,
                recentLogs,
                tips
        );
    }

    private List<String> generateTips(Map<String, Double> breakdown, Double totalEmissions, Double goal) {
        List<String> tips = new ArrayList<>();

        if (totalEmissions == 0.0) {
            tips.add("Welcome to Carbon Tracker! Start by logging your travel, food choices, or utilities in the Quick Logger.");
            tips.add("Tip: Opt-in to easy challenges like 'Meatless Monday' or 'Switch to LED Bulbs' under the Challenges tab.");
            return tips;
        }

        double transport = breakdown.getOrDefault("TRANSPORT", 0.0);
        double energy = breakdown.getOrDefault("ENERGY", 0.0);
        double food = breakdown.getOrDefault("FOOD", 0.0);
        double waste = breakdown.getOrDefault("WASTE", 0.0);

        // Find highest contributor
        String highestCategory = "TRANSPORT";
        double highestVal = transport;

        if (energy > highestVal) {
            highestCategory = "ENERGY";
            highestVal = energy;
        }
        if (food > highestVal) {
            highestCategory = "FOOD";
            highestVal = food;
        }
        if (waste > highestVal) {
            highestCategory = "WASTE";
            highestVal = waste;
        }

        // Generate customized tip based on highest category
        switch (highestCategory) {
            case "TRANSPORT":
                tips.add("Transport is your biggest carbon contributor this month. Try sharing rides, using public transit, or cycling short trips.");
                break;
            case "ENERGY":
                tips.add("Your home energy consumption is high. Lowering your thermostat by 1°C or unplugging standby electronics can reduce bills and emissions.");
                break;
            case "FOOD":
                tips.add("Food choices represent your primary emissions. Swapping even 2 beef/pork meals per week with vegetarian options reduces footprint significantly.");
                break;
            case "WASTE":
                tips.add("Waste emissions are prominent. Start composting food scraps and ensuring plastic/glass/paper are correctly recycled to minimize landfill gas.");
                break;
        }

        // Goal check tip
        if (goal != null) {
            if (totalEmissions > goal) {
                tips.add("Warning: You have exceeded your carbon budget of " + goal + " kg CO2 for this month. Try selecting easy reduction habits to get back on track.");
            } else if (totalEmissions > goal * 0.8) {
                tips.add("Keep it up: You are close to your monthly carbon budget. Monitor your energy and transit logs closely.");
            } else {
                tips.add("Great job: You are well within your monthly carbon budget. Continue logging to maintain your performance!");
            }
        } else {
            tips.add("Setting a monthly carbon budget under the 'Goals' tab can help structure your reduction efforts.");
        }

        // General ecological facts/tips
        tips.add("Did you know? Plant-based meals have on average 70% lower greenhouse gas emissions compared to animal-based options.");
        tips.add("Did you know? Phantom loads from standby appliances account for up to 10% of standard household electric usage.");

        return tips;
    }
}
