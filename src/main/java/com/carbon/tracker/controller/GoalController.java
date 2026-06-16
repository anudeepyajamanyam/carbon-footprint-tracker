package com.carbon.tracker.controller;

import com.carbon.tracker.dto.GoalRequest;
import com.carbon.tracker.model.Goal;
import com.carbon.tracker.model.User;
import com.carbon.tracker.service.GoalService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping
    @CacheEvict(value = "dashboard", key = "#user.username")
    public ResponseEntity<Goal> createOrUpdateGoal(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody GoalRequest request) {
        Goal goal = goalService.setGoal(user, request);
        return ResponseEntity.ok(goal);
    }

    @GetMapping
    public ResponseEntity<List<Goal>> getGoals(@AuthenticationPrincipal User user) {
        List<Goal> goals = goalService.getUserGoals(user);
        return ResponseEntity.ok(goals);
    }

    @GetMapping("/current")
    public ResponseEntity<Goal> getCurrentGoal(@AuthenticationPrincipal User user) {
        LocalDate today = LocalDate.now();
        Goal goal = goalService.getGoalForMonth(user, today.getMonthValue(), today.getYear()).orElse(null);
        if (goal == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(goal);
    }
}
