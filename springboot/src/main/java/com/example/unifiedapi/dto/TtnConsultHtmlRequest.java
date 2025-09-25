// TtnConsultHtmlRequest.java
package com.example.unifiedapi.dto;

/**
 * Request DTO for TTN consultation with HTML transformation
 */
public class TtnConsultHtmlRequest {
    private Object criteria;
    
    public TtnConsultHtmlRequest() {}
    
    public TtnConsultHtmlRequest(Object criteria) {
        this.criteria = criteria;
    }
    
    public Object getCriteria() {
        return criteria;
    }
    
    public void setCriteria(Object criteria) {
        this.criteria = criteria;
    }
    
    @Override
    public String toString() {
        return "TtnConsultHtmlRequest{" +
               "criteria=" + criteria +
               '}';
    }
}
