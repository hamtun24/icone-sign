package com.example.unifiedapi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "workflow_files")
public class WorkflowFile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_session_id", nullable = false)
    private WorkflowSession workflowSession;
    
    @Column(name = "filename", nullable = false)
    private String filename;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;
    
    @Column(name = "stage", nullable = false)
    @Enumerated(EnumType.STRING)
    private Stage stage = Stage.SIGN;
    
    @Column(name = "progress")
    private Integer progress = 0;
    
    @Column(name = "error_message", length = 2000)
    private String errorMessage;
    
    @Column(name = "ttn_invoice_id")
    private String ttnInvoiceId;
    
    @Column(name = "signed_xml_path")
    private String signedXmlPath;
    
    @Column(name = "validation_report_path")
    private String validationReportPath;
    
    @Column(name = "html_report_path")
    private String htmlReportPath;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    public enum Status {
        PENDING, PROCESSING, COMPLETED, FAILED
    }
    
    public enum Stage {
        SIGN, SAVE, VALIDATE, TRANSFORM, PACKAGE
    }
    
    // Constructors
    public WorkflowFile() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public WorkflowFile(WorkflowSession workflowSession, String filename, Long fileSize) {
        this();
        this.workflowSession = workflowSession;
        this.filename = filename;
        this.fileSize = fileSize;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public WorkflowSession getWorkflowSession() { return workflowSession; }
    public void setWorkflowSession(WorkflowSession workflowSession) { this.workflowSession = workflowSession; }
    
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    
    public Status getStatus() { return status; }
    public void setStatus(Status status) { 
        this.status = status;
        this.updatedAt = LocalDateTime.now();
        if (status == Status.COMPLETED || status == Status.FAILED) {
            this.completedAt = LocalDateTime.now();
        }
    }
    
    public Stage getStage() { return stage; }
    public void setStage(Stage stage) { 
        this.stage = stage;
        this.updatedAt = LocalDateTime.now();
    }
    
    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { 
        this.progress = progress;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public String getTtnInvoiceId() { return ttnInvoiceId; }
    public void setTtnInvoiceId(String ttnInvoiceId) { this.ttnInvoiceId = ttnInvoiceId; }
    
    public String getSignedXmlPath() { return signedXmlPath; }
    public void setSignedXmlPath(String signedXmlPath) { this.signedXmlPath = signedXmlPath; }
    
    public String getValidationReportPath() { return validationReportPath; }
    public void setValidationReportPath(String validationReportPath) { this.validationReportPath = validationReportPath; }
    
    public String getHtmlReportPath() { return htmlReportPath; }
    public void setHtmlReportPath(String htmlReportPath) { this.htmlReportPath = htmlReportPath; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
