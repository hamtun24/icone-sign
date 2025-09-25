package com.example.unifiedapi.dto;

import java.util.List;

/**
 * Response DTO for TTN consultation with HTML transformation
 */
public class TtnConsultHtmlResponse {
    private boolean success;
    private int count;
    private List<Object> invoices;
    private String htmlContent;
    private String htmlTransformationError;
    private String rawResponse;
    private String error;
    
    public TtnConsultHtmlResponse() {}
    
    // Getters and setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public int getCount() {
        return count;
    }
    
    public void setCount(int count) {
        this.count = count;
    }
    
    public List<Object> getInvoices() {
        return invoices;
    }
    
    public void setInvoices(List<Object> invoices) {
        this.invoices = invoices;
    }
    
    public String getHtmlContent() {
        return htmlContent;
    }
    
    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }
    
    public String getHtmlTransformationError() {
        return htmlTransformationError;
    }
    
    public void setHtmlTransformationError(String htmlTransformationError) {
        this.htmlTransformationError = htmlTransformationError;
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
    
    @Override
    public String toString() {
        return "TtnConsultHtmlResponse{" +
               "success=" + success +
               ", count=" + count +
               ", invoices=" + (invoices != null ? invoices.size() : 0) +
               ", htmlContent=" + (htmlContent != null ? "present" : "null") +
               ", htmlTransformationError='" + htmlTransformationError + '\'' +
               ", error='" + error + '\'' +
               '}';
    }
}