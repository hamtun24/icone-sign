package com.example.unifiedapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ProgressCleanupService {
    
    private static final Logger logger = LoggerFactory.getLogger(ProgressCleanupService.class);
    
    @Autowired
    private ProgressTrackingService progressTrackingService;
    
    /**
     * Clean up old progress sessions every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void cleanupOldSessions() {
        try {
            int beforeCount = progressTrackingService.getAllSessions().size();
            progressTrackingService.cleanupOldSessions();
            int afterCount = progressTrackingService.getAllSessions().size();
            
            if (beforeCount > afterCount) {
                logger.info("Cleaned up {} old progress sessions ({} -> {})", 
                    beforeCount - afterCount, beforeCount, afterCount);
            }
        } catch (Exception e) {
            logger.error("Failed to cleanup old progress sessions: {}", e.getMessage(), e);
        }
    }
}
