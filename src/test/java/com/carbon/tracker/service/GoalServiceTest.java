package com.carbon.tracker.service;

import com.carbon.tracker.dto.GoalRequest;
import com.carbon.tracker.dto.ActivityLogRequest;
import com.carbon.tracker.model.Goal;
import com.carbon.tracker.model.User;
import com.carbon.tracker.repository.UserRepository;
import com.carbon.tracker.repository.GoalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class GoalServiceTest {

    @Autowired
    private GoalService goalService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private ActivityLogService activityLogService;

    private User testUser;

    @BeforeEach
    public void setUp() {
        testUser = new User("goaluser_" + System.currentTimeMillis(), "goaluser@mail.com", "password", "ROLE_USER");
        userRepository.save(testUser);
    }

    @Test
    public void testSetGoal_NewAndExisting() {
        LocalDate now = LocalDate.now();
        GoalRequest request = new GoalRequest();
        request.setTargetEmission(100.0);
        request.setMonth(now.getMonthValue());
        request.setYear(now.getYear());

        Goal created = goalService.setGoal(testUser, request);
        assertNotNull(created);
        assertEquals(100.0, created.getTargetEmission());
        assertEquals("IN_PROGRESS", created.getStatus());

        // Update target emissions
        request.setTargetEmission(80.0);
        Goal updated = goalService.setGoal(testUser, request);
        assertEquals(created.getId(), updated.getId());
        assertEquals(80.0, updated.getTargetEmission());
    }

    @Test
    public void testGetUserGoalsAndGetGoalForMonth() {
        LocalDate now = LocalDate.now();
        GoalRequest request = new GoalRequest();
        request.setTargetEmission(150.0);
        request.setMonth(now.getMonthValue());
        request.setYear(now.getYear());

        goalService.setGoal(testUser, request);

        List<Goal> goals = goalService.getUserGoals(testUser);
        assertEquals(1, goals.size());

        Optional<Goal> fetched = goalService.getGoalForMonth(testUser, now.getMonthValue(), now.getYear());
        assertTrue(fetched.isPresent());
        assertEquals(150.0, fetched.get().getTargetEmission());
    }

    @Test
    public void testUpdateGoalStatus_Exceeded() {
        LocalDate now = LocalDate.now();
        GoalRequest request = new GoalRequest();
        request.setTargetEmission(5.0); // low target
        request.setMonth(now.getMonthValue());
        request.setYear(now.getYear());

        Goal goal = goalService.setGoal(testUser, request);
        assertEquals("IN_PROGRESS", goal.getStatus());

        // Log emissions that exceed the target
        ActivityLogRequest logRequest = new ActivityLogRequest();
        logRequest.setCategory("TRANSPORT");
        logRequest.setSubType("Petrol Car");
        logRequest.setAmount(100.0); // 100 * 0.18 = 18.0 kg CO2 (exceeds 5.0)
        logRequest.setLogDate(now);
        activityLogService.logActivity(testUser, logRequest);

        // Fetch again, status should update to EXCEEDED
        Goal updated = goalService.getGoalForMonth(testUser, now.getMonthValue(), now.getYear()).get();
        assertEquals("EXCEEDED", updated.getStatus());
    }
}
