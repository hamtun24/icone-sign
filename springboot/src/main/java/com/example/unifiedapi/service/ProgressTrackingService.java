package com.example.unifiedapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import org.springframework.context.event.EventListener;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Map;
import java.util.List;
import java.util.UUID;

@Service
public class ProgressTrackingService {
    
    private static final Logger logger = LoggerFactory.getLogger(ProgressTrackingService.class);
    
    // Store progress sessions
    private final Map<String, WorkflowProgress> progressSessions = new ConcurrentHashMap<>();
    
    // Store progress listeners (for WebSocket connections)
    private final Map<String, List<ProgressListener>> progressListeners = new ConcurrentHashMap<>();
    
    public interface ProgressListener {
        void onProgressUpdate(WorkflowProgress progress);
    }
    
    public static class WorkflowProgress {
        private String sessionId;
        private String status; // "INITIALIZING", "PROCESSING", "COMPLETED", "FAILED"
        private String currentStage; // "SIGN", "SAVE", "VALIDATE", "TRANSFORM", "PACKAGE"
        private int overallProgress; // 0-100
        private LocalDateTime lastUpdated;
        private List<FileProgress> files;
        private String message;
        private boolean success;
        private String errorMessage;
        private String zipDownloadUrl;
        
        public WorkflowProgress(String sessionId) {
            this.sessionId = sessionId;
            this.status = "INITIALIZING";
            this.currentStage = "SIGN";
            this.overallProgress = 0;
            this.lastUpdated = LocalDateTime.now();
            this.files = new CopyOnWriteArrayList<>();
            this.message = "Initialisation du workflow...";
            this.success = false;
        }
        
        // Getters and setters
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { 
            this.status = status; 
            this.lastUpdated = LocalDateTime.now();
        }
        
        public String getCurrentStage() { return currentStage; }
        public void setCurrentStage(String currentStage) { 
            this.currentStage = currentStage;
            this.lastUpdated = LocalDateTime.now();
        }
        
        public int getOverallProgress() { return overallProgress; }
        public void setOverallProgress(int overallProgress) { 
            this.overallProgress = Math.max(0, Math.min(100, overallProgress));
            this.lastUpdated = LocalDateTime.now();
        }
        
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
        
        public List<FileProgress> getFiles() { return files; }
        public void setFiles(List<FileProgress> files) { this.files = files; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { 
            this.message = message;
            this.lastUpdated = LocalDateTime.now();
        }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public String getZipDownloadUrl() { return zipDownloadUrl; }
        public void setZipDownloadUrl(String zipDownloadUrl) { this.zipDownloadUrl = zipDownloadUrl; }
    }
    
    public static class FileProgress {
        private String filename;
        private String status; // "PENDING", "PROCESSING", "COMPLETED", "FAILED"
        private String stage; // "SIGN", "SAVE", "VALIDATE", "TRANSFORM", "PACKAGE"
        private int progress; // 0-100
        private String errorMessage;
        private String ttnInvoiceId;
        private long fileSize;
        
        public FileProgress(String filename, long fileSize) {
            this.filename = filename;
            this.fileSize = fileSize;
            this.status = "PENDING";
            this.stage = "SIGN";
            this.progress = 0;
        }
        
        // Getters and setters
        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getStage() { return stage; }
        public void setStage(String stage) { this.stage = stage; }
        
        public int getProgress() { return progress; }
        public void setProgress(int progress) { this.progress = Math.max(0, Math.min(100, progress)); }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public String getTtnInvoiceId() { return ttnInvoiceId; }
        public void setTtnInvoiceId(String ttnInvoiceId) { this.ttnInvoiceId = ttnInvoiceId; }
        
        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    }
    
    /**
     * Create a new progress session
     */
    public String createProgressSession(List<String> filenames, List<Long> fileSizes) {
        String sessionId = UUID.randomUUID().toString();
        WorkflowProgress progress = new WorkflowProgress(sessionId);
        
        // Initialize file progress
        for (int i = 0; i < filenames.size(); i++) {
            String filename = filenames.get(i);
            long fileSize = i < fileSizes.size() ? fileSizes.get(i) : 0L;
            progress.getFiles().add(new FileProgress(filename, fileSize));
        }
        
        progressSessions.put(sessionId, progress);
        logger.info("Created progress session: {} with {} files", sessionId, filenames.size());
        
        return sessionId;
    }
    
    /**
     * Get progress for a session
     */
    public WorkflowProgress getProgress(String sessionId) {
        return progressSessions.get(sessionId);
    }
    
    /**
     * Update overall workflow progress
     */
    public void updateWorkflowProgress(String sessionId, String status, String stage, int progress, String message) {
        WorkflowProgress workflowProgress = progressSessions.get(sessionId);
        if (workflowProgress != null) {
            workflowProgress.setStatus(status);
            workflowProgress.setCurrentStage(stage);
            workflowProgress.setOverallProgress(progress);
            workflowProgress.setMessage(message);
            
            logger.debug("Updated workflow progress for session {}: {}% - {}", sessionId, progress, message);
            notifyListeners(sessionId, workflowProgress);
        }
    }
    
