package com.example.unifiedapi.service;

import com.example.unifiedapi.dto.TtnSaveEfactRequest;
import com.example.unifiedapi.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service dedicated to TTN (Tunisie TradeNet) integration operations.
 * Handles saving invoices to TTN and extracting invoice IDs.
 */
@Service
public class TtnIntegrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(TtnIntegrationService.class);
    
    private final TtnOperationsService ttnOperationsService;
    private final UserCredentialsService userCredentialsService;
    
    @Autowired
    public TtnIntegrationService(TtnOperationsService ttnOperationsService,
                               UserCredentialsService userCredentialsService) {
        this.ttnOperationsService = ttnOperationsService;
        this.userCredentialsService = userCredentialsService;
    }
    
    /**
     * Save a single signed XML file to TTN and extract the invoice ID
     */
    public String saveSingleFileToTtn(String signedXml, String filename, User currentUser) throws Exception {
        logger.info("💾 Saving file to TTN: {}", filename);
        
        // Create a temporary MultipartFile from the signed XML
        MultipartFile tempFile = createTempMultipartFile(signedXml, filename);
        
        // Get user credentials from passed user
        String ttnUsername = userCredentialsService.getTtnUsername(currentUser);
        String ttnPassword = userCredentialsService.getTtnPassword(currentUser);
        String ttnMatriculeFiscal = userCredentialsService.getTtnMatriculeFiscal(currentUser);
        
        TtnSaveEfactRequest ttnRequest = new TtnSaveEfactRequest(
            ttnUsername, ttnPassword, ttnMatriculeFiscal
        );
        
        var saveResponse = ttnOperationsService.saveEfact(ttnRequest, Arrays.asList(tempFile));
        
        if (saveResponse.isSuccess() && saveResponse.getSuccessCount() > 0) {
            var fileResult = saveResponse.getResults().get(0);
            if (fileResult.isSuccess()) {
                String invoiceId = extractInvoiceIdFromResponse(fileResult.getReference());
                logger.info("✅ TTN save successful for file: {} with ID: {}", filename, invoiceId);
                return invoiceId;
            } else {
                throw new RuntimeException("TTN save failed: " + fileResult.getError());
            }
        } else {
            throw new RuntimeException("TTN save operation failed");
        }
    }
    
    /**
     * Extract invoice ID from TTN response
     */
    public String extractInvoiceIdFromResponse(String response) {
        // Extract ID from response like "Facture enregistree avec ID 1805137 est en cours de validation"
        Pattern pattern = Pattern.compile("ID\\s+(\\d+)");
        Matcher matcher = pattern.matcher(response);
        
        if (matcher.find()) {
            String invoiceId = matcher.group(1);
            logger.debug("Extracted invoice ID: {} from response: {}", invoiceId, response);
            return invoiceId;
        }
        
        // If no pattern match, return the full response
        logger.warn("Could not extract invoice ID from response: {}", response);
        return response;
    }
    
    /**
     * Validate TTN credentials for a user
     */
    public void validateTtnCredentials(User currentUser) throws Exception {
        String ttnUsername = userCredentialsService.getTtnUsername(currentUser);
        String ttnPassword = userCredentialsService.getTtnPassword(currentUser);
        String ttnMatriculeFiscal = userCredentialsService.getTtnMatriculeFiscal(currentUser);
        
        if (ttnUsername == null || ttnUsername.trim().isEmpty()) {
            throw new IllegalArgumentException("TTN username is required");
        }
        if (ttnPassword == null || ttnPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("TTN password is required");
        }
        if (ttnMatriculeFiscal == null || ttnMatriculeFiscal.trim().isEmpty()) {
            throw new IllegalArgumentException("TTN matricule fiscal is required");
        }
        
        logger.info("✅ TTN credentials validated for user: {}", currentUser.getUsername());
    }
    
    /**
     * Create temporary MultipartFile from signed XML content
     */
    private MultipartFile createTempMultipartFile(String signedXml, String filename) {
        byte[] xmlBytes = signedXml.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        
        return new MultipartFile() {
            @Override
            public String getName() {
                return "file";
            }
            
            @Override
            public String getOriginalFilename() {
                return filename;
            }
            
            @Override
            public String getContentType() {
                return "application/xml";
            }
            
            @Override
            public boolean isEmpty() {
                return xmlBytes.length == 0;
            }
            
            @Override
            public long getSize() {
                return xmlBytes.length;
            }
            
            @Override
            public byte[] getBytes() {
                return xmlBytes;
            }
            
            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream(xmlBytes);
            }
            
            @Override
            public void transferTo(File dest) throws IOException, IllegalStateException {
                try (FileOutputStream fos = new FileOutputStream(dest)) {
                    fos.write(xmlBytes);
                }
            }
        };
    }
    
    /**
     * Check if TTN service is available
     */
    public boolean isTtnServiceAvailable() {
        try {
            // Simple health check - could be enhanced with actual TTN ping
            return ttnOperationsService != null;
        } catch (Exception e) {
            logger.warn("TTN service availability check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get TTN service status information
     */
    public String getTtnServiceStatus() {
        if (isTtnServiceAvailable()) {
            return "TTN service is available";
        } else {
            return "TTN service is not available";
        }
    }
}
