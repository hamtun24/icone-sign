package com.example.unifiedapi.service;
import com.example.unifiedapi.entity.User;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AnceSealClient {

    @Value("${ance.seal.signUrl}")
    private String signUrlTemplate; // e.g. https://193.95.63.230/tunsign-proxy-webapp/services/rest/tunsign-proxy/signHash/{alias}/SHA256

    @Value("${ance.seal.pin}")
    private String pin;

    @Value("${ance.seal.validationUrl}")
    private String validationUrl;

    @Value("${ance.seal.reportsDir:reports}")
    private String reportsDirectory;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final UserCredentialsService userCredentialsService;

    @Autowired
    public AnceSealClient(RestTemplate restTemplate, ObjectMapper objectMapper, UserCredentialsService userCredentialsService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.userCredentialsService = userCredentialsService;
    }

    /**
     * Calls the ANCE SEAL API to validate the XML signature and saves the validation details as a JSON file.
     *
     * @param xmlContent The XML content to validate.
     * @return The path to the saved JSON validation report file.
     * @throws Exception if validation or saving fails.
     */
    public String validateSignatureWithResponse(String xmlContent) throws Exception {
        return validateSignatureWithResponse(xmlContent, null);
    }

    public String validateSignatureWithResponse(String xmlContent, String ttnInvoiceId) throws Exception {
        // Encode XML content in Base64
        String base64Xml = Base64.getEncoder().encodeToString(xmlContent.getBytes(StandardCharsets.UTF_8));

        Map<String, Object> mimeTypeMap = new HashMap<>();
        mimeTypeMap.put("mimeTypeString", "text/xml");

        Map<String, Object> signedDocument = new HashMap<>();
        signedDocument.put("bytes", base64Xml);
        signedDocument.put("name", "signed.xml");
        signedDocument.put("mimeType", mimeTypeMap);

        Map<String, Object> payload = new HashMap<>();
        payload.put("signedDocument", signedDocument);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        // Call the ANCE validation API
        ResponseEntity<String> response = restTemplate.postForEntity(validationUrl, request, String.class);
        String rawBody = response.getBody();

        if (!response.getStatusCode().is2xxSuccessful() || rawBody == null) {
            throw new RuntimeException("ANCE SEAL validation failed: " + response.getStatusCode());
        }

        // Parse the raw ANCE response to add TTN invoice ID if needed
        String finalResponse;
        if (ttnInvoiceId != null && !ttnInvoiceId.isEmpty()) {
            // Parse the ANCE JSON response and add TTN information
            @SuppressWarnings("unchecked")
            Map<String, Object> anceResponse = objectMapper.readValue(rawBody, Map.class);
            anceResponse.put("ttnInvoiceId", ttnInvoiceId);
            anceResponse.put("ttnStatus", "Successfully saved to TTN with ID: " + ttnInvoiceId);
            finalResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(anceResponse);
        } else {
            // Use raw ANCE response as-is
            finalResponse = rawBody;
        }

        // Ensure directory exists
        Path reportsPath = Paths.get(reportsDirectory);
        if (!Files.exists(reportsPath)) {
            Files.createDirectories(reportsPath);
        }

        // Save report to file
        String fileName = "validation_report_" + System.currentTimeMillis() + ".json";
        Path filePath = reportsPath.resolve(fileName);

        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            writer.write(finalResponse);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save validation report", e);
        }

        return filePath.toString();
    }

    /**
     * Sign hash using the user's ANCE SEAL alias (dynamic URL)
     */
    public String signHash(String base64Digest, com.example.unifiedapi.entity.User user) {
        String alias = userCredentialsService.getAnceSealAlias(user);
        String dynamicSignUrl = buildSignUrl(alias);
        return signHashWithPin(base64Digest, pin, dynamicSignUrl);
    }

    public String signHashWithPin(String base64Digest, String userPin, String signUrl) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("password", userPin);
        payload.put("bytes", base64Digest);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(signUrl, request, String.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        }
        throw new RuntimeException("ANCE SEAL signing failed: " + response.getStatusCode());
    }
    /**
     * Build the dynamic sign URL using the alias
     */
    public String buildSignUrl(String alias) {
        // signUrlTemplate should contain {alias} as a placeholder
        if (signUrlTemplate.contains("{alias}")) {
            return signUrlTemplate.replace("{alias}", alias);
        }
        // fallback: append alias if not present
        if (!signUrlTemplate.endsWith("/")) {
            return signUrlTemplate + "/" + alias + "/SHA256";
        }
        return signUrlTemplate + alias + "/SHA256";
    }

    @Async
    public CompletableFuture<String> signHashAsync(String base64Digest, User user) {
        return CompletableFuture.supplyAsync(() -> signHash(base64Digest, user));
    }

    @Async
    public CompletableFuture<String> validateSignatureAsync(String xmlContent) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return validateSignatureWithResponse(xmlContent);
            } catch (Exception e) {
                throw new RuntimeException("Async validation failed", e);
            }
        });
    }

    // Inner class for validation response
    public static class ValidationResponse {
        private boolean success;
        private String message;
        private Object details;

        public ValidationResponse() {}

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Object getDetails() {
            return details;
        }

        public void setDetails(Object details) {
            this.details = details;
        }
    }
}
