package com.example.unifiedapi.service;

import com.example.unifiedapi.dto.WorkflowResponse;
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
import java.nio.file.Path;
import java.util.Map;

/**
 * Service responsible for collecting processing results, creating ZIP files,
 * and finalizing workflow responses.
 */
@Service
public class ResultCollectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(ResultCollectionService.class);
    
    private final ZipService zipService;
    private final ProgressTrackingService progressTrackingService;
    private final UserCredentialsService userCredentialsService;
    private final OperationLogService operationLogService;
    
    @Autowired
    public ResultCollectionService(ZipService zipService,
                                 ProgressTrackingService progressTrackingService,
                                 UserCredentialsService userCredentialsService,
                                 OperationLogService operationLogService) {
        this.zipService = zipService;
        this.progressTrackingService = progressTrackingService;
        this.userCredentialsService = userCredentialsService;
        this.operationLogService = operationLogService;
    }
    
    /**
     * Finalize workflow response by creating ZIP file and setting final status
     */
    public WorkflowResponse finalizeWorkflowResponse(WorkflowResponse response,
                                                   Map<String, String> signedXmlFiles,
                                                   Map<String, String> validationReports,
                                                   Map<String, String> htmlFiles,
                                                   Map<String, String> errorReports,
                                                   int successfulFiles, int failedFiles, int totalFiles,
                                                   String sessionId, String baseUrl, User currentUser) {
        
        logger.info("📦 Finalizing workflow response - Success: {}, Failed: {}, Total: {}", 
                   successfulFiles, failedFiles, totalFiles);
        
        try {
            // Create ZIP file with all results
            createResultsZipFile(response, signedXmlFiles, validationReports, htmlFiles, errorReports,
                               sessionId, baseUrl, currentUser);
            
            // Set final response status and message
            setFinalResponseStatus(response, successfulFiles, failedFiles, totalFiles);
            
            // Complete workflow progress tracking
            completeWorkflowProgress(sessionId, successfulFiles, failedFiles, totalFiles);
            
            // Log final workflow status
            logFinalWorkflowStatus(currentUser, successfulFiles, failedFiles, totalFiles);
            
        } catch (Exception e) {
            logger.error("❌ Failed to finalize workflow response: {}", e.getMessage(), e);
            response.setSuccess(false);
            response.setMessage("Traitement terminé mais échec de finalisation: " + e.getMessage());
            progressTrackingService.completeWorkflow(sessionId, false, 
                "Erreur de finalisation: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * Create ZIP file with all processing results
     */
    private void createResultsZipFile(WorkflowResponse response,
                                    Map<String, String> signedXmlFiles,
                                    Map<String, String> validationReports,
                                    Map<String, String> htmlFiles,
                                    Map<String, String> errorReports,
                                    String sessionId, String baseUrl, User currentUser) throws Exception {
        
        logger.info("📦 Creating ZIP file with {} signed files, {} validation reports, {} HTML files, {} error reports",
                   signedXmlFiles.size(), validationReports.size(), htmlFiles.size(), errorReports.size());
        
        progressTrackingService.updateWorkflowProgress(sessionId, "PROCESSING", "PACKAGE", 95,
            "Création de l'archive ZIP...");
        
        String ttnUsername = userCredentialsService.getTtnUsername(currentUser);
        // Build map of filename to TTN invoice ID from workflow results
        Map<String, String> ttnInvoiceIds = new java.util.HashMap<>();
        if (response.getResults() != null) {
            for (WorkflowResponse.FileProcessingResult result : response.getResults()) {
                if (result.getTtnInvoiceId() != null && !result.getTtnInvoiceId().isEmpty()) {
                    ttnInvoiceIds.put(result.getFilename(), result.getTtnInvoiceId());
                }
            }
        }
        Path zipPath = zipService.createProcessedFilesZipWithErrors(
            signedXmlFiles, validationReports, htmlFiles, errorReports, ttnInvoiceIds, ttnUsername
        );
        
        String downloadUrl = zipService.getDownloadUrl(zipPath, baseUrl);
        response.setZipDownloadUrl(downloadUrl);
        
        // Set ZIP download URL in progress tracking
        progressTrackingService.setZipDownloadUrl(sessionId, downloadUrl);
        
        logger.info("✅ ZIP file created successfully: {}", downloadUrl);
    }
    
    /**
     * Set final response status and message based on processing results
     */
    private void setFinalResponseStatus(WorkflowResponse response, int successfulFiles, 
                                      int failedFiles, int totalFiles) {
        
        if (successfulFiles > 0) {
            response.setSuccess(true);
            if (failedFiles > 0) {
                response.setMessage(String.format(
                    "Traitement parallèle terminé avec succès partiel. %d/%d fichiers réussis, %d fichiers échoués. Archive ZIP créée avec tous les résultats.",
                    successfulFiles, totalFiles, failedFiles
                ));
            } else {
                response.setMessage(String.format(
                    "Traitement parallèle terminé avec succès complet. %d/%d fichiers traités avec succès. Archive ZIP créée.",
                    successfulFiles, totalFiles
                ));
            }
        } else {
            response.setSuccess(false);
            response.setMessage(String.format(
                "Traitement parallèle terminé avec échec. %d/%d fichiers échoués. Archive ZIP créée avec les rapports d'erreur.",
                failedFiles, totalFiles
            ));
        }
        
        logger.info("📊 Final status - Success: {}, Message: {}", response.isSuccess(), response.getMessage());
    }
    
    /**
     * Complete workflow progress tracking
     */
    private void completeWorkflowProgress(String sessionId, int successfulFiles, 
                                        int failedFiles, int totalFiles) {
        
        boolean overallSuccess = successfulFiles > 0;
        String completionMessage;
        
        if (overallSuccess) {
            if (failedFiles > 0) {
                completionMessage = String.format("Traitement parallèle terminé avec succès partiel: %d/%d fichiers réussis", 
                                                 successfulFiles, totalFiles);
            } else {
                completionMessage = String.format("Traitement parallèle terminé avec succès complet: %d/%d fichiers traités", 
                                                 successfulFiles, totalFiles);
            }
        } else {
            completionMessage = String.format("Traitement parallèle terminé avec échec: %d fichiers échoués", failedFiles);
        }
        
        progressTrackingService.completeWorkflow(sessionId, overallSuccess, completionMessage);
        logger.info("📈 Workflow progress completed - Session: {}, Success: {}", sessionId, overallSuccess);
    }
    
    /**
     * Log final workflow status for audit purposes
     */
    private void logFinalWorkflowStatus(User currentUser, int successfulFiles, 
                                      int failedFiles, int totalFiles) {
        try {
            String ttnUsername = userCredentialsService.getTtnUsername(currentUser);
            String ttnMatriculeFiscal = userCredentialsService.getTtnMatriculeFiscal(currentUser);
            
            String operationType = "PARALLEL_WORKFLOW_COMPLETE";
            String status = successfulFiles > 0 ? "SUCCESS" : "FAILURE";
            String details = String.format("Parallel workflow completed: %d successful, %d failed, %d total. ZIP created.", 
                                          successfulFiles, failedFiles, totalFiles);
            
            operationLogService.logOperationWithDetails(
                operationType, status, null, null, details,
                ttnUsername, ttnMatriculeFiscal, null
            );
            
            logger.info("📝 Workflow completion logged - Type: {}, Status: {}", operationType, status);
            
        } catch (Exception logError) {
            logger.warn("Failed to log final workflow status: {}", logError.getMessage());
        }
    }
    

    
    /**
     * Generate summary statistics for the workflow
     */
    public String generateWorkflowSummary(int successfulFiles, int failedFiles, int totalFiles) {
        double successRate = totalFiles > 0 ? (double) successfulFiles / totalFiles * 100 : 0;
        
        return String.format(
            "Workflow Summary: %d/%d files processed successfully (%.1f%% success rate), %d failed",
            successfulFiles, totalFiles, successRate, failedFiles
        );
    }
}
