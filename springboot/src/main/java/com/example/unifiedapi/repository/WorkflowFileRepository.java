package com.example.unifiedapi.repository;

import com.example.unifiedapi.entity.WorkflowFile;
import com.example.unifiedapi.entity.WorkflowSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowFileRepository extends JpaRepository<WorkflowFile, Long> {
    
    List<WorkflowFile> findByWorkflowSession(WorkflowSession workflowSession);
    
    List<WorkflowFile> findByWorkflowSessionOrderByCreatedAtAsc(WorkflowSession workflowSession);
    
    Optional<WorkflowFile> findByWorkflowSessionAndFilename(WorkflowSession workflowSession, String filename);
    
    List<WorkflowFile> findByStatus(WorkflowFile.Status status);
    
    List<WorkflowFile> findByStage(WorkflowFile.Stage stage);
    
    @Query("SELECT wf FROM WorkflowFile wf WHERE wf.workflowSession = :session AND wf.status = :status")
    List<WorkflowFile> findBySessionAndStatus(@Param("session") WorkflowSession session, @Param("status") WorkflowFile.Status status);
    
    @Query("SELECT wf FROM WorkflowFile wf WHERE wf.workflowSession = :session AND wf.stage = :stage")
    List<WorkflowFile> findBySessionAndStage(@Param("session") WorkflowSession session, @Param("stage") WorkflowFile.Stage stage);
    
    @Query("SELECT COUNT(wf) FROM WorkflowFile wf WHERE wf.workflowSession = :session AND wf.status = 'COMPLETED'")
    Long countCompletedFilesBySession(@Param("session") WorkflowSession session);
    
    @Query("SELECT COUNT(wf) FROM WorkflowFile wf WHERE wf.workflowSession = :session AND wf.status = 'FAILED'")
    Long countFailedFilesBySession(@Param("session") WorkflowSession session);
}
