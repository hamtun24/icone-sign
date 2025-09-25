package com.example.unifiedapi.dto;

import java.util.List;

public class WorkflowResponse {
    
    private boolean success;
    private int totalFiles;
    private int successfulFiles;
    private int failedFiles;
    private List<FileProcessingResult> results;
    private String zipDownloadUrl; // URL to download the ZIP file
    private String message;
    private String sessionId; // Progress tracking session ID
    
    // Constructors
    public WorkflowResponse() {}
    
    public WorkflowResponse(boolean success, int totalFiles, int successfulFiles, int failedFiles) {
        this.success = success;
        this.totalFiles = totalFiles;
        this.successfulFiles = successfulFiles;
        this.failedFiles = failedFiles;
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public int getTotalFiles() {
        return totalFiles;
    }
    
    public void setTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
    }
    
    public int getSuccessfulFiles() {
        return successfulFiles;
    }
    
    public void setSuccessfulFiles(int successfulFiles) {
        this.successfulFiles = successfulFiles;
    }
    
    public int getFailedFiles() {
        return failedFiles;
    }
    
    public void setFailedFiles(int failedFiles) {
        this.failedFiles = failedFiles;
    }
    
    public List<FileProcessingResult> getResults() {
        return results;
    }
    
    public void setResults(List<FileProcessingResult> results) {
        this.results = results;
    }
    
    public String getZipDownloadUrl() {
        return zipDownloadUrl;
    }
    
    public void setZipDownloadUrl(String zipDownloadUrl) {
        this.zipDownloadUrl = zipDownloadUrl;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    // Inner class for individual file results
    public static class FileProcessingResult {
        private String filename;
        private boolean success;
        private String stage; // SIGN, SAVE, VALIDATE, TRANSFORM
        private String ttnInvoiceId;
        private String errorMessage;
        private ProcessingStages stages;
        
        // Constructors
        public FileProcessingResult() {}
        
        public FileProcessingResult(String filename) {
            this.filename = filename;
            this.stages = new ProcessingStages();
        }
        
        // Getters and Setters
        public String getFilename() {
            return filename;
        }
        
        public void setFilename(String filename) {
            this.filename = filename;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public void setSuccess(boolean success) {
            this.success = success;
        }
        
        public String getStage() {
            return stage;
        }
        
        public void setStage(String stage) {
            this.stage = stage;
        }
        
        public String getTtnInvoiceId() {
            return ttnInvoiceId;
        }
        
        public void setTtnInvoiceId(String ttnInvoiceId) {
            this.ttnInvoiceId = ttnInvoiceId;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
        
        public ProcessingStages getStages() {
            return stages;
        }
        
        public void setStages(ProcessingStages stages) {
            this.stages = stages;
        }
    }
    
    // Inner class for tracking processing stages
    public static class ProcessingStages {
        private boolean signCompleted;
        private boolean saveCompleted;
        private boolean validateCompleted;
        private boolean transformCompleted;
        
        // Getters and Setters
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
    }
}
