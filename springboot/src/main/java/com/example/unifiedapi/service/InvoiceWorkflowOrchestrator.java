package com.example.unifiedapi.service;

import com.example.unifiedapi.dto.FileData;
import com.example.unifiedapi.dto.WorkflowResponse;
import com.example.unifiedapi.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Main orchestrator for invoice processing workflow.
 * Coordinates parallel processing of multiple invoices through the complete workflow:
 * Sign → Save → Validate → Transform → Package
 */
@Service
public class InvoiceWorkflowOrchestrator {
    
    private static final Logger logger = LoggerFactory.getLogger(InvoiceWorkflowOrchestrator.class);
    
    private final InvoiceFileProcessor fileProcessor;
    private final ResultCollectionService resultCollectionService;
    private final ProgressTrackingService progressTrackingService;
    private final OperationLogService operationLogService;
    private final UserCredentialsService userCredentialsService;
    
    // Thread pool for parallel processing
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    
    @Autowired
    public InvoiceWorkflowOrchestrator(InvoiceFileProcessor fileProcessor,
                                     ResultCollectionService resultCollectionService,
                                     ProgressTrackingService progressTrackingService,
                                     OperationLogService operationLogService,
                                     UserCredentialsService userCredentialsService) {
        this.fileProcessor = fileProcessor;
        this.resultCollectionService = resultCollectionService;
        this.progressTrackingService = progressTrackingService;
        this.operationLogService = operationLogService;
        this.userCredentialsService = userCredentialsService;
    }
    
