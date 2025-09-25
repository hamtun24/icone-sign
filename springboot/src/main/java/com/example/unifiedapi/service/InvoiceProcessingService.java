package com.example.unifiedapi.service;

import com.example.unifiedapi.dto.WorkflowResponse;
import com.example.unifiedapi.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Simplified InvoiceProcessingService that delegates to the new refactored services.
 * This maintains backward compatibility while using the new scalable architecture.
 * 
 * The original monolithic service has been refactored into:
 * - InvoiceWorkflowOrchestrator: Main coordinator for parallel processing
 * - InvoiceFileProcessor: Individual file processing logic
 * - TtnIntegrationService: TTN-specific operations
 * - ResultCollectionService: Result aggregation and ZIP creation
 */
@Service
public class InvoiceProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(InvoiceProcessingService.class);
    
    private final InvoiceWorkflowOrchestrator workflowOrchestrator;
    
    @Autowired
    public InvoiceProcessingService(InvoiceWorkflowOrchestrator workflowOrchestrator) {
        this.workflowOrchestrator = workflowOrchestrator;
    }
    
    /**
     * Start processing and return session ID immediately for progress tracking.
     * Delegates to InvoiceWorkflowOrchestrator for actual processing.
     */
    public String startProcessingAsync(List<MultipartFile> files, String baseUrl, User currentUser) {
        logger.info("🔄 Delegating async processing to WorkflowOrchestrator for {} files", files.size());
        return workflowOrchestrator.startProcessingAsync(files, baseUrl, currentUser);
    }
    
    /**
     * Process multiple invoice files through the complete workflow.
     * Delegates to InvoiceWorkflowOrchestrator for actual processing.
     */
    public CompletableFuture<WorkflowResponse> processInvoicesAsync(List<MultipartFile> files,
                                                                   String baseUrl, User currentUser) {
        logger.info("🔄 Delegating async workflow processing to WorkflowOrchestrator for {} files", files.size());
        return workflowOrchestrator.processInvoicesAsync(files, baseUrl, currentUser);
    }
    
    /**
     * Clean up resources.
     * Delegates to InvoiceWorkflowOrchestrator for cleanup.
     */
    public void shutdown() {
        logger.info("🔄 Delegating shutdown to WorkflowOrchestrator");
        workflowOrchestrator.shutdown();
    }
}
