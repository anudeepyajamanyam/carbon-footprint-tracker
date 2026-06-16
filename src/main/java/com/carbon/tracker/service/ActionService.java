package com.carbon.tracker.service;

import com.carbon.tracker.model.Action;
import com.carbon.tracker.model.User;
import com.carbon.tracker.model.UserAction;
import com.carbon.tracker.model.ActivityLog;
import com.carbon.tracker.repository.ActionRepository;
import com.carbon.tracker.repository.UserActionRepository;
import com.carbon.tracker.repository.ActivityLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ActionService {

    private final ActionRepository actionRepository;
    private final UserActionRepository userActionRepository;
    private final ActivityLogRepository activityLogRepository;

    public ActionService(ActionRepository actionRepository, UserActionRepository userActionRepository, ActivityLogRepository activityLogRepository) {
        this.actionRepository = actionRepository;
        this.userActionRepository = userActionRepository;
        this.activityLogRepository = activityLogRepository;
    }

    public List<Action> getAllActions() {
        return actionRepository.findAll();
    }

    @Transactional
    public UserAction optInToAction(User user, Long actionId, String challengedBy) {
        Action action = actionRepository.findById(actionId)
                .orElseThrow(() -> new IllegalArgumentException("Action/Challenge not found"));

        // Check if user already opted in to this action as ACTIVE
        boolean exists = userActionRepository.existsByUserAndActionAndStatus(user, action, "ACTIVE");
        if (exists) {
            throw new IllegalArgumentException("You have already opted into this challenge");
        }

        UserAction userAction = new UserAction(user, action, "ACTIVE", LocalDate.now());
        userAction.setChallengedBy(challengedBy);
        return userActionRepository.save(userAction);
    }

    @Transactional
    public UserAction completeAction(User user, Long userActionId) {
        UserAction userAction = userActionRepository.findById(userActionId)
                .orElseThrow(() -> new IllegalArgumentException("User Action not found"));

        if (!userAction.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized access to this challenge");
        }

        LocalDate today = LocalDate.now();
        LocalDate lastCompletion = userAction.getCompletionDate();

        if (lastCompletion != null) {
            if (lastCompletion.equals(today)) {
                throw new IllegalArgumentException("You have already completed this challenge today.");
            } else if (lastCompletion.equals(today.minusDays(1))) {
                userAction.setStreakDays(userAction.getStreakDays() + 1);
            } else {
                userAction.setStreakDays(1);
            }
        } else {
            userAction.setStreakDays(1);
        }

        userAction.setTotalCompletions(userAction.getTotalCompletions() + 1);
        userAction.setCompletionDate(today);

        // Log offset activity to the user's activity log history
        ActivityLog log = new ActivityLog(
                user,
                userAction.getAction().getCategory(),
                "Challenge: " + userAction.getAction().getTitle(),
                1.0,
                -userAction.getAction().getCo2Savings(),
                today,
                "Habit completed! Streak: " + userAction.getStreakDays() + " days."
        );
        activityLogRepository.save(log);
        
        return userActionRepository.save(userAction);
    }

    @Transactional
    public void abandonAction(User user, Long userActionId) {
        UserAction userAction = userActionRepository.findById(userActionId)
                .orElseThrow(() -> new IllegalArgumentException("User Action not found"));

        if (!userAction.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized access to this challenge");
        }

        userActionRepository.delete(userAction);
    }

    public List<UserAction> getUserActions(User user) {
        return userActionRepository.findByUser(user);
    }

    public Double calculateTotalSavedCo2(User user) {
        List<UserAction> activeActions = userActionRepository.findByUser(user);
        return activeActions.stream()
                .mapToDouble(ua -> ua.getTotalCompletions() * ua.getAction().getCo2Savings())
                .sum();
    }
}
