package com.example.unifiedapi.repository;

import com.example.unifiedapi.model.InvoiceProcessingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InvoiceProcessingRecordRepository extends JpaRepository<InvoiceProcessingRecord, Long> {
    
    List<InvoiceProcessingRecord> findByUsernameOrderByStartTimeDesc(String username);
    
    List<InvoiceProcessingRecord> findByMatriculeFiscalOrderByStartTimeDesc(String matriculeFiscal);
    
    List<InvoiceProcessingRecord> findByStatusOrderByStartTimeDesc(String status);
    
    List<InvoiceProcessingRecord> findByCurrentStageOrderByStartTimeDesc(String currentStage);
    
    @Query("SELECT ipr FROM InvoiceProcessingRecord ipr WHERE ipr.startTime >= :startDate ORDER BY ipr.startTime DESC")
    List<InvoiceProcessingRecord> findRecentRecords(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT COUNT(ipr) FROM InvoiceProcessingRecord ipr WHERE ipr.status = :status")
    long countByStatus(@Param("status") String status);
    
    @Query("SELECT COUNT(ipr) FROM InvoiceProcessingRecord ipr WHERE ipr.username = :username")
    long countByUsername(@Param("username") String username);
    
    @Query("SELECT ipr FROM InvoiceProcessingRecord ipr ORDER BY ipr.startTime DESC")
    List<InvoiceProcessingRecord> findAllOrderByStartTimeDesc();
    
    @Query("SELECT ipr FROM InvoiceProcessingRecord ipr WHERE ipr.ttnInvoiceId = :invoiceId")
    List<InvoiceProcessingRecord> findByTtnInvoiceId(@Param("invoiceId") String invoiceId);
    
    @Query("SELECT COUNT(ipr) FROM InvoiceProcessingRecord ipr WHERE ipr.signCompleted = true")
    long countSignCompleted();
    
    @Query("SELECT COUNT(ipr) FROM InvoiceProcessingRecord ipr WHERE ipr.saveCompleted = true")
    long countSaveCompleted();
    
    @Query("SELECT COUNT(ipr) FROM InvoiceProcessingRecord ipr WHERE ipr.validateCompleted = true")
    long countValidateCompleted();
    
    @Query("SELECT COUNT(ipr) FROM InvoiceProcessingRecord ipr WHERE ipr.transformCompleted = true")
    long countTransformCompleted();
}
