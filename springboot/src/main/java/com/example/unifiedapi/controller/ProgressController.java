package com.example.unifiedapi.controller;

import com.example.unifiedapi.service.ProgressTrackingService;
import com.example.unifiedapi.service.ProgressTrackingService.WorkflowProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/progress")
@CrossOrigin(origins = "*")
public class ProgressController {
    
    private static final Logger logger = LoggerFactory.getLogger(ProgressController.class);
    
    @Autowired
    private ProgressTrackingService progressTrackingService;
    
    /**
     * Get current progress for a workflow session
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<WorkflowProgress> getProgress(@PathVariable String sessionId) {
        logger.debug("Getting progress for session: {}", sessionId);
        
        WorkflowProgress progress = progressTrackingService.getProgress(sessionId);
        if (progress == null) {
            logger.warn("Progress session not found: {}", sessionId);
            return ResponseEntity.notFound().build();
        }

        // Debug log to see what TTN IDs are being sent
        if (progress.getFiles() != null) {
            progress.getFiles().forEach(file -> {
                if (file.getTtnInvoiceId() != null) {
                    logger.debug("📤 Sending TTN ID for {}: {}", file.getFilename(), file.getTtnInvoiceId());
                }
            });
        }

        return ResponseEntity.ok(progress);
    }
    
    /**
     * Get all active progress sessions (for debugging)
     */
    @GetMapping("/sessions")
    public ResponseEntity<Map<String, WorkflowProgress>> getAllSessions() {
        logger.debug("Getting all active progress sessions");
        return ResponseEntity.ok(progressTrackingService.getAllSessions());
    }
    
    /**
     * Health check endpoint for progress service
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = Map.of(
            "status", "UP",
            "service", "ProgressTrackingService",
            "activeSessions", progressTrackingService.getAllSessions().size(),
            "timestamp", java.time.LocalDateTime.now()
        );
        
        return ResponseEntity.ok(health);
    }
    
    /**
     * Manually trigger cleanup of old sessions (for admin use)
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupSessions() {
        logger.info("Manual cleanup of progress sessions triggered");
        
        int beforeCount = progressTrackingService.getAllSessions().size();
        progressTrackingService.cleanupOldSessions();
        int afterCount = progressTrackingService.getAllSessions().size();
        
        Map<String, Object> result = Map.of(
            "message", "Cleanup completed",
            "sessionsBefore", beforeCount,
            "sessionsAfter", afterCount,
            "sessionsRemoved", beforeCount - afterCount
        );
        
        return ResponseEntity.ok(result);
    }
}