    /**
     * Update file-specific progress
     */
    public void updateFileProgress(String sessionId, String filename, String status, String stage, int progress, String errorMessage) {
        WorkflowProgress workflowProgress = progressSessions.get(sessionId);
        if (workflowProgress != null) {
            FileProgress fileProgress = workflowProgress.getFiles().stream()
                .filter(fp -> fp.getFilename().equals(filename))
                .findFirst()
                .orElse(null);
                
            if (fileProgress != null) {
                fileProgress.setStatus(status);
                fileProgress.setStage(stage);
                fileProgress.setProgress(progress);
                if (errorMessage != null) {
                    fileProgress.setErrorMessage(errorMessage);
                }
                
                // Update overall progress based on file progress
                updateOverallProgress(sessionId);
                
                logger.debug("Updated file progress for {}: {}% - {} ({})", filename, progress, stage, status);
                notifyListeners(sessionId, workflowProgress);
            }
        }
    }
    
    /**
     * Set TTN Invoice ID for a file
     */
    public void setTtnInvoiceId(String sessionId, String filename, String ttnInvoiceId) {
        WorkflowProgress workflowProgress = progressSessions.get(sessionId);
        if (workflowProgress != null) {
            FileProgress fileProgress = workflowProgress.getFiles().stream()
                .filter(fp -> fp.getFilename().equals(filename))
                .findFirst()
                .orElse(null);
                
            if (fileProgress != null) {
                fileProgress.setTtnInvoiceId(ttnInvoiceId);
                logger.info("🎯 Set TTN Invoice ID for {}: {}", filename, ttnInvoiceId);
                notifyListeners(sessionId, workflowProgress);
            } else {
                logger.warn("⚠️ Could not find file progress for {} to set TTN ID: {}", filename, ttnInvoiceId);
            }
        }
    }
    
    /**
     * Complete workflow processing
     */
    public void completeWorkflow(String sessionId, boolean success, String message) {
        WorkflowProgress workflowProgress = progressSessions.get(sessionId);
        if (workflowProgress != null) {
            workflowProgress.setStatus(success ? "COMPLETED" : "FAILED");
            workflowProgress.setSuccess(success);
            workflowProgress.setMessage(message);
            workflowProgress.setOverallProgress(100);

            if (!success) {
                workflowProgress.setErrorMessage(message);
            }

            logger.info("Completed workflow for session {}: {} - {}", sessionId, success ? "SUCCESS" : "FAILED", message);
            notifyListeners(sessionId, workflowProgress);
        }
    }

    /**
     * Set ZIP download URL for a workflow session
     */
    public void setZipDownloadUrl(String sessionId, String zipDownloadUrl) {
        WorkflowProgress workflowProgress = progressSessions.get(sessionId);
        if (workflowProgress != null) {
            workflowProgress.setZipDownloadUrl(zipDownloadUrl);
            logger.debug("Set ZIP download URL for session {}: {}", sessionId, zipDownloadUrl);
            notifyListeners(sessionId, workflowProgress);
        }
    }
    
    /**
     * Calculate and update overall progress based on file progress
     */
    private void updateOverallProgress(String sessionId) {
        WorkflowProgress workflowProgress = progressSessions.get(sessionId);
        if (workflowProgress != null && !workflowProgress.getFiles().isEmpty()) {
            int totalProgress = workflowProgress.getFiles().stream()
                .mapToInt(FileProgress::getProgress)
                .sum();
            int averageProgress = totalProgress / workflowProgress.getFiles().size();
            workflowProgress.setOverallProgress(averageProgress);
        }
    }
    
    /**
     * Add progress listener for real-time updates
     */
    public void addProgressListener(String sessionId, ProgressListener listener) {
        progressListeners.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>()).add(listener);
        logger.debug("Added progress listener for session: {}", sessionId);
    }
    
    /**
     * Remove progress listener
     */
    public void removeProgressListener(String sessionId, ProgressListener listener) {
        List<ProgressListener> listeners = progressListeners.get(sessionId);
        if (listeners != null) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                progressListeners.remove(sessionId);
            }
        }
        logger.debug("Removed progress listener for session: {}", sessionId);
    }
    
    /**
     * Notify all listeners of progress update
     */
    @Async
    private void notifyListeners(String sessionId, WorkflowProgress progress) {
        List<ProgressListener> listeners = progressListeners.get(sessionId);
        if (listeners != null) {
            for (ProgressListener listener : listeners) {
                try {
                    listener.onProgressUpdate(progress);
                } catch (Exception e) {
                    logger.warn("Failed to notify progress listener for session {}: {}", sessionId, e.getMessage());
                }
            }
        }
    }
    
    /**
     * Clean up old progress sessions
     */
    public void cleanupOldSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
        progressSessions.entrySet().removeIf(entry -> 
            entry.getValue().getLastUpdated().isBefore(cutoff));
        logger.debug("Cleaned up old progress sessions");
    }
    
    /**
     * Get all active sessions (for debugging)
     */
    public Map<String, WorkflowProgress> getAllSessions() {
        return new ConcurrentHashMap<>(progressSessions);
    }
}
