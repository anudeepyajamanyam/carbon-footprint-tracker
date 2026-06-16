package com.carbon.tracker.service;

import com.carbon.tracker.dto.ActivityLogRequest;
import com.carbon.tracker.dto.DashboardResponse;
import com.carbon.tracker.dto.GoalRequest;
import com.carbon.tracker.model.User;
import com.carbon.tracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class InsightServiceTest {

    @Autowired
    private InsightService insightService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityLogService activityLogService;

    @Autowired
    private GoalService goalService;

    private User testUser;

    @BeforeEach
    public void setUp() {
        testUser = new User("insightuser_" + System.currentTimeMillis(), "insightuser@mail.com", "password", "ROLE_USER");
        userRepository.save(testUser);
    }

    @Test
    public void testGetDashboardData_Empty() {
        DashboardResponse response = insightService.getDashboardData(testUser);
        assertNotNull(response);
        assertEquals(0.0, response.getCurrentMonthEmissions(), 0.001);
        assertEquals(0.0, response.getPreviousMonthEmissions(), 0.001);
        assertEquals(0.0, response.getTotalSavedCo2(), 0.001);
        assertNull(response.getCurrentMonthGoal());
        assertTrue(response.getRecommendationTips().contains("Welcome to Carbon Tracker! Start by logging your travel, food choices, or utilities in the Quick Logger."));
    }

    @Test
    public void testGetDashboardData_WithDataAndGoal() {
        LocalDate now = LocalDate.now();

        // Set monthly goal
        GoalRequest goalRequest = new GoalRequest();
        goalRequest.setTargetEmission(50.0);
        goalRequest.setMonth(now.getMonthValue());
        goalRequest.setYear(now.getYear());
        goalService.setGoal(testUser, goalRequest);

        // Log some activities
        ActivityLogRequest log1 = new ActivityLogRequest();
        log1.setCategory("TRANSPORT");
        log1.setSubType("Petrol Car");
        log1.setAmount(100.0); // 100 * 0.18 = 18.0 kg CO2
        log1.setLogDate(now);
        activityLogService.logActivity(testUser, log1);

        ActivityLogRequest log2 = new ActivityLogRequest();
        log2.setCategory("FOOD");
        log2.setSubType("Vegetarian Meal");
        log2.setAmount(1.0); // 1 * 0.8 = 0.8 kg CO2
        log2.setLogDate(now);
        activityLogService.logActivity(testUser, log2);

        DashboardResponse response = insightService.getDashboardData(testUser);
        assertNotNull(response);
        assertEquals(18.8, response.getCurrentMonthEmissions(), 0.001);
        assertEquals(50.0, response.getCurrentMonthGoal(), 0.001);
        assertEquals(18.0, response.getCategoryBreakdown().get("TRANSPORT"), 0.001);
        assertEquals(0.8, response.getCategoryBreakdown().get("FOOD"), 0.001);

        // Since transport (18.0) > food (0.8), transport tip should be recommended
        assertTrue(response.getRecommendationTips().stream().anyMatch(tip -> tip.contains("Transport is your biggest carbon contributor")));
    }
}
