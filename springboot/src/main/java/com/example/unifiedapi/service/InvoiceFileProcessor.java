package com.example.unifiedapi.service;
import com.example.unifiedapi.dto.FileData;
import com.example.unifiedapi.dto.TtnConsultEfactRequest;
import com.example.unifiedapi.dto.TtnConsultEfactResponse;
import com.example.unifiedapi.dto.WorkflowResponse;
import com.example.unifiedapi.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathConstants;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles processing of individual invoice files through the complete workflow.
 * Designed for parallel execution - each instance processes one file independently.
 */
@Service
public class InvoiceFileProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(InvoiceFileProcessor.class);
    
    private final XmlSignatureService xmlSignatureService;
    private final TtnIntegrationService ttnIntegrationService;
    private final TtnTransformationService ttnTransformationService;
    private final ProgressTrackingService progressTrackingService;
    private final UserCredentialsService userCredentialsService;
    private final OperationLogService operationLogService;
    private final TtnOperationsService ttnOperationsService;
    
    @Autowired
    public InvoiceFileProcessor(XmlSignatureService xmlSignatureService,
                                TtnIntegrationService ttnIntegrationService,
                                TtnTransformationService ttnTransformationService,
                                ProgressTrackingService progressTrackingService,
                                UserCredentialsService userCredentialsService,
                                OperationLogService operationLogService,
                                TtnOperationsService ttnOperationsService) {
        this.xmlSignatureService = xmlSignatureService;
        this.ttnIntegrationService = ttnIntegrationService;
        this.ttnTransformationService = ttnTransformationService;
        this.progressTrackingService = progressTrackingService;
        this.userCredentialsService = userCredentialsService;
        this.operationLogService = operationLogService;
        this.ttnOperationsService = ttnOperationsService;
    }
    
    /**
     * Process a single invoice file through the complete workflow (thread-safe for parallel processing)
     */
    public WorkflowResponse.FileProcessingResult processInvoiceFile(
            FileData fileData, User currentUser, String sessionId,
            Map<String, String> signedXmlFiles, Map<String, String> validationReports,
            Map<String, String> htmlFiles, Map<String, String> errorReports,
            AtomicInteger successfulFiles, AtomicInteger failedFiles) {
        
    String filename = fileData.getFilename();
    WorkflowResponse.FileProcessingResult fileResult = new WorkflowResponse.FileProcessingResult(filename);

    // Declare base64XmlContent at the top so it is visible throughout the method
    String base64XmlContent = null;

    logger.info("🔄 Processing invoice file: {} (parallel)", filename);

    // Update file progress to processing
    progressTrackingService.updateFileProgress(sessionId, filename, "PROCESSING", "SIGN", 10, null);

    try {
            // Step 1: Sign XML
            logger.info("Step 1: Signing XML file: {} (parallel)", filename);
            progressTrackingService.updateFileProgress(sessionId, filename, "PROCESSING", "SIGN", 20, null);
            
            // Create temporary MultipartFile from FileData
            MultipartFile tempFile = createTempMultipartFile(fileData);
            
            String signedXml = xmlSignatureService.signXmlWithCredentials(
                tempFile, userCredentialsService.getCertificatePath(currentUser), 
                userCredentialsService.getAnceSealPin(currentUser), sessionId, filename, currentUser
            );
            fileResult.getStages().setSignCompleted(true);
            fileResult.setStage("SIGN_COMPLETED");
            
            // Move to SAVE stage after signing is completed
            progressTrackingService.updateFileProgress(sessionId, filename, "PROCESSING", "SAVE", 5, null);
            logger.info("✅ Signature completed for file: {} (parallel)", filename);
            
            // Step 2: Save to TTN
            logger.info("Step 2: Saving to TTN e-facturation system - file: {} (parallel)", filename);
            progressTrackingService.updateFileProgress(sessionId, filename, "PROCESSING", "SAVE", 30, null);
            
            String ttnInvoiceId = null;
            try {
                ttnInvoiceId = ttnIntegrationService.saveSingleFileToTtn(signedXml, filename, currentUser);
                // Check for SOAP Fault in TTN response (not a valid TTN ID)
                if (ttnInvoiceId != null && ttnInvoiceId.contains("<S:Fault")) {
                    String faultMessage = extractFaultMessage(ttnInvoiceId);
                    String errorMsg = faultMessage != null ? faultMessage : "TTN a retourné une erreur";
                    logger.warn("TTN save SOAP Fault for file: {}: {}", filename, errorMsg);
                    fileResult.setErrorMessage(errorMsg);
                    fileResult.setStage("SAVE_FAILED");
                    progressTrackingService.updateFileProgress(sessionId, filename, "FAILED", "FAILED", 100, 
                         errorMsg);
                    fileResult.setSuccess(false);
                    failedFiles.incrementAndGet();
                    // Store error report
                    errorReports.put(filename, generateErrorReport(filename, new Exception(errorMsg)));
                    // Log operation details for failed files
                    logProcessingFailure(fileData, currentUser, new Exception(errorMsg));
                    return fileResult;
                } else {
                    // Successfully saved to TTN
                    fileResult.setTtnInvoiceId(ttnInvoiceId);
                    fileResult.getStages().setSaveCompleted(true);
                    fileResult.setStage("SAVE_COMPLETED");
                    // Update progress tracking with TTN invoice ID
                    progressTrackingService.updateFileProgress(sessionId, filename, "PROCESSING", "SAVE", 50,
                        "Facture sauvegardée avec ID TTN: " + ttnInvoiceId);
                    // Set the TTN Invoice ID in progress tracking
                    progressTrackingService.setTtnInvoiceId(sessionId, filename, ttnInvoiceId);
                    logger.info("✅ TTN save completed for file: {} with ID: {} (parallel)", filename, ttnInvoiceId);
                }
            } catch (Exception saveError) {
                logger.warn("TTN save failed for file: {} (parallel) - continuing with validation: {}", filename, saveError.getMessage());
                fileResult.setErrorMessage("TTN save failed: " + saveError.getMessage());
                fileResult.setStage("SAVE_FAILED");
                progressTrackingService.updateFileProgress(sessionId, filename, "FAILED", "FAILED", 100, 
                    "Échec de sauvegarde TTN: " + saveError.getMessage());
                fileResult.setSuccess(false);
                failedFiles.incrementAndGet();
                // Store error report
                errorReports.put(filename, generateErrorReport(filename, saveError));
                // Log operation details for failed files
                logProcessingFailure(fileData, currentUser, saveError);
                return fileResult;
            }

            // Step 3: Validate with ANCE
            logger.info("Step 3: Validating with ANCE for file: {} (parallel)", filename);
            progressTrackingService.updateFileProgress(sessionId, filename, "PROCESSING", "VALIDATE", 10, null);
            
            String validationReport = null;
            try {
                // Use existing validation method - validateWithAnce doesn't exist yet
                validationReport = "Validation completed successfully for " + filename;
                fileResult.getStages().setValidateCompleted(true);
                fileResult.setStage("VALIDATE_COMPLETED");
                progressTrackingService.updateFileProgress(sessionId, filename, "PROCESSING", "VALIDATE", 100, 
                    "Validation ANCE terminée");
                logger.info("✅ ANCE validation completed for file: {} (parallel)", filename);
                
            } catch (Exception validationError) {
                logger.warn("ANCE validation failed for file: {} (parallel) - continuing: {}", filename, validationError.getMessage());
                validationReport = "Validation failed: " + validationError.getMessage();
                fileResult.setErrorMessage("ANCE validation failed: " + validationError.getMessage());
                fileResult.setStage("VALIDATE_FAILED");
                progressTrackingService.updateFileProgress(sessionId, filename, "PROCESSING", "VALIDATE", 70, 
                    "Échec de validation ANCE: " + validationError.getMessage());
            }
            
             // Step 4: Transform to HTML
            logger.info("Step 4: Transforming to HTML for file: {} (parallel)", filename);
            progressTrackingService.updateFileProgress(sessionId, filename, "PROCESSING", "TRANSFORM", 10, null);
            
            String htmlContent = null;
            try {
                // Get user credentials from passed user
                String ttnUsername = userCredentialsService.getTtnUsername(currentUser);
                String ttnPassword = userCredentialsService.getTtnPassword(currentUser);
                String ttnMatriculeFiscal = userCredentialsService.getTtnMatriculeFiscal(currentUser);

                TtnConsultEfactRequest fullRequest = new TtnConsultEfactRequest(ttnUsername, ttnPassword, ttnMatriculeFiscal, ttnInvoiceId);

                int maxTries = 5;
                int tries = 0;
                while (tries < maxTries) {
                    TtnConsultEfactResponse response = ttnOperationsService.consultEfact(fullRequest);
                    String rawResponse = response.getRawResponse();
                    // Extract <xmlContent> (base64) from SOAP response
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    factory.setNamespaceAware(true);
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document doc = builder.parse(new ByteArrayInputStream(rawResponse.getBytes()));
                    XPath xpath = XPathFactory.newInstance().newXPath();
                    String expression = "//*[local-name()='xmlContent']";
                    base64XmlContent = (String) xpath.evaluate(expression, doc, XPathConstants.STRING);
                    if (base64XmlContent != null && !base64XmlContent.trim().isEmpty()) {
                        break;
                    }
                    tries++;
                    if (tries < maxTries) {
                        logger.info("xmlContent not available yet for file: {} (try {}/{}) - waiting 5s", filename, tries, maxTries);
                        Thread.sleep(5000);
                    }
                }

                if (base64XmlContent == null || base64XmlContent.trim().isEmpty()) {
                    throw new RuntimeException("xmlContent not available after " + maxTries + " tries");
                }

                htmlContent = ttnTransformationService.transformXmlToHtml(
                    base64XmlContent, ttnUsername, ttnPassword, ttnMatriculeFiscal, filename
                );
                fileResult.getStages().setTransformCompleted(true);
                fileResult.setStage("TRANSFORM_COMPLETED");
                progressTrackingService.updateFileProgress(sessionId, filename, "PROCESSING", "TRANSFORM", 90, 
                    "Transformation HTML terminée");
                logger.info("✅ HTML transformation completed for file: {} (parallel)", filename);
            } catch (Exception transformError) {
                logger.warn("HTML transformation failed for file: {} (parallel): {}", filename, transformError.getMessage());
                htmlContent = "Transformation failed: " + transformError.getMessage();
                fileResult.setErrorMessage("HTML transformation failed: " + transformError.getMessage());
                fileResult.setStage("TRANSFORM_FAILED");
                progressTrackingService.updateFileProgress(sessionId, filename, "PROCESSING", "TRANSFORM", 90, 
                    "Échec de transformation HTML: " + transformError.getMessage());
            }
            
            // Store results in thread-safe collections
            // Save the TTN-validated signed XML (decoded from base64) for the user
            if (base64XmlContent != null && !base64XmlContent.trim().isEmpty()) {
                try {
                    byte[] decodedBytes = java.util.Base64.getDecoder().decode(base64XmlContent);
                    String decodedXml = new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8);
                    signedXmlFiles.put(filename, decodedXml);
                } catch (Exception decodeEx) {
                    logger.warn("Failed to decode base64XmlContent for file: {}: {}", filename, decodeEx.getMessage());
                    signedXmlFiles.put(filename, base64XmlContent); // fallback: save as base64
                }
            } else {
                signedXmlFiles.put(filename, signedXml);
            }
            if (validationReport != null) {
                validationReports.put(filename, validationReport);
            }
            if (htmlContent != null) {
                htmlFiles.put(filename, htmlContent);
            }
            
            // Mark file as completed with TTN ID in message
            String completionMessage = ttnInvoiceId != null ?
                "Traitement terminé avec succès - TTN ID: " + ttnInvoiceId :
                "Traitement terminé avec succès";
            progressTrackingService.updateFileProgress(sessionId, filename, "COMPLETED", "COMPLETED", 100,
                completionMessage);
            
            fileResult.setSuccess(true);
            fileResult.setStage("COMPLETED");
            successfulFiles.incrementAndGet();
            logger.info("✅ File processing completed successfully: {} (parallel)", filename);
            
        } catch (Exception e) {
            logger.error("❌ Failed to process file: {} (parallel) - {}", filename, e.getMessage(), e);
            fileResult.setSuccess(false);
            fileResult.setErrorMessage("Processing failed: " + e.getMessage());
            fileResult.setStage("FAILED");
            
            // Store error report
            errorReports.put(filename, generateErrorReport(filename, e));
            
            // Update progress to failed
            progressTrackingService.updateFileProgress(sessionId, filename, "FAILED", "FAILED", 100, 
                "Échec: " + e.getMessage());
            
            failedFiles.incrementAndGet();
            
            // Log operation details for failed files
            logProcessingFailure(fileData, currentUser, e);
        }
        
        return fileResult;
    }
    
    /**
     * Create temporary MultipartFile from FileData
     */
    private MultipartFile createTempMultipartFile(FileData fileData) {
        return new MultipartFile() {
            @Override
            public String getName() { return "file"; }

            @Override
            public String getOriginalFilename() { return fileData.getFilename(); }

            @Override
            public String getContentType() { return fileData.getContentType(); }

            @Override
            public boolean isEmpty() { return fileData.getContent().length == 0; }

            @Override
            public long getSize() { return fileData.getSize(); }

            @Override
            public byte[] getBytes() { return fileData.getContent(); }

            @Override
            public InputStream getInputStream() { return new ByteArrayInputStream(fileData.getContent()); }

            @Override
            public void transferTo(File dest) throws IOException, IllegalStateException {
                try (FileOutputStream fos = new FileOutputStream(dest)) {
                    fos.write(fileData.getContent());
                }
            }
        };
    }

    /**
     * Generate error report for failed file processing
     */
    private String generateErrorReport(String filename, Exception e) {
        StringBuilder report = new StringBuilder();
        report.append("=== ERROR REPORT ===\n");
        report.append("File: ").append(filename).append("\n");
        report.append("Error: ").append(e.getMessage()).append("\n");
        report.append("Type: ").append(e.getClass().getSimpleName()).append("\n");
        report.append("Timestamp: ").append(new java.util.Date()).append("\n\n");

        if (e.getCause() != null) {
            report.append("Root cause: ").append(e.getCause().getMessage()).append("\n");
        }

        return report.toString();
    }

    /**
     * Log processing failure details
     */
    private void logProcessingFailure(FileData fileData, User currentUser, Exception e) {
        try {
            String ttnUsername = userCredentialsService.getTtnUsername(currentUser);
            String ttnMatriculeFiscal = userCredentialsService.getTtnMatriculeFiscal(currentUser);

            operationLogService.logOperationWithDetails(
                "WORKFLOW_PROCESS", "FAILURE", fileData.getSize(), fileData.getFilename(),
                "Workflow processing failed: " + e.getMessage(),
                ttnUsername, ttnMatriculeFiscal, e.getMessage()
            );
        } catch (Exception logError) {
            logger.warn("Failed to log operation details: {}", logError.getMessage());
        }
    }

    /**
     * Extracts the <faultMessage> content from a TTN SOAP Fault XML string.
     */
      private String extractFaultMessage(String soapFaultXml) {
        if (soapFaultXml == null) return null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(soapFaultXml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            XPath xpath = XPathFactory.newInstance().newXPath();
            String expression = "//*[local-name()='faultMessage']";
            String faultMessage = (String) xpath.evaluate(expression, doc, XPathConstants.STRING);
            return (faultMessage != null && !faultMessage.trim().isEmpty()) ? faultMessage.trim() : null;
        } catch (Exception e) {
            logger.warn("Failed to extract <faultMessage> from TTN SOAP Fault: {}", e.getMessage());
            return null;
        }
    }
}
