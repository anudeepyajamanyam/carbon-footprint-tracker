package com.carbon.tracker.repository;

import com.carbon.tracker.model.Goal;
import com.carbon.tracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findByUser(User user);
    Optional<Goal> findByUserAndMonthAndYear(User user, Integer month, Integer year);
    void deleteByUser(User user);
}
