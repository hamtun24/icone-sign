package com.example.unifiedapi.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice_processing_records")
public class InvoiceProcessingRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String filename;
    
    @Column(nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String matriculeFiscal;
    
    @Column(nullable = false)
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;
    
    @Column(nullable = false)
    private String status; // PROCESSING, COMPLETED, FAILED
    
    private String currentStage; // SIGN, SAVE, VALIDATE, TRANSFORM, ZIP
    
    // Processing stages completion flags
    private boolean signCompleted = false;
    private boolean saveCompleted = false;
    private boolean validateCompleted = false;
    private boolean transformCompleted = false;
    
    // Results
    private String ttnInvoiceId;
    private String signedXmlPath;
    private String validationReportPath;
    private String htmlPreviewPath;
    
    // Error information
    private String errorMessage;
    private String errorStage;
    
    // File information
    private Long originalFileSize;
    private Long processedFileSize;
    
    // Constructors
    public InvoiceProcessingRecord() {
        this.startTime = LocalDateTime.now();
        this.status = "PROCESSING";
    }
    
    public InvoiceProcessingRecord(String filename, String username, String matriculeFiscal) {
        this();
        this.filename = filename;
        this.username = username;
        this.matriculeFiscal = matriculeFiscal;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getFilename() {
        return filename;
    }
    
    public void setFilename(String filename) {
        this.filename = filename;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getMatriculeFiscal() {
        return matriculeFiscal;
    }
    
    public void setMatriculeFiscal(String matriculeFiscal) {
        this.matriculeFiscal = matriculeFiscal;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getCurrentStage() {
        return currentStage;
    }
    
    public void setCurrentStage(String currentStage) {
        this.currentStage = currentStage;
    }
    
    public boolean isSignCompleted() {
        return signCompleted;
    }
    
    public void setSignCompleted(boolean signCompleted) {
        this.signCompleted = signCompleted;
    }
    
    public boolean isSaveCompleted() {
        return saveCompleted;
    }
    
    public void setSaveCompleted(boolean saveCompleted) {
        this.saveCompleted = saveCompleted;
    }
    
    public boolean isValidateCompleted() {
        return validateCompleted;
    }
    
    public void setValidateCompleted(boolean validateCompleted) {
        this.validateCompleted = validateCompleted;
    }
    
    public boolean isTransformCompleted() {
        return transformCompleted;
    }
    
    public void setTransformCompleted(boolean transformCompleted) {
        this.transformCompleted = transformCompleted;
    }
    
    public String getTtnInvoiceId() {
        return ttnInvoiceId;
    }
    
    public void setTtnInvoiceId(String ttnInvoiceId) {
        this.ttnInvoiceId = ttnInvoiceId;
    }
    
    public String getSignedXmlPath() {
        return signedXmlPath;
    }
    
    public void setSignedXmlPath(String signedXmlPath) {
        this.signedXmlPath = signedXmlPath;
    }
    
    public String getValidationReportPath() {
        return validationReportPath;
    }
    
    public void setValidationReportPath(String validationReportPath) {
        this.validationReportPath = validationReportPath;
    }
    
    public String getHtmlPreviewPath() {
        return htmlPreviewPath;
    }
    
    public void setHtmlPreviewPath(String htmlPreviewPath) {
        this.htmlPreviewPath = htmlPreviewPath;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getErrorStage() {
        return errorStage;
    }
    
    public void setErrorStage(String errorStage) {
        this.errorStage = errorStage;
    }
    
    public Long getOriginalFileSize() {
        return originalFileSize;
    }
    
    public void setOriginalFileSize(Long originalFileSize) {
        this.originalFileSize = originalFileSize;
    }
    
    public Long getProcessedFileSize() {
        return processedFileSize;
    }
    
    public void setProcessedFileSize(Long processedFileSize) {
        this.processedFileSize = processedFileSize;
    }
    
    // Utility methods
    public void markCompleted() {
        this.status = "COMPLETED";
        this.endTime = LocalDateTime.now();
    }
    
    public void markFailed(String errorMessage, String errorStage) {
        this.status = "FAILED";
        this.errorMessage = errorMessage;
        this.errorStage = errorStage;
        this.endTime = LocalDateTime.now();
    }
    
    public long getProcessingDurationMinutes() {
        if (endTime != null && startTime != null) {
            return java.time.Duration.between(startTime, endTime).toMinutes();
        }
        return 0;
    }
}
