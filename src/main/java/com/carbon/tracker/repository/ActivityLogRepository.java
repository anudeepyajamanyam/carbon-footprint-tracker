package com.carbon.tracker.repository;

import com.carbon.tracker.model.ActivityLog;
import com.carbon.tracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    
    List<ActivityLog> findByUserOrderByLogDateDesc(User user);
    
    List<ActivityLog> findByUserAndLogDateBetween(User user, LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT COALESCE(SUM(a.emission), 0.0) FROM ActivityLog a WHERE a.user = :user AND a.logDate BETWEEN :startDate AND :endDate")
    Double sumEmissionsByUserAndDateRange(@Param("user") User user, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT a.category, COALESCE(SUM(a.emission), 0.0) FROM ActivityLog a WHERE a.user = :user AND a.logDate BETWEEN :startDate AND :endDate GROUP BY a.category")
    List<Object[]> sumEmissionsByCategoryAndDateRange(@Param("user") User user, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    void deleteByUser(User user);
}
