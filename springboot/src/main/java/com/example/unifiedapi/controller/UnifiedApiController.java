package com.example.unifiedapi.controller;

import com.example.unifiedapi.service.OperationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/")
public class UnifiedApiController {

    private static final Logger logger = LoggerFactory.getLogger(UnifiedApiController.class);
    
    private final OperationLogService operationLogService;

    @Autowired
    public UnifiedApiController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> globalHealthCheck() {
        logger.debug("Global health check requested");
        
        Map<String, Object> health = new HashMap<>();
        health.put("status", "OK");
        health.put("service", "Unified Operations API");
        health.put("version", "1.0.0");
        health.put("timestamp", java.time.Instant.now().toString());
        health.put("description", "Unified API for TTN E-Facturation and ANCE SEAL XML Signature Operations");
        
        // Add service endpoints
        Map<String, Object> endpoints = new HashMap<>();
        endpoints.put("ttn", Map.of(
            "saveEfact", "POST /api/v1/ttn/save-efact",
            "consultEfact", "POST /api/v1/ttn/consult-efact",
            "getEfact", "GET /api/v1/ttn/efact/{id}",
            "health", "GET /api/v1/ttn/health"
        ));
        endpoints.put("anceSeal", Map.of(
            "sign", "POST /api/v1/ance-seal/sign",
            "validate", "POST /api/v1/ance-seal/validate",
            "batchSign", "POST /api/v1/ance-seal/batch/sign",
            "health", "GET /api/v1/ance-seal/health"
        ));
        health.put("availableEndpoints", endpoints);
        
        return ResponseEntity.ok(health);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getOperationStats() {
        logger.debug("Operation statistics requested");
        
        Map<String, Object> stats = new HashMap<>();
        
        // TTN Operations Stats
        long ttnSaveCount = operationLogService.countByOperationType("TTN_SAVE");
        long ttnConsultCount = operationLogService.countByOperationType("TTN_CONSULT");
        
        // ANCE SEAL Operations Stats
        long anceSignCount = operationLogService.countByOperationType("ANCE_SIGN");
        long anceValidateCount = operationLogService.countByOperationType("ANCE_VALIDATE");
        long anceBatchSignCount = operationLogService.countByOperationType("ANCE_BATCH_SIGN");
        
        // Overall Stats
        long totalOperations = operationLogService.countAll();
        long successfulOperations = operationLogService.countByStatus("SUCCESS");
        long failedOperations = operationLogService.countByStatus("FAILURE");
        double successRate = totalOperations > 0 ? (100.0 * successfulOperations / totalOperations) : 0.0;
        
        Map<String, Object> ttnStats = new HashMap<>();
        ttnStats.put("saveEfact", ttnSaveCount);
        ttnStats.put("consultEfact", ttnConsultCount);
        ttnStats.put("total", ttnSaveCount + ttnConsultCount);
        
        Map<String, Object> anceSealStats = new HashMap<>();
        anceSealStats.put("sign", anceSignCount);
        anceSealStats.put("validate", anceValidateCount);
        anceSealStats.put("batchSign", anceBatchSignCount);
        anceSealStats.put("total", anceSignCount + anceValidateCount + anceBatchSignCount);
        
        Map<String, Object> overallStats = new HashMap<>();
        overallStats.put("totalOperations", totalOperations);
        overallStats.put("successfulOperations", successfulOperations);
        overallStats.put("failedOperations", failedOperations);
        overallStats.put("successRate", String.format("%.1f%%", successRate));
        
        stats.put("ttn", ttnStats);
        stats.put("anceSeal", anceSealStats);
        stats.put("overall", overallStats);
        stats.put("timestamp", java.time.Instant.now().toString());
        
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/recent-operations")
    public ResponseEntity<Map<String, Object>> getRecentOperations(
            @RequestParam(defaultValue = "10") int limit) {
        logger.debug("Recent operations requested with limit: {}", limit);
        
        Map<String, Object> response = new HashMap<>();
        response.put("operations", operationLogService.getRecentOperations(limit));
        response.put("timestamp", java.time.Instant.now().toString());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/operations/by-type/{operationType}")
    public ResponseEntity<Map<String, Object>> getOperationsByType(
            @PathVariable String operationType) {
        logger.debug("Operations by type requested: {}", operationType);
        
        Map<String, Object> response = new HashMap<>();
        response.put("operationType", operationType);
        response.put("operations", operationLogService.getOperationsByType(operationType));
        response.put("count", operationLogService.countByOperationType(operationType));
        response.put("timestamp", java.time.Instant.now().toString());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/operations/by-user/{username}")
    public ResponseEntity<Map<String, Object>> getOperationsByUser(
            @PathVariable String username) {
        logger.debug("Operations by user requested: {}", username);

        Map<String, Object> response = new HashMap<>();
        response.put("username", username);
        // Note: This endpoint would need User lookup to work with new entity structure
        // For now, return empty list to avoid compilation error
        response.put("operations", java.util.Collections.emptyList());
        response.put("timestamp", java.time.Instant.now().toString());
        response.put("message", "User operations endpoint needs User entity lookup");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/operations/since/{hours}")
    public ResponseEntity<Map<String, Object>> getOperationsSince(
            @PathVariable int hours) {
        logger.debug("Operations since {} hours requested", hours);
        
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        
        Map<String, Object> response = new HashMap<>();
        response.put("since", since.toString());
        response.put("operations", operationLogService.getRecentOperations(since));
        response.put("timestamp", java.time.Instant.now().toString());
        
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(Exception e) {
        logger.error("Unhandled exception in unified API: {}", e.getMessage(), e);
        
        Map<String, Object> error = new HashMap<>();
        error.put("error", "Internal server error");
        error.put("message", e.getMessage());
        error.put("timestamp", java.time.Instant.now().toString());
        
        return ResponseEntity.internalServerError().body(error);
    }
}
