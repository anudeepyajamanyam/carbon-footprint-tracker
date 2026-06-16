package com.carbon.tracker.controller;

import com.carbon.tracker.dto.DashboardResponse;
import com.carbon.tracker.model.User;
import com.carbon.tracker.service.InsightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/insights")
public class InsightController {

    private final InsightService insightService;

    public InsightController(InsightService insightService) {
        this.insightService = insightService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard(@AuthenticationPrincipal User user) {
        DashboardResponse response = insightService.getDashboardData(user);
        return ResponseEntity.ok(response);
    }
}
