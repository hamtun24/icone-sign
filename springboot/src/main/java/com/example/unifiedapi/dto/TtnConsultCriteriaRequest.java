package com.example.unifiedapi.dto;

/**
 * Request DTO for TTN consult operations using stored user credentials
 * Only contains search criteria, credentials are retrieved from authenticated user
 */
public class TtnConsultCriteriaRequest {
    
    private Object criteria; // Can be a Map or custom criteria object
    
    // Constructors
    public TtnConsultCriteriaRequest() {}
    
    public TtnConsultCriteriaRequest(Object criteria) {
        this.criteria = criteria;
    }
    
    // Getters and Setters
    public Object getCriteria() {
        return criteria;
    }
    
    public void setCriteria(Object criteria) {
        this.criteria = criteria;
    }
}
