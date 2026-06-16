package com.carbon.tracker.controller;

import com.carbon.tracker.config.JwtTokenProvider;
import com.carbon.tracker.dto.ActivityLogRequest;
import com.carbon.tracker.model.ActivityLog;
import com.carbon.tracker.model.User;
import com.carbon.tracker.repository.ActivityLogRepository;
import com.carbon.tracker.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ActivityLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private String jwtToken;

    @BeforeEach
    public void setUp() {
        String username = "log_ctrl_user_" + System.currentTimeMillis();
        testUser = new User(username, username + "@example.com", "password123!", "ROLE_USER");
        testUser = userRepository.save(testUser);

        jwtToken = jwtTokenProvider.generateToken(username);
    }

    @Test
    public void testGetLogs_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/logs"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testCreateLog_Success() throws Exception {
        ActivityLogRequest request = new ActivityLogRequest();
        request.setCategory("TRANSPORT");
        request.setSubType("Petrol Car");
        request.setAmount(10.0);
        request.setLogDate(LocalDate.now());
        request.setNotes("Commute to office");

        mockMvc.perform(post("/api/logs")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.category").value("TRANSPORT"))
                .andExpect(jsonPath("$.emission", notNullValue()));
    }

    @Test
    public void testCreateLog_InvalidRequest() throws Exception {
        ActivityLogRequest request = new ActivityLogRequest();
        // Missing category and subType
        request.setAmount(-5.0); // Invalid amount
        request.setLogDate(LocalDate.now());

        mockMvc.perform(post("/api/logs")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetLogs_Success() throws Exception {
        ActivityLog log1 = new ActivityLog(testUser, "TRANSPORT", "Petrol Car", 10.0, 1.8, LocalDate.now(), "Notes 1");
        ActivityLog log2 = new ActivityLog(testUser, "FOOD", "Meat Meal", 1.0, 2.5, LocalDate.now(), "Notes 2");
        activityLogRepository.save(log1);
        activityLogRepository.save(log2);

        mockMvc.perform(get("/api/logs")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    public void testDeleteLog_Success() throws Exception {
        ActivityLog log = new ActivityLog(testUser, "TRANSPORT", "Petrol Car", 10.0, 1.8, LocalDate.now(), "Notes");
        log = activityLogRepository.save(log);

        mockMvc.perform(delete("/api/logs/" + log.getId())
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/logs")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
