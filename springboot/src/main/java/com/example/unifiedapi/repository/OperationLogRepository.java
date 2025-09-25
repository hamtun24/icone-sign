package com.example.unifiedapi.repository;

import com.example.unifiedapi.entity.OperationLog;
import com.example.unifiedapi.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {

    List<OperationLog> findByOperationTypeOrderByCreatedAtDesc(String operationType);

    List<OperationLog> findByOperationStatusOrderByCreatedAtDesc(String operationStatus);

    List<OperationLog> findByUserOrderByCreatedAtDesc(User user);

    Page<OperationLog> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    List<OperationLog> findBySessionId(String sessionId);

    @Query("SELECT ol FROM OperationLog ol WHERE ol.createdAt >= :startDate ORDER BY ol.createdAt DESC")
    List<OperationLog> findRecentOperations(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(ol) FROM OperationLog ol WHERE ol.operationType = :operationType")
    long countByOperationType(@Param("operationType") String operationType);

    @Query("SELECT COUNT(ol) FROM OperationLog ol WHERE ol.operationStatus = :status")
    long countByStatus(@Param("status") String status);

    @Query("SELECT ol FROM OperationLog ol ORDER BY ol.createdAt DESC")
    List<OperationLog> findAllOrderByCreatedAtDesc();
}
