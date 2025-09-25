package com.example.unifiedapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class TtnTransformationService {
    
    private static final Logger logger = LoggerFactory.getLogger(TtnTransformationService.class);
    
    private static final String TRANSFORMATION_URL = "https://test.elfatoora.tn/ElfatouraServicesRest/rest/api/transform";
    
    private final RestTemplate restTemplate;
    private final OperationLogService operationLogService;
    private final UserCredentialsService userCredentialsService;
    
    @Autowired
    public TtnTransformationService(RestTemplate restTemplate, OperationLogService operationLogService, UserCredentialsService userCredentialsService) {
        this.restTemplate = restTemplate;
        this.operationLogService = operationLogService;
        this.userCredentialsService = userCredentialsService;
    }
    
    /**
     * Transform XML invoice to HTML using TTN transformation service
     * 
     * @param xmlContent The signed XML content
     * @param username TTN username
     * @param password TTN password
     * @param matriculeFiscal TTN matricule fiscal
     * @param filename Original filename for logging
     * @return HTML content as string
     * @throws Exception if transformation fails
     */
    public String transformXmlToHtml(String base64XmlContent, String username, String password, 
                                   String matriculeFiscal, String filename) throws Exception {
        logger.info("Starting XML to HTML transformation for file: {}", filename);
        try {
            // Use credentials from UserCredentialsService if not provided
            String user = (username == null || username.isEmpty()) ? userCredentialsService.getTtnUsername() : username;
            String pass = (password == null || password.isEmpty()) ? userCredentialsService.getTtnPassword() : password;
            String matricule = (matriculeFiscal == null || matriculeFiscal.isEmpty()) ? userCredentialsService.getTtnMatriculeFiscal() : matriculeFiscal;

            // Use the base64-encoded xmlContent directly (from TTN consult)
            Map<String, String> requestPayload = new HashMap<>();
            requestPayload.put("login", user);
            requestPayload.put("password", pass);
            requestPayload.put("matricule", matricule);
            requestPayload.put("documentEfact", base64XmlContent);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", MediaType.TEXT_HTML_VALUE);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestPayload, headers);

            logger.debug("Sending transformation request to: {}", TRANSFORMATION_URL);

            // Make the REST call
            ResponseEntity<String> response = restTemplate.postForEntity(TRANSFORMATION_URL, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String htmlContent = response.getBody();

                if (htmlContent != null && !htmlContent.trim().isEmpty()) {
                    logger.info("XML to HTML transformation completed successfully for file: {}", filename);

                    // Log successful operation
                    operationLogService.logOperationWithDetails(
                        "TTN_TRANSFORM", "SUCCESS", (long) base64XmlContent.length(), filename,
                        "XML to HTML transformation completed", user, matricule, null
                    );

                    return htmlContent;
                } else {
                    throw new RuntimeException("Empty HTML response received from transformation service");
                }
            } else {
                throw new RuntimeException("Transformation service returned error: " + response.getStatusCode());
            }

        } catch (Exception e) {
            logger.error("XML to HTML transformation failed for file {}: {}", filename, e.getMessage(), e);

            // Log failed operation
            operationLogService.logOperationWithDetails(
                "TTN_TRANSFORM", "FAILURE", (long) base64XmlContent.length(), filename,
                "XML to HTML transformation failed", username, matriculeFiscal, e.getMessage()
            );

            throw new RuntimeException("Failed to transform XML to HTML: " + e.getMessage(), e);
        }
    }
    
    /**
     * Transform multiple XML files to HTML
     * 
     * @param xmlContents Map of filename to XML content
     * @param username TTN username
     * @param password TTN password
     * @param matriculeFiscal TTN matricule fiscal
     * @return Map of filename to HTML content
     */
    public Map<String, String> transformMultipleXmlToHtml(Map<String, String> xmlContents, 
                                                         String username, String password, 
                                                         String matriculeFiscal) {
        
        Map<String, String> htmlResults = new HashMap<>();
        
        for (Map.Entry<String, String> entry : xmlContents.entrySet()) {
            String filename = entry.getKey();
            String xmlContent = entry.getValue();
            
            try {
                String htmlContent = transformXmlToHtml(xmlContent, username, password, matriculeFiscal, filename);
                htmlResults.put(filename, htmlContent);
            } catch (Exception e) {
                logger.error("Failed to transform file {}: {}", filename, e.getMessage());
                // Continue with other files, don't fail the entire batch
                htmlResults.put(filename, createErrorHtml(filename, e.getMessage()));
            }
        }
        
        return htmlResults;
    }
    
    /**
     * Create error HTML content when transformation fails
     */
    private String createErrorHtml(String filename, String errorMessage) {
        return String.format(
            "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "    <title>Transformation Error - %s</title>\n" +
            "    <style>\n" +
            "        body { font-family: Arial, sans-serif; margin: 40px; }\n" +
            "        .error { color: #d32f2f; background: #ffebee; padding: 20px; border-radius: 4px; }\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <h1>Transformation Error</h1>\n" +
            "    <div class=\"error\">\n" +
            "        <h3>File: %s</h3>\n" +
            "        <p><strong>Error:</strong> %s</p>\n" +
            "        <p>The XML to HTML transformation failed for this file. Please check the XML format and try again.</p>\n" +
            "    </div>\n" +
            "</body>\n" +
            "</html>",
            filename, filename, errorMessage
        );
    }
    
    /**
     * Validate if the transformation service is available
     */
    public boolean isTransformationServiceAvailable() {
        try {
            // Simple health check - you might want to implement a proper health endpoint
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
            
            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                TRANSFORMATION_URL, HttpMethod.OPTIONS, request, String.class
            );
            
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.warn("Transformation service health check failed: {}", e.getMessage());
            return false;
        }
    }
}
