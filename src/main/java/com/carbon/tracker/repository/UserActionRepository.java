package com.carbon.tracker.repository;

import com.carbon.tracker.model.Action;
import com.carbon.tracker.model.User;
import com.carbon.tracker.model.UserAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserActionRepository extends JpaRepository<UserAction, Long> {
    List<UserAction> findByUser(User user);
    List<UserAction> findByUserAndStatus(User user, String status);
    Optional<UserAction> findByUserAndActionId(User user, Long actionId);
    Boolean existsByUserAndActionAndStatus(User user, Action action, String status);
    void deleteByUser(User user);
}
