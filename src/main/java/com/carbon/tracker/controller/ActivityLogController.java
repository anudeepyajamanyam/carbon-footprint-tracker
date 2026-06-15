package com.carbon.tracker.controller;

import com.carbon.tracker.dto.ActivityLogRequest;
import com.carbon.tracker.dto.ActivityLogResponse;
import com.carbon.tracker.model.User;
import com.carbon.tracker.service.ActivityLogService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
public class ActivityLogController {

    @Autowired
    private ActivityLogService activityLogService;

    @PostMapping
    @CacheEvict(value = "dashboard", key = "#user.username")
    public ResponseEntity<ActivityLogResponse> createLog(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ActivityLogRequest request) {
        ActivityLogResponse response = activityLogService.logActivity(user, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ActivityLogResponse>> getLogs(@AuthenticationPrincipal User user) {
        List<ActivityLogResponse> logs = activityLogService.getUserLogs(user);
        return ResponseEntity.ok(logs);
    }

    @DeleteMapping("/{id}")
    @CacheEvict(value = "dashboard", key = "#user.username")
    public ResponseEntity<Void> deleteLog(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        activityLogService.deleteLog(user, id);
        return ResponseEntity.noContent().build();
    }
}
