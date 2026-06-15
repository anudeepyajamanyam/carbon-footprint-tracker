package com.carbon.tracker.service;

import com.carbon.tracker.dto.AuthRequest;
import com.carbon.tracker.dto.AuthResponse;
import com.carbon.tracker.dto.UpdateUserRequest;
import com.carbon.tracker.model.User;
import com.carbon.tracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String testUsername;
    private String testEmail;

    @BeforeEach
    public void setUp() {
        testUsername = "test_user_" + System.currentTimeMillis();
        testEmail = "test_user_" + System.currentTimeMillis() + "@example.com";
    }

    @Test
    public void testRegisterUser_Success() {
        AuthRequest request = new AuthRequest();
        request.setUsername(testUsername);
        request.setEmail(testEmail);
        request.setPassword("strongPassword123!");

        AuthResponse response = userService.registerUser(request);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals(testUsername, response.getUsername());
        assertEquals(testEmail, response.getEmail());

        Optional<User> userOpt = userRepository.findByUsername(testUsername);
        assertTrue(userOpt.isPresent());
        User user = userOpt.get();
        assertTrue(passwordEncoder.matches("strongPassword123!", user.getPassword()));
        assertEquals("ROLE_USER", user.getRole());
    }

    @Test
    public void testRegisterUser_DuplicateUsername() {
        AuthRequest request = new AuthRequest();
        request.setUsername(testUsername);
        request.setEmail(testEmail);
        request.setPassword("strongPassword123!");

        userService.registerUser(request);

        // Try registering duplicate username
        AuthRequest duplicateRequest = new AuthRequest();
        duplicateRequest.setUsername(testUsername);
        duplicateRequest.setEmail("different_" + testEmail);
        duplicateRequest.setPassword("anotherPassword");

        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(duplicateRequest));
    }

    @Test
    public void testRegisterUser_DuplicateEmail() {
        AuthRequest request = new AuthRequest();
        request.setUsername(testUsername);
        request.setEmail(testEmail);
        request.setPassword("strongPassword123!");

        userService.registerUser(request);

        // Try registering duplicate email
        AuthRequest duplicateRequest = new AuthRequest();
        duplicateRequest.setUsername("different_" + testUsername);
        duplicateRequest.setEmail(testEmail);
        duplicateRequest.setPassword("anotherPassword");

        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(duplicateRequest));
    }

    @Test
    public void testLoginUser_Success() {
        AuthRequest registerRequest = new AuthRequest();
        registerRequest.setUsername(testUsername);
        registerRequest.setEmail(testEmail);
        registerRequest.setPassword("strongPassword123!");
        userService.registerUser(registerRequest);

        AuthRequest loginRequest = new AuthRequest();
        loginRequest.setUsername(testUsername);
        loginRequest.setPassword("strongPassword123!");

        AuthResponse loginResponse = userService.loginUser(loginRequest);

        assertNotNull(loginResponse);
        assertNotNull(loginResponse.getToken());
        assertEquals(testUsername, loginResponse.getUsername());
    }

    @Test
    public void testLoginUser_IncorrectPassword() {
        AuthRequest registerRequest = new AuthRequest();
        registerRequest.setUsername(testUsername);
        registerRequest.setEmail(testEmail);
        registerRequest.setPassword("strongPassword123!");
        userService.registerUser(registerRequest);

        AuthRequest loginRequest = new AuthRequest();
        loginRequest.setUsername(testUsername);
        loginRequest.setPassword("wrongPassword");

        assertThrows(IllegalArgumentException.class, () -> userService.loginUser(loginRequest));
    }

    @Test
    public void testLoginUser_UnregisteredUser() {
        AuthRequest loginRequest = new AuthRequest();
        loginRequest.setUsername("nonexistent_user");
        loginRequest.setPassword("somePassword");

        assertThrows(IllegalArgumentException.class, () -> userService.loginUser(loginRequest));
    }

    @Test
    public void testUpdateUser_Success() {
        AuthRequest registerRequest = new AuthRequest();
        registerRequest.setUsername(testUsername);
        registerRequest.setEmail(testEmail);
        registerRequest.setPassword("strongPassword123!");
        userService.registerUser(registerRequest);

        User currentUser = userRepository.findByUsername(testUsername).orElseThrow();

        UpdateUserRequest updateRequest = new UpdateUserRequest();
        updateRequest.setUsername(testUsername + "_new");
        updateRequest.setEmail("new_" + testEmail);
        updateRequest.setPassword("newStrongPassword123!");

        AuthResponse updateResponse = userService.updateUser(currentUser, updateRequest);

        assertNotNull(updateResponse);
        assertEquals(testUsername + "_new", updateResponse.getUsername());
        assertEquals("new_" + testEmail, updateResponse.getEmail());

        User updatedUser = userRepository.findById(currentUser.getId()).orElseThrow();
        assertEquals(testUsername + "_new", updatedUser.getUsername());
        assertEquals("new_" + testEmail, updatedUser.getEmail());
        assertTrue(passwordEncoder.matches("newStrongPassword123!", updatedUser.getPassword()));
    }
}
