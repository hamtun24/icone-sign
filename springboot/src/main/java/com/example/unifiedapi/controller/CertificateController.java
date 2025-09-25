package com.example.unifiedapi.controller;

import com.example.unifiedapi.service.CertificatePathService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/certificates")
@CrossOrigin(origins = "*")
public class CertificateController {

    private static final Logger logger = LoggerFactory.getLogger(CertificateController.class);

    private final CertificatePathService certificatePathService;

    @Autowired
    public CertificateController(CertificatePathService certificatePathService) {
        this.certificatePathService = certificatePathService;
    }

    /**
     * Get certificate configuration information
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getCertificateInfo() {
        try {
            Map<String, Object> info = new HashMap<>();
            
            // Default certificate path
            info.put("defaultPath", certificatePathService.getDefaultCertificatePath());
            
            // Available certificates in resources
            String[] availableCerts = certificatePathService.getAvailableCertificates();
            info.put("availableCertificates", availableCerts);
            
            // Supported formats
            info.put("supportedFormats", new String[]{".crt", ".pem", ".cer"});
            
            // Instructions
            info.put("instructions", new String[]{
                "Place your certificate file in src/main/resources/certificates/",
                "Default certificate should be named 'icone.crt'",
                "You can override the path in the frontend if needed",
                "Supported formats: .crt, .pem, .cer"
            });
            
            logger.info("Certificate info requested - default path: {}, available certificates: {}", 
                       certificatePathService.getDefaultCertificatePath(), availableCerts.length);
            
            return ResponseEntity.ok(info);
            
        } catch (Exception e) {
            logger.error("Failed to get certificate info: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get certificate information");
            error.put("details", e.getMessage());
            
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Validate a certificate path
     */
    @PostMapping("/validate-path")
    public ResponseEntity<Map<String, Object>> validateCertificatePath(@RequestBody Map<String, String> request) {
        try {
            String path = request.get("path");
            
            Map<String, Object> response = new HashMap<>();
            
            if (path == null || path.trim().isEmpty()) {
                response.put("valid", false);
                response.put("message", "Certificate path is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            boolean isValidFormat = certificatePathService.isValidCertificatePath(path);
            
            if (!isValidFormat) {
                response.put("valid", false);
                response.put("message", "Invalid certificate path format. Use .crt, .pem, or .cer files");
                return ResponseEntity.ok(response);
            }
            
            try {
                // Try to resolve the path
                String resolvedPath = certificatePathService.resolveCertificatePath(path);
                response.put("valid", true);
                response.put("resolvedPath", resolvedPath);
                response.put("message", "Certificate path is valid");
                
                logger.info("Certificate path validation successful: {} -> {}", path, resolvedPath);
                
            } catch (IllegalArgumentException e) {
                response.put("valid", false);
                response.put("message", e.getMessage());
                
                logger.warn("Certificate path validation failed: {}", e.getMessage());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to validate certificate path: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("valid", false);
            error.put("error", "Validation failed");
            error.put("details", e.getMessage());
            
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Get recommended certificate paths
     */
    @GetMapping("/recommended-paths")
    public ResponseEntity<Map<String, Object>> getRecommendedPaths() {
        try {
            Map<String, Object> response = new HashMap<>();
            
            // Recommended paths in order of preference
            String[] recommendedPaths = {
                "classpath:certificates/icone.crt",
                "./certificates/icone.crt",
                "C:/certificates/icone.crt",
                "/opt/certificates/icone.crt"
            };
            
            response.put("recommendedPaths", recommendedPaths);
            response.put("defaultPath", certificatePathService.getDefaultCertificatePath());
            
            // Instructions for each path type
            Map<String, String> pathInstructions = new HashMap<>();
            pathInstructions.put("classpath:", "Place in src/main/resources/certificates/ (recommended)");
            pathInstructions.put("./", "Relative to application working directory");
            pathInstructions.put("C:/", "Absolute Windows path");
            pathInstructions.put("/opt/", "Absolute Linux/Unix path");
            
            response.put("pathInstructions", pathInstructions);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get recommended paths: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get recommended paths");
            error.put("details", e.getMessage());
            
            return ResponseEntity.status(500).body(error);
        }
    }
}
