package com.example.unifiedapi.controller;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.unifiedapi.dto.WorkflowRequest;
import com.example.unifiedapi.entity.User;
import com.example.unifiedapi.repository.UserRepository;
import com.example.unifiedapi.service.InvoiceWorkflowOrchestrator;
import com.example.unifiedapi.service.ZipService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/workflow")
public class WorkflowController {
    
    private static final Logger logger = LoggerFactory.getLogger(WorkflowController.class);
    
    private final InvoiceWorkflowOrchestrator workflowOrchestrator;
    private final ZipService zipService;
    private final UserRepository userRepository;

    @Autowired
    public WorkflowController(InvoiceWorkflowOrchestrator workflowOrchestrator, ZipService zipService, UserRepository userRepository) {
        this.workflowOrchestrator = workflowOrchestrator;
        this.zipService = zipService;
        this.userRepository = userRepository;
    }
    
    /**
     * Complete invoice processing workflow: Sign → Save → Validate → Transform → Zip
     * Credentials are retrieved from the authenticated user's profile
     * Returns immediately with sessionId for progress tracking
     */
    @PostMapping(value = "/process-invoices", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> processInvoices(
            @RequestParam("files") MultipartFile[] files,
            HttpServletRequest httpRequest) {

        // Get current user from authenticated context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        logger.info("Received workflow processing request for {} files from authenticated user: {}",
                   files.length, currentUsername);

        // Get the current user to pass to async processing (to avoid security context issues)
        User currentUser;
        if (authentication.getPrincipal() instanceof User) {
            currentUser = (User) authentication.getPrincipal();
        } else {
            // Fallback: lookup user by username
            currentUser = userRepository.findByUsernameOrEmail(currentUsername, currentUsername)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + currentUsername));
        }
        
        try {
            // Validate input
            if (files == null || files.length == 0) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "No XML files provided");
                return ResponseEntity.badRequest().body((Object) error);
            }
            
            // Validate file types and sizes
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Empty file detected: " + file.getOriginalFilename());
                    return ResponseEntity.badRequest().body((Object) error);
                }

                if (!file.getOriginalFilename().toLowerCase().endsWith(".xml")) {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Only XML files are allowed: " + file.getOriginalFilename());
                    return ResponseEntity.badRequest().body((Object) error);
                }

                if (file.getSize() > 16 * 1024 * 1024) { // 16MB limit
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "File too large (max 16MB): " + file.getOriginalFilename());
                    return ResponseEntity.badRequest().body((Object) error);
                }
            }
            
            // Get base URL for download links
            String baseUrl = getBaseUrl(httpRequest);
            
            // Start async processing and get session ID immediately
            String sessionId = workflowOrchestrator.startProcessingAsync(Arrays.asList(files), baseUrl, currentUser);

            // Return immediate response with session ID for progress tracking
            Map<String, Object> immediateResponse = new HashMap<>();
            immediateResponse.put("success", true);
            immediateResponse.put("message", "Traitement démarré avec succès. Utilisez l'ID de session pour suivre la progression.");
            immediateResponse.put("sessionId", sessionId);
            immediateResponse.put("totalFiles", files.length);
            immediateResponse.put("status", "PROCESSING");

            return ResponseEntity.ok().body((Object) immediateResponse);
                
        } catch (Exception e) {
            logger.error("Error in workflow processing: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to start workflow processing");
            error.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((Object) error);
        }
    }
    
    /**
     * Download processed files ZIP
     */
    @GetMapping("/download/{filename}")
    public ResponseEntity<Resource> downloadZip(@PathVariable String filename) {
        logger.info("Download request for ZIP file: {}", filename);
        
        try {
            // Validate filename (security check)
            if (!filename.matches("^[a-zA-Z0-9_\\-\\.]+\\.zip$")) {
                return ResponseEntity.badRequest().build();
            }
            
            if (!zipService.isZipFileAvailable(filename)) {
                return ResponseEntity.notFound().build();
            }
            
            Path zipPath = zipService.getZipFilePath(filename);
            Resource resource = new FileSystemResource(zipPath);
            
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            
            // Set headers for file download
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, "application/zip");
            
            logger.info("Serving ZIP file: {} (size: {} bytes)", filename, resource.contentLength());
            
            return ResponseEntity.ok()
                .headers(headers)
                .contentLength(resource.contentLength())
                .body(resource);
                
        } catch (Exception e) {
            logger.error("Error serving ZIP file {}: {}", filename, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get workflow processing status (for future implementation)
     */
    @GetMapping("/status/{requestId}")
    public ResponseEntity<Map<String, Object>> getProcessingStatus(@PathVariable String requestId) {
        // TODO: Implement status tracking for long-running workflows
        Map<String, Object> status = new HashMap<>();
        status.put("requestId", requestId);
        status.put("status", "NOT_IMPLEMENTED");
        status.put("message", "Status tracking not yet implemented");
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * Health check for workflow service
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        logger.debug("Workflow service health check requested");
        
        Map<String, Object> health = new HashMap<>();
        health.put("status", "OK");
        health.put("service", "Invoice Processing Workflow");
        health.put("timestamp", java.time.Instant.now().toString());
        health.put("workflow", "Sign → Save → Validate → Transform → Zip");
        
        return ResponseEntity.ok(health);
    }
    
    /**
     * Get supported file formats and limits
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getWorkflowInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("supportedFormats", Arrays.asList("XML"));
        info.put("maxFileSize", "16MB");
        info.put("maxFiles", "No limit");
        info.put("workflow", Arrays.asList(
            "1. Digital signature with ANCE SEAL",
            "2. Save to TTN e-facturation system", 
            "3. Validate XML signature",
            "4. Transform XML to HTML preview",
            "5. Package all files into ZIP"
        ));
        info.put("requiredCredentials", Arrays.asList(
            "TTN: username, password, matriculeFiscal",
            "ANCE SEAL: pin, alias, certificate path"
        ));
        
        return ResponseEntity.ok(info);
    }
    
    /**
     * Clean up old ZIP files
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupOldFiles(
            @RequestParam(defaultValue = "24") int hoursOld) {
        
        logger.info("Cleaning up ZIP files older than {} hours", hoursOld);
        
        try {
            zipService.cleanupOldZipFiles(hoursOld);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Cleanup completed for files older than " + hoursOld + " hours");
            result.put("timestamp", java.time.Instant.now().toString());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Cleanup failed: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Cleanup failed: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Get base URL from request
     */
    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();
        
        StringBuilder baseUrl = new StringBuilder();
        baseUrl.append(scheme).append("://").append(serverName);
        
        if ((scheme.equals("http") && serverPort != 80) || 
            (scheme.equals("https") && serverPort != 443)) {
            baseUrl.append(":").append(serverPort);
        }
        
        baseUrl.append(contextPath);
        return baseUrl.toString();
    }
}
