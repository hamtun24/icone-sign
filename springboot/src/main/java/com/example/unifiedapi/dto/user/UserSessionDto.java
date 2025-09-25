package com.example.unifiedapi.dto.user;

import java.time.LocalDateTime;

public class UserSessionDto {
    private String sessionId;
    private String status;
    private String currentStage;
    private int overallProgress;
    private int totalFiles;
    private int successfulFiles;
    private int failedFiles;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    
    // Constructors
    public UserSessionDto() {}
    
    // Getters and Setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getCurrentStage() { return currentStage; }
    public void setCurrentStage(String currentStage) { this.currentStage = currentStage; }
    
    public int getOverallProgress() { return overallProgress; }
    public void setOverallProgress(int overallProgress) { this.overallProgress = overallProgress; }
    
    public int getTotalFiles() { return totalFiles; }
    public void setTotalFiles(int totalFiles) { this.totalFiles = totalFiles; }
    
    public int getSuccessfulFiles() { return successfulFiles; }
    public void setSuccessfulFiles(int successfulFiles) { this.successfulFiles = successfulFiles; }
    
    public int getFailedFiles() { return failedFiles; }
    public void setFailedFiles(int failedFiles) { this.failedFiles = failedFiles; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
