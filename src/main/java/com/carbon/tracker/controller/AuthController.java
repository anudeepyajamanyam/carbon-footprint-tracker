package com.carbon.tracker.controller;

import com.carbon.tracker.dto.AuthRequest;
import com.carbon.tracker.dto.AuthResponse;
import com.carbon.tracker.repository.UserRepository;
import com.carbon.tracker.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = userService.registerUser(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = userService.loginUser(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Real-time availability check for username and/or email.
     * Used for inline validation feedback as the user types.
     * GET /api/auth/check?username=foo&email=bar@example.com
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkAvailability(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email) {

        Map<String, Object> result = new HashMap<>();

        if (username != null && !username.isBlank()) {
            boolean taken = userRepository.existsByUsername(username.trim());
            result.put("usernameTaken", taken);
        }

        if (email != null && !email.isBlank()) {
            boolean taken = userRepository.existsByEmail(email.trim());
            result.put("emailTaken", taken);
        }

        return ResponseEntity.ok(result);
    }
}
