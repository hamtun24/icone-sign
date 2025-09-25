package com.example.unifiedapi.dto;

import java.util.List;

public class TtnSaveEfactResponse {
    
    private boolean success;
    private int totalProcessed;
    private int successCount;
    private int errorCount;
    private List<FileResult> results;
    
    // Constructors
    public TtnSaveEfactResponse() {}
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public int getTotalProcessed() {
        return totalProcessed;
    }
    
    public void setTotalProcessed(int totalProcessed) {
        this.totalProcessed = totalProcessed;
    }
    
    public int getSuccessCount() {
        return successCount;
    }
    
    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }
    
    public int getErrorCount() {
        return errorCount;
    }
    
    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }
    
    public List<FileResult> getResults() {
        return results;
    }
    
    public void setResults(List<FileResult> results) {
        this.results = results;
    }
    
    // Inner class for file results
    public static class FileResult {
        private String filename;
        private boolean success;
        private String reference;
        private String rawResponse;
        private String error;
        
        // Constructors
        public FileResult() {}
        
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
        
        public String getReference() {
            return reference;
        }
        
        public void setReference(String reference) {
            this.reference = reference;
        }
        
        public String getRawResponse() {
            return rawResponse;
        }
        
        public void setRawResponse(String rawResponse) {
            this.rawResponse = rawResponse;
        }
        
        public String getError() {
            return error;
        }
        
        public void setError(String error) {
            this.error = error;
        }
    }
}
