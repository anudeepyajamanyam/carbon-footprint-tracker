package com.carbon.tracker.dto;

import java.time.LocalDateTime;

public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private LocalDateTime createdAt;

    public UserResponse(Long id, String username, String email, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
