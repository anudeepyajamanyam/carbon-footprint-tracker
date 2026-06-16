package com.carbon.tracker.controller;

import com.carbon.tracker.dto.LeaderboardEntry;
import com.carbon.tracker.service.SocialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/social")
public class SocialController {

    private final SocialService socialService;

    public SocialController(SocialService socialService) {
        this.socialService = socialService;
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardEntry>> getLeaderboard() {
        List<LeaderboardEntry> leaderboard = socialService.getLeaderboard();
        return ResponseEntity.ok(leaderboard);
    }
}
