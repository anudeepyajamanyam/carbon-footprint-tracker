package com.carbon.tracker.controller;

import com.carbon.tracker.dto.AuthResponse;
import com.carbon.tracker.dto.UpdateUserRequest;
import com.carbon.tracker.model.User;
import com.carbon.tracker.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping("/profile")
    @CacheEvict(value = "dashboard", key = "#user.username")
    public ResponseEntity<AuthResponse> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateUserRequest request) {
        AuthResponse response = userService.updateUser(user, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/profile")
    @CacheEvict(value = "dashboard", key = "#user.username")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal User user) {
        userService.deleteUser(user);
        return ResponseEntity.noContent().build();
    }
}
