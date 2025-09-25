package com.example.unifiedapi.repository;

import com.example.unifiedapi.entity.WorkflowSession;
import com.example.unifiedapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowSessionRepository extends JpaRepository<WorkflowSession, Long> {
    
    Optional<WorkflowSession> findBySessionId(String sessionId);
    
    List<WorkflowSession> findByUser(User user);
    
    List<WorkflowSession> findByUserOrderByCreatedAtDesc(User user);
    
    List<WorkflowSession> findByStatus(WorkflowSession.Status status);
    
    @Query("SELECT ws FROM WorkflowSession ws WHERE ws.user = :user AND ws.status = :status ORDER BY ws.createdAt DESC")
    List<WorkflowSession> findByUserAndStatus(@Param("user") User user, @Param("status") WorkflowSession.Status status);
    
    @Query("SELECT ws FROM WorkflowSession ws WHERE ws.updatedAt < :cutoffTime AND ws.status IN ('PROCESSING', 'INITIALIZING')")
    List<WorkflowSession> findStaleProcessingSessions(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    @Query("SELECT COUNT(ws) FROM WorkflowSession ws WHERE ws.user = :user AND ws.createdAt >= :startDate")
    Long countUserSessionsSince(@Param("user") User user, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT ws FROM WorkflowSession ws WHERE ws.createdAt >= :startDate AND ws.createdAt <= :endDate ORDER BY ws.createdAt DESC")
    List<WorkflowSession> findSessionsBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
