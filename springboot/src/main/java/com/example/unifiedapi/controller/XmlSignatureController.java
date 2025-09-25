package com.example.unifiedapi.controller;

import com.example.unifiedapi.service.XmlSignatureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ance-seal")
public class XmlSignatureController {

    private static final Logger logger = LoggerFactory.getLogger(XmlSignatureController.class);
    
    private final XmlSignatureService xmlSignatureService;

    @Autowired
    public XmlSignatureController(XmlSignatureService xmlSignatureService) {
        this.xmlSignatureService = xmlSignatureService;
    }

    @PostMapping(value = "/sign", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> signXml(@RequestParam("files") MultipartFile[] files, @RequestAttribute("user") com.example.unifiedapi.entity.User user) {
        logger.info("Received XML signature request for {} files", files.length);
        try {
            List<Map<String, Object>> results = new ArrayList<>();
            for (MultipartFile file : files) {
                Map<String, Object> result = new HashMap<>();
                try {
                    String signedXml = xmlSignatureService.signXml(file, user);
                    result.put("success", true);
                    result.put("signedXml", signedXml);
                    result.put("originalFileName", file.getOriginalFilename());
                } catch (Exception e) {
                    logger.error("Error signing file {}: {}", file.getOriginalFilename(), e.getMessage(), e);
                    result.put("success", false);
                    result.put("error", e.getMessage());
                    result.put("originalFileName", file.getOriginalFilename());
                }
                results.add(result);
            }
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("Error in signXml: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PostMapping(value = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> validateXml(@RequestParam("file") MultipartFile file) {
        logger.info("Received XML validation request for file: {}", file.getOriginalFilename());
        
        Map<String, Object> response = new HashMap<>();
        try {
            byte[] validationReport = xmlSignatureService.validateXmlSignature(file);
            if (validationReport != null) {
                response.put("valid", true);
                response.put("report", new String(validationReport));
                return ResponseEntity.ok(response);
            } else {
                response.put("valid", false);
                response.put("error", "Invalid XML signature.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (Exception e) {
            logger.error("Error validating XML signature: {}", e.getMessage(), e);
            response.put("valid", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping(value = "/batch/sign")
    public ResponseEntity<?> signBatch(@RequestParam("files") List<MultipartFile> files, @RequestAttribute("user") com.example.unifiedapi.entity.User user) {
        logger.info("Received batch XML signature request for {} files", files.size());
        try {
            return xmlSignatureService.signMultipleFilesAsync(files, user)
                    .thenApply(ResponseEntity::ok)
                    .get();
        } catch (Exception e) {
            logger.error("Error in batch signing: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        logger.debug("ANCE SEAL health check requested");
        
        Map<String, Object> health = new HashMap<>();
        health.put("status", "OK");
        health.put("service", "ANCE SEAL XML Signature API");
        health.put("timestamp", java.time.Instant.now().toString());
        
        return ResponseEntity.ok(health);
    }
}