    /**
     * Start processing and return session ID immediately for progress tracking
     */
    public String startProcessingAsync(List<MultipartFile> files, String baseUrl, User currentUser) {
        // Create progress session first
        List<String> filenames = files.stream().map(MultipartFile::getOriginalFilename).toList();
        List<Long> fileSizes = files.stream().map(MultipartFile::getSize).toList();
        String sessionId = progressTrackingService.createProgressSession(filenames, fileSizes);
        
        logger.info("🚀 Starting async workflow processing for session: {} with {} files", sessionId, files.size());
        
        // Convert MultipartFiles to FileData for background processing
        List<FileData> fileDataList = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                FileData fileData = new FileData(
                    file.getOriginalFilename(),
                    file.getSize(),
                    file.getContentType(),
                    file.getBytes()
                );
                fileDataList.add(fileData);
            }
        } catch (Exception e) {
            logger.error("Failed to read file contents for session {}: {}", sessionId, e.getMessage(), e);
            progressTrackingService.completeWorkflow(sessionId, false, "Erreur de lecture des fichiers: " + e.getMessage());
            throw new RuntimeException("Failed to read file contents: " + e.getMessage());
        }
        
        // Start async processing in background with file data
        CompletableFuture.runAsync(() -> {
            try {
                processInvoicesSync(fileDataList, baseUrl, currentUser, sessionId);
            } catch (Exception e) {
                logger.error("Background processing failed for session {}: {}", sessionId, e.getMessage(), e);
                progressTrackingService.completeWorkflow(sessionId, false, "Erreur de traitement: " + e.getMessage());
            }
        });
        
        return sessionId;
    }
    
    /**
     * Process multiple invoice files through the complete workflow with parallel processing
     */
    public CompletableFuture<WorkflowResponse> processInvoicesAsync(List<MultipartFile> files,
                                                                   String baseUrl, User currentUser) {
        return CompletableFuture.supplyAsync(() -> {
            // Create progress session
            List<String> filenames = files.stream().map(MultipartFile::getOriginalFilename).toList();
            List<Long> fileSizes = files.stream().map(MultipartFile::getSize).toList();
            String sessionId = progressTrackingService.createProgressSession(filenames, fileSizes);
            
            // Convert MultipartFiles to FileData
            List<FileData> fileDataList = new ArrayList<>();
            try {
                for (MultipartFile file : files) {
                    FileData fileData = new FileData(
                        file.getOriginalFilename(),
                        file.getSize(),
                        file.getContentType(),
                        file.getBytes()
                    );
                    fileDataList.add(fileData);
                }
            } catch (Exception e) {
                logger.error("Failed to read file contents: {}", e.getMessage(), e);
                WorkflowResponse errorResponse = new WorkflowResponse();
                errorResponse.setTotalFiles(files.size());
                errorResponse.setResults(new ArrayList<>());
                errorResponse.setMessage("Erreur de lecture des fichiers: " + e.getMessage());
                return errorResponse;
            }
            
            return processInvoicesSync(fileDataList, baseUrl, currentUser, sessionId);
        });
    }
    
    /**
     * Synchronous processing with parallel execution of all files
     */
    public WorkflowResponse processInvoicesSync(List<FileData> fileDataList, String baseUrl, 
                                              User currentUser, String sessionId) {
        logger.info("🚀 Starting PARALLEL processing workflow for {} invoices", fileDataList.size());
        
        WorkflowResponse response = new WorkflowResponse();
        response.setSessionId(sessionId);
        response.setTotalFiles(fileDataList.size());
        response.setResults(new ArrayList<>());
        
        try {
            // Initialize thread-safe collections for results
            Map<String, String> signedXmlFiles = Collections.synchronizedMap(new HashMap<>());
            Map<String, String> validationReports = Collections.synchronizedMap(new HashMap<>());
            Map<String, String> htmlFiles = Collections.synchronizedMap(new HashMap<>());
            Map<String, String> errorReports = Collections.synchronizedMap(new HashMap<>());
            
            AtomicInteger successfulFiles = new AtomicInteger(0);
            AtomicInteger failedFiles = new AtomicInteger(0);
            
            // Process ALL files in parallel using CompletableFuture
            logger.info("🚀 Starting PARALLEL processing of {} invoices", fileDataList.size());
            
            List<CompletableFuture<WorkflowResponse.FileProcessingResult>> futures = fileDataList.stream()
                .map(fileData -> CompletableFuture.supplyAsync(() -> {
                    return fileProcessor.processInvoiceFile(fileData, currentUser, sessionId, 
                                                          signedXmlFiles, validationReports, htmlFiles, errorReports,
                                                          successfulFiles, failedFiles);
                }, executorService))
                .collect(Collectors.toList());
            
            // Wait for all files to complete processing
            logger.info("⏳ Waiting for all {} invoices to complete parallel processing...", futures.size());
            @SuppressWarnings("unchecked")
            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
            
            try {
                allOf.get(); // Wait for all to complete
                logger.info("✅ All {} invoices completed parallel processing", fileDataList.size());
            } catch (Exception e) {
                logger.error("❌ Error during parallel processing: {}", e.getMessage(), e);
            }
            
            // Collect all results
            List<WorkflowResponse.FileProcessingResult> allResults = new ArrayList<>();
            for (CompletableFuture<WorkflowResponse.FileProcessingResult> future : futures) {
                try {
                    allResults.add(future.get());
                } catch (Exception e) {
                    logger.error("Failed to get result from future: {}", e.getMessage());
                    // Create error result for failed future
                    WorkflowResponse.FileProcessingResult errorResult = new WorkflowResponse.FileProcessingResult("unknown");
                    errorResult.setSuccess(false);
                    errorResult.setErrorMessage("Processing failed: " + e.getMessage());
                    allResults.add(errorResult);
                    failedFiles.incrementAndGet();
                }
            }
            
            // Add all results to response
            response.getResults().addAll(allResults);
            response.setSuccessfulFiles(successfulFiles.get());
            response.setFailedFiles(failedFiles.get());
            
            // Create ZIP file and finalize response
            response = resultCollectionService.finalizeWorkflowResponse(
                response, signedXmlFiles, validationReports, htmlFiles, errorReports,
                successfulFiles.get(), failedFiles.get(), fileDataList.size(),
                sessionId, baseUrl, currentUser
            );
            
            // Log workflow completion
            logWorkflowCompletion(currentUser, successfulFiles.get(), failedFiles.get());
            
        } catch (Exception e) {
            logger.error("❌ Workflow processing failed for session {}: {}", sessionId, e.getMessage(), e);
            response.setSuccess(false);
            response.setMessage("Erreur de traitement du workflow: " + e.getMessage());
            progressTrackingService.completeWorkflow(sessionId, false, "Erreur: " + e.getMessage());
        }
        
        logger.info("✅ Invoice processing workflow completed. Success: {}, Failed: {}",
                   response.getSuccessfulFiles(), response.getFailedFiles());
        
        return response;
    }
    
    /**
     * Log workflow completion details
     */
    private void logWorkflowCompletion(User currentUser, int successfulFiles, int failedFiles) {
        try {
            String ttnUsername = userCredentialsService.getTtnUsername(currentUser);
            String ttnMatriculeFiscal = userCredentialsService.getTtnMatriculeFiscal(currentUser);
            operationLogService.logOperationWithDetails(
                "WORKFLOW_COMPLETE", successfulFiles > 0 ? "SUCCESS" : "FAILURE", null, null,
                String.format("Parallel workflow completed: %d successful, %d failed", successfulFiles, failedFiles),
                ttnUsername, ttnMatriculeFiscal, null
            );
        } catch (Exception logError) {
            logger.warn("Failed to log workflow completion: {}", logError.getMessage());
        }
    }
    
    /**
     * Clean up resources
     */
    public void shutdown() {
        executorService.shutdown();
    }
}
