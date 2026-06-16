package com.carbon.tracker.service;

import com.carbon.tracker.config.JwtTokenProvider;
import com.carbon.tracker.dto.AuthRequest;
import com.carbon.tracker.dto.AuthResponse;
import com.carbon.tracker.dto.UpdateUserRequest;
import com.carbon.tracker.model.User;
import com.carbon.tracker.repository.ActivityLogRepository;
import com.carbon.tracker.repository.GoalRepository;
import com.carbon.tracker.repository.UserActionRepository;
import com.carbon.tracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final GoalRepository goalRepository;
    private final UserActionRepository userActionRepository;
    private final ActivityLogRepository activityLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public UserService(UserRepository userRepository, GoalRepository goalRepository, UserActionRepository userActionRepository, ActivityLogRepository activityLogRepository, PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.goalRepository = goalRepository;
        this.userActionRepository = userActionRepository;
        this.activityLogRepository = activityLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public AuthResponse registerUser(AuthRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        User user = new User(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                "ROLE_USER"
        );

        userRepository.save(user);

        String token = tokenProvider.generateToken(user.getUsername());
        return new AuthResponse(token, user.getUsername(), user.getEmail());
    }

    public AuthResponse loginUser(AuthRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not registered"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Incorrect password");
        }

        String token = tokenProvider.generateToken(user.getUsername());
        return new AuthResponse(token, user.getUsername(), user.getEmail());
    }

    @Transactional
    public AuthResponse updateUser(User currentUser, UpdateUserRequest request) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!user.getUsername().equalsIgnoreCase(request.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new IllegalArgumentException("Username is already taken");
            }
            user.setUsername(request.getUsername());
        }

        if (!user.getEmail().equalsIgnoreCase(request.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email is already registered");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            if (request.getPassword().trim().length() < 8) {
                throw new IllegalArgumentException("Password must be at least 8 characters");
            }
            user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        }

        userRepository.save(user);

        // Re-generate token with new/current username
        String token = tokenProvider.generateToken(user.getUsername());
        return new AuthResponse(token, user.getUsername(), user.getEmail());
    }

    @Transactional
    public void deleteUser(User currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        userActionRepository.deleteByUser(user);
        goalRepository.deleteByUser(user);
        activityLogRepository.deleteByUser(user);
        userRepository.delete(user);
    }
}
