package com.carbon.tracker.controller;

import com.carbon.tracker.model.Action;
import com.carbon.tracker.model.User;
import com.carbon.tracker.model.UserAction;
import com.carbon.tracker.service.ActionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/actions")
public class ActionController {

    @Autowired
    private ActionService actionService;

    @GetMapping
    public ResponseEntity<List<Action>> getAllActions() {
        return ResponseEntity.ok(actionService.getAllActions());
    }

    @GetMapping("/user")
    public ResponseEntity<List<UserAction>> getUserActions(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(actionService.getUserActions(user));
    }

    @PostMapping("/opt-in/{actionId}")
    @CacheEvict(value = "dashboard", key = "#user.username")
    public ResponseEntity<UserAction> optIn(
            @AuthenticationPrincipal User user,
            @PathVariable Long actionId,
            @RequestParam(required = false) String challengedBy) {
        UserAction ua = actionService.optInToAction(user, actionId, challengedBy);
        return ResponseEntity.ok(ua);
    }

    @PostMapping("/complete/{userActionId}")
    @CacheEvict(value = "dashboard", key = "#user.username")
    public ResponseEntity<UserAction> complete(
            @AuthenticationPrincipal User user,
            @PathVariable Long userActionId) {
        UserAction ua = actionService.completeAction(user, userActionId);
        return ResponseEntity.ok(ua);
    }

    @DeleteMapping("/abandon/{userActionId}")
    @CacheEvict(value = "dashboard", key = "#user.username")
    public ResponseEntity<Void> abandon(
            @AuthenticationPrincipal User user,
            @PathVariable Long userActionId) {
        actionService.abandonAction(user, userActionId);
        return ResponseEntity.noContent().build();
    }
}
