package com.example.unifiedapi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "workflow_sessions")
public class WorkflowSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "session_id", unique = true, nullable = false)
    private String sessionId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status = Status.INITIALIZING;
    
    @Column(name = "current_stage")
    @Enumerated(EnumType.STRING)
    private Stage currentStage = Stage.SIGN;
    
    @Column(name = "overall_progress")
    private Integer overallProgress = 0;
    
    @Column(name = "total_files")
    private Integer totalFiles = 0;
    
    @Column(name = "successful_files")
    private Integer successfulFiles = 0;
    
    @Column(name = "failed_files")
    private Integer failedFiles = 0;
    
    @Column(name = "message", length = 1000)
    private String message;
    
    @Column(name = "error_message", length = 2000)
    private String errorMessage;
    
    @Column(name = "zip_download_url")
    private String zipDownloadUrl;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @OneToMany(mappedBy = "workflowSession", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WorkflowFile> files;
    
    public enum Status {
        INITIALIZING, PROCESSING, COMPLETED, FAILED
    }
    
    public enum Stage {
        SIGN, SAVE, VALIDATE, TRANSFORM, PACKAGE
    }
    
    // Constructors
    public WorkflowSession() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public WorkflowSession(String sessionId, User user) {
        this();
        this.sessionId = sessionId;
        this.user = user;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public Status getStatus() { return status; }
    public void setStatus(Status status) { 
        this.status = status;
        this.updatedAt = LocalDateTime.now();
        if (status == Status.COMPLETED || status == Status.FAILED) {
            this.completedAt = LocalDateTime.now();
        }
    }
    
    public Stage getCurrentStage() { return currentStage; }
    public void setCurrentStage(Stage currentStage) { 
        this.currentStage = currentStage;
        this.updatedAt = LocalDateTime.now();
    }
    
    public Integer getOverallProgress() { return overallProgress; }
    public void setOverallProgress(Integer overallProgress) { 
        this.overallProgress = overallProgress;
        this.updatedAt = LocalDateTime.now();
    }
    
    public Integer getTotalFiles() { return totalFiles; }
    public void setTotalFiles(Integer totalFiles) { this.totalFiles = totalFiles; }
    
    public Integer getSuccessfulFiles() { return successfulFiles; }
    public void setSuccessfulFiles(Integer successfulFiles) { this.successfulFiles = successfulFiles; }
    
    public Integer getFailedFiles() { return failedFiles; }
    public void setFailedFiles(Integer failedFiles) { this.failedFiles = failedFiles; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { 
        this.message = message;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public String getZipDownloadUrl() { return zipDownloadUrl; }
    public void setZipDownloadUrl(String zipDownloadUrl) { this.zipDownloadUrl = zipDownloadUrl; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    
    public List<WorkflowFile> getFiles() { return files; }
    public void setFiles(List<WorkflowFile> files) { this.files = files; }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
