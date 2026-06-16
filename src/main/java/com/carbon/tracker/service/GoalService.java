package com.carbon.tracker.service;

import com.carbon.tracker.dto.GoalRequest;
import com.carbon.tracker.model.Goal;
import com.carbon.tracker.model.User;
import com.carbon.tracker.repository.ActivityLogRepository;
import com.carbon.tracker.repository.GoalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

@Service
public class GoalService {

    private final GoalRepository goalRepository;
    private final ActivityLogRepository activityLogRepository;

    public GoalService(GoalRepository goalRepository, ActivityLogRepository activityLogRepository) {
        this.goalRepository = goalRepository;
        this.activityLogRepository = activityLogRepository;
    }

    @Transactional
    public Goal setGoal(User user, GoalRequest request) {
        Optional<Goal> existingGoal = goalRepository.findByUserAndMonthAndYear(
                user, request.getMonth(), request.getYear()
        );

        Goal goal;
        if (existingGoal.isPresent()) {
            goal = existingGoal.get();
            goal.setTargetEmission(request.getTargetEmission());
        } else {
            goal = new Goal(
                    user,
                    request.getTargetEmission(),
                    request.getMonth(),
                    request.getYear(),
                    "IN_PROGRESS"
            );
        }

        updateGoalStatus(user, goal);
        return goalRepository.save(goal);
    }

    public List<Goal> getUserGoals(User user) {
        List<Goal> goals = goalRepository.findByUser(user);
        for (Goal goal : goals) {
            updateGoalStatus(user, goal);
        }
        return goals;
    }

    public Optional<Goal> getGoalForMonth(User user, int month, int year) {
        Optional<Goal> goalOpt = goalRepository.findByUserAndMonthAndYear(user, month, year);
        goalOpt.ifPresent(goal -> updateGoalStatus(user, goal));
        return goalOpt;
    }

    public void updateGoalStatus(User user, Goal goal) {
        LocalDate start = LocalDate.of(goal.getYear(), goal.getMonth(), 1);
        LocalDate end = start.with(TemporalAdjusters.lastDayOfMonth());

        Double currentEmissions = activityLogRepository.sumEmissionsByUserAndDateRange(user, start, end);
        if (currentEmissions == null) {
            currentEmissions = 0.0;
        }

        if (currentEmissions > goal.getTargetEmission()) {
            goal.setStatus("EXCEEDED");
        } else {
            // If the month has already passed, we can mark it as ACHIEVED
            LocalDate today = LocalDate.now();
            if (today.isAfter(end)) {
                goal.setStatus("ACHIEVED");
            } else {
                goal.setStatus("IN_PROGRESS");
            }
        }
    }
}
