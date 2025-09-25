package com.example.unifiedapi.dto;

import java.util.List;

public class TtnConsultEfactResponse {
    
    private boolean success;
    private int count;
    private List<Object> invoices; // Can be a list of invoice objects
    private String rawResponse;
    private String error;
    
    // Constructors
    public TtnConsultEfactResponse() {}
    
    // Getters and Setters
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
