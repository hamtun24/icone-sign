package com.example.unifiedapi.controller;

import com.example.unifiedapi.dto.TtnConsultHtmlRequest;
import com.example.unifiedapi.dto.TtnConsultHtmlResponse;
import com.example.unifiedapi.entity.User;
import com.example.unifiedapi.service.TtnConsultService;
import com.example.unifiedapi.service.UserCredentialsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/**
 * Controller for TTN consultation operations with HTML transformation
 */
@RestController
@RequestMapping("/ttn")
@CrossOrigin(origins = "*")
public class TtnConsultController {
    
    private static final Logger logger = LoggerFactory.getLogger(TtnConsultController.class);
    
    private final TtnConsultService ttnConsultService;
    private final UserCredentialsService userCredentialsService;
    
    @Autowired
    public TtnConsultController(TtnConsultService ttnConsultService,
                                UserCredentialsService userCredentialsService) {
        this.ttnConsultService = ttnConsultService;
        this.userCredentialsService = userCredentialsService;
    }
    
    /**
     * Consult TTN e-facturation system with HTML transformation
     * POST /api/v1/ttn/consulthtml
     */
    @PostMapping("/consulthtml")
    public ResponseEntity<TtnConsultHtmlResponse> consultHtml(@RequestBody TtnConsultHtmlRequest request) {
        logger.info("Received TTN consult HTML request with criteria: {}", request.getCriteria());
        try {
            // TODO: Replace with actual user lookup (e.g., from JWT/session)
            User currentUser = null;
            // Validate request
            if (request.getCriteria() == null) {
                logger.warn("Missing criteria in TTN consult HTML request");
                TtnConsultHtmlResponse errorResponse = new TtnConsultHtmlResponse();
                errorResponse.setSuccess(false);
                errorResponse.setError("Criteria is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            TtnConsultHtmlResponse response = ttnConsultService.consultWithHtml(request);
            if (response.isSuccess()) {
                logger.info("TTN consult HTML completed successfully for user: {} - {} invoices found", 
                           (currentUser != null ? currentUser.getUsername() : "unknown"), response.getCount());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("TTN consult HTML failed for user: {} - Error: {}", 
                           (currentUser != null ? currentUser.getUsername() : "unknown"), response.getError());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (Exception e) {
            logger.error("Unexpected error in TTN consult HTML: {}", e.getMessage(), e);
            TtnConsultHtmlResponse errorResponse = new TtnConsultHtmlResponse();
            errorResponse.setSuccess(false);
            errorResponse.setError("Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Health check endpoint for TTN consultation service
     * GET /api/v1/ttn/consult/health
     */
    @GetMapping("/consult/health")
    public ResponseEntity<Object> healthCheck() {
        try {
            return ResponseEntity.ok().body(new Object() {
                public final boolean healthy = true;
                public final String service = "TTN Consult Service";
                public final long timestamp = System.currentTimeMillis();
                public final String status = "operational";
            });
        } catch (Exception e) {
            logger.error("Health check failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new Object() {
                public final boolean healthy = false;
                public final String service = "TTN Consult Service";
                public final long timestamp = System.currentTimeMillis();
                public final String status = "error";
                public final String error = e.getMessage();
            });
        }
    }
    
    // ...existing code...
}