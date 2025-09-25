package com.example.unifiedapi.dto;

import jakarta.validation.constraints.NotBlank;

public class TtnConsultEfactRequest {
    
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Password is required")
    private String password;
    
    @NotBlank(message = "Matricule fiscal is required")
    private String matriculeFiscal;
    
    private Object criteria; // Can be a Map or custom criteria object
    
    // Constructors
    public TtnConsultEfactRequest() {}
    
    public TtnConsultEfactRequest(String username, String password, String matriculeFiscal, Object criteria) {
        this.username = username;
        this.password = password;
        this.matriculeFiscal = matriculeFiscal;
        this.criteria = criteria;
    }
    
    // Getters and Setters
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getMatriculeFiscal() {
        return matriculeFiscal;
    }
    
    public void setMatriculeFiscal(String matriculeFiscal) {
        this.matriculeFiscal = matriculeFiscal;
    }
    
    public Object getCriteria() {
        return criteria;
    }
    
    public void setCriteria(Object criteria) {
        this.criteria = criteria;
    }
}
