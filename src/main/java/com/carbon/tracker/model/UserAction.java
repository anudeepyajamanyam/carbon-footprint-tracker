package com.carbon.tracker.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(
    name = "user_actions",
    indexes = {
        @Index(name = "idx_user_actions_user_status", columnList = "user_id, status")
    }
)
public class UserAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "action_id", nullable = false)
    private Action action;

    @Column(nullable = false)
    private String status; // ACTIVE, COMPLETED

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "completion_date")
    private LocalDate completionDate;

    @Column(name = "streak_days", nullable = false)
    private Integer streakDays = 0;

    @Column(name = "total_completions", nullable = false)
    private Integer totalCompletions = 0;

    @Column(name = "challenged_by")
    private String challengedBy;

    public UserAction() {}

    public UserAction(User user, Action action, String status, LocalDate startDate) {
        this.user = user;
        this.action = action;
        this.status = status;
        this.startDate = startDate;
        this.streakDays = 0;
        this.totalCompletions = 0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Action getAction() { return action; }
    public void setAction(Action action) { this.action = action; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getCompletionDate() { return completionDate; }
    public void setCompletionDate(LocalDate completionDate) { this.completionDate = completionDate; }

    public Integer getStreakDays() { return streakDays; }
    public void setStreakDays(Integer streakDays) { this.streakDays = streakDays; }

    public Integer getTotalCompletions() { return totalCompletions; }
    public void setTotalCompletions(Integer totalCompletions) { this.totalCompletions = totalCompletions; }

    public String getChallengedBy() { return challengedBy; }
    public void setChallengedBy(String challengedBy) { this.challengedBy = challengedBy; }
}
