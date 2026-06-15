package com.carbon.tracker.service;

import com.carbon.tracker.dto.ActivityLogRequest;
import com.carbon.tracker.dto.ActivityLogResponse;
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
public class ActivityLogServiceTest {

    @Autowired
    private ActivityLogService activityLogService;

    @Autowired
    private UserRepository userRepository;

    private User testUser1;
    private User testUser2;

    @BeforeEach
    public void setUp() {
        testUser1 = new User("user1_" + System.currentTimeMillis(), "email1_" + System.currentTimeMillis() + "@mail.com", "pass", "ROLE_USER");
        testUser2 = new User("user2_" + System.currentTimeMillis(), "email2_" + System.currentTimeMillis() + "@mail.com", "pass", "ROLE_USER");
        userRepository.save(testUser1);
        userRepository.save(testUser2);
    }

    @Test
    public void testCalculateEmissions() {
        // Petrol Car: 0.18 kg CO2/km
        assertEquals(1.8, activityLogService.calculateEmissions("TRANSPORT", "Petrol Car", 10.0), 0.001);
        // Vegetarian Meal: 0.8 kg CO2/meal
        assertEquals(2.4, activityLogService.calculateEmissions("FOOD", "Vegetarian Meal", 3.0), 0.001);
        // Walk Distance Offset: -0.18 kg CO2/km
        assertEquals(-0.9, activityLogService.calculateEmissions("TRANSPORT", "Walk Distance Offset", 5.0), 0.001);
        // Electric Car: 0.05 kg CO2/km
        assertEquals(0.5, activityLogService.calculateEmissions("TRANSPORT", "Electric Car", 10.0), 0.001);
    }

    @Test
    public void testLogActivity_Success() {
        ActivityLogRequest request = new ActivityLogRequest();
        request.setCategory("TRANSPORT");
        request.setSubType("Petrol Car");
        request.setAmount(10.0);
        request.setLogDate(LocalDate.now());
        request.setNotes("Commute to office");

        ActivityLogResponse response = activityLogService.logActivity(testUser1, request);

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals("TRANSPORT", response.getCategory());
        assertEquals("Petrol Car", response.getSubType());
        assertEquals(10.0, response.getAmount());
        assertEquals(1.8, response.getEmission(), 0.001);
    }

    @Test
    public void testLogActivity_DuplicateWalkingOffsetGuard() {
        ActivityLogRequest request1 = new ActivityLogRequest();
        request1.setCategory("TRANSPORT");
        request1.setSubType("Walk Distance Offset");
        request1.setAmount(5.0);
        request1.setLogDate(LocalDate.now());

        activityLogService.logActivity(testUser1, request1);

        // Try logging walking offset on same date again
        ActivityLogRequest request2 = new ActivityLogRequest();
        request2.setCategory("TRANSPORT");
        request2.setSubType("Walk Distance Offset");
        request2.setAmount(3.0);
        request2.setLogDate(LocalDate.now());

        assertThrows(IllegalArgumentException.class, () -> activityLogService.logActivity(testUser1, request2));
    }

    @Test
    public void testDeleteLog_OwnershipGuard() {
        ActivityLogRequest request = new ActivityLogRequest();
        request.setCategory("TRANSPORT");
        request.setSubType("Petrol Car");
        request.setAmount(10.0);
        request.setLogDate(LocalDate.now());

        ActivityLogResponse logged = activityLogService.logActivity(testUser1, request);

        // Try deleting user1's log using user2
        assertThrows(SecurityException.class, () -> activityLogService.deleteLog(testUser2, logged.getId()));

        // Succeeds with user1
        assertDoesNotThrow(() -> activityLogService.deleteLog(testUser1, logged.getId()));
    }
}
