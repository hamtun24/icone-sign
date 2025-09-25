package com.example.unifiedapi.controller;

import com.example.unifiedapi.dto.TtnSaveEfactRequest;
import com.example.unifiedapi.dto.TtnConsultEfactRequest;
import com.example.unifiedapi.dto.TtnConsultCriteriaRequest;
import com.example.unifiedapi.service.UserCredentialsService;
import com.example.unifiedapi.dto.TtnSaveEfactResponse;
import com.example.unifiedapi.dto.TtnConsultEfactResponse;
import com.example.unifiedapi.service.TtnOperationsService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ttn")
public class TtnOperationsController {
    
    private static final Logger logger = LoggerFactory.getLogger(TtnOperationsController.class);
    
    private final TtnOperationsService ttnOperationsService;
    private final UserCredentialsService userCredentialsService;

    @Autowired
    public TtnOperationsController(TtnOperationsService ttnOperationsService, UserCredentialsService userCredentialsService) {
        this.ttnOperationsService = ttnOperationsService;
        this.userCredentialsService = userCredentialsService;
    }
    
    @PostMapping(value = "/save-efact", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> saveEfact(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam("matriculeFiscal") String matriculeFiscal,
            @RequestParam("files") MultipartFile[] files) {
        
        logger.info("Received saveEfact request for user: {}, matricule: {}, files: {}", 
                   username, matriculeFiscal, files.length);
        
        try {
            // Validate input
            if (username == null || username.trim().isEmpty() ||
                password == null || password.trim().isEmpty() ||
                matriculeFiscal == null || matriculeFiscal.trim().isEmpty()) {
                
                Map<String, String> error = new HashMap<>();
                error.put("error", "Missing required fields: username, password, matriculeFiscal");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (files == null || files.length == 0) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "No XML files provided");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Validate file types
            for (MultipartFile file : files) {
                if (file.isEmpty() || !file.getOriginalFilename().toLowerCase().endsWith(".xml")) {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Only XML files are allowed");
                    return ResponseEntity.badRequest().body(error);
                }
            }
            
            // Create request object
            TtnSaveEfactRequest request = new TtnSaveEfactRequest(username, password, matriculeFiscal);
            
            // Process the request
            TtnSaveEfactResponse response = ttnOperationsService.saveEfact(request, Arrays.asList(files));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error in saveEfact: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to save e-invoices");
            error.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Consult e-invoices using stored user credentials
     */
    @PostMapping("/consult")
    public ResponseEntity<?> consultWithStoredCredentials(@RequestBody TtnConsultCriteriaRequest request) {
        logger.info("Received consult request using stored credentials");

        try {
            // Get credentials from authenticated user
            String username = userCredentialsService.getTtnUsername();
            String password = userCredentialsService.getTtnPassword();
            String matriculeFiscal = userCredentialsService.getTtnMatriculeFiscal();

            // Create full request with stored credentials
            TtnConsultEfactRequest fullRequest = new TtnConsultEfactRequest(username, password, matriculeFiscal, request.getCriteria());

            TtnConsultEfactResponse response = ttnOperationsService.consultEfact(fullRequest);

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

        } catch (Exception e) {
            logger.error("Error in consult with stored credentials: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to consult e-invoices");
            error.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/consult-efact")
    public ResponseEntity<?> consultEfact(@Valid @RequestBody TtnConsultEfactRequest request) {
        logger.info("Received consultEfact request for user: {}, matricule: {}", 
                   request.getUsername(), request.getMatriculeFiscal());
        
        try {
            TtnConsultEfactResponse response = ttnOperationsService.consultEfact(request);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error in consultEfact: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to consult e-invoices");
            error.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/efact/{id}")
    public ResponseEntity<?> getEfactById(
            @PathVariable String id,
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam("matriculeFiscal") String matriculeFiscal) {
        
        logger.info("Received getEfactById request for ID: {}, user: {}, matricule: {}", 
                   id, username, matriculeFiscal);
        
        try {
            // Validate input
            if (username == null || username.trim().isEmpty() ||
                password == null || password.trim().isEmpty() ||
                matriculeFiscal == null || matriculeFiscal.trim().isEmpty()) {
                
                Map<String, String> error = new HashMap<>();
                error.put("error", "Missing required query parameters: username, password, matriculeFiscal");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Create criteria for specific ID lookup
            Map<String, Object> criteria = new HashMap<>();
            criteria.put("generatedRef", id);
            
            TtnConsultEfactRequest request = new TtnConsultEfactRequest(username, password, matriculeFiscal, criteria);
            TtnConsultEfactResponse response = ttnOperationsService.consultEfact(request);
            
            if (response.isSuccess()) {
                if (response.getInvoices() == null || response.getInvoices().isEmpty()) {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "E-invoice not found");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
                }
                
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("invoice", response.getInvoices().get(0));
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error in getEfactById: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to get e-invoice details");
            error.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        logger.debug("Health check requested");
        
        Map<String, Object> health = new HashMap<>();
        health.put("status", "OK");
        health.put("service", "TTN Operations API");
        health.put("timestamp", java.time.Instant.now().toString());
        
        return ResponseEntity.ok(health);
    }
}
