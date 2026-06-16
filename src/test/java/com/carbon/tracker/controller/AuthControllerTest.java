package com.carbon.tracker.controller;

import com.carbon.tracker.dto.AuthRequest;
import com.carbon.tracker.dto.AuthResponse;
import com.carbon.tracker.model.User;
import com.carbon.tracker.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private String testUsername;
    private String testEmail;

    @BeforeEach
    public void setUp() {
        testUsername = "auth_ctrl_user_" + System.currentTimeMillis();
        testEmail = "auth_ctrl_user_" + System.currentTimeMillis() + "@example.com";
    }

    @Test
    public void testRegister_Success() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setUsername(testUsername);
        request.setEmail(testEmail);
        request.setPassword("strongPassword123!");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.username", is(testUsername)))
                .andExpect(jsonPath("$.email", is(testEmail)));
    }

    @Test
    public void testRegister_Conflict() throws Exception {
        User existingUser = new User(testUsername, testEmail, passwordEncoder.encode("strongPassword123!"), "ROLE_USER");
        userRepository.save(existingUser);

        AuthRequest request = new AuthRequest();
        request.setUsername(testUsername);
        request.setEmail(testEmail);
        request.setPassword("strongPassword123!");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    public void testLogin_Success() throws Exception {
        User existingUser = new User(testUsername, testEmail, passwordEncoder.encode("strongPassword123!"), "ROLE_USER");
        userRepository.save(existingUser);

        AuthRequest request = new AuthRequest();
        request.setUsername(testUsername);
        request.setPassword("strongPassword123!");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.username", is(testUsername)));
    }

    @Test
    public void testLogin_Failure() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setUsername("nonexistent_user");
        request.setPassword("wrongPassword");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testCheckAvailability() throws Exception {
        User existingUser = new User(testUsername, testEmail, passwordEncoder.encode("strongPassword123!"), "ROLE_USER");
        userRepository.save(existingUser);

        // Check availability where username/email are taken
        mockMvc.perform(get("/api/auth/check")
                .param("username", testUsername)
                .param("email", testEmail))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usernameTaken", is(true)))
                .andExpect(jsonPath("$.emailTaken", is(true)));

        // Check availability where username/email are free
        mockMvc.perform(get("/api/auth/check")
                .param("username", "free_username")
                .param("email", "free_email@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usernameTaken", is(false)))
                .andExpect(jsonPath("$.emailTaken", is(false)));
    }
}
