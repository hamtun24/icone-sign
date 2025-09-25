package com.example.unifiedapi.dto;

import jakarta.validation.constraints.NotBlank;

public class WorkflowRequest {
    
    // TTN Credentials
    @NotBlank(message = "TTN username is required")
    private String ttnUsername;
    
    @NotBlank(message = "TTN password is required")
    private String ttnPassword;
    
    @NotBlank(message = "TTN matricule fiscal is required")
    private String ttnMatriculeFiscal;
    
    // ANCE SEAL Credentials
    @NotBlank(message = "ANCE SEAL pin is required")
    private String anceSealPin;
    
    @NotBlank(message = "ANCE SEAL alias is required")
    private String anceSealAlias;
    
    @NotBlank(message = "Certificate file path is required")
    private String certificatePath; // Path to .crt file
    
    // Constructors
    public WorkflowRequest() {}
    
    public WorkflowRequest(String ttnUsername, String ttnPassword, String ttnMatriculeFiscal,
                          String anceSealPin, String anceSealAlias, String certificatePath) {
        this.ttnUsername = ttnUsername;
        this.ttnPassword = ttnPassword;
        this.ttnMatriculeFiscal = ttnMatriculeFiscal;
        this.anceSealPin = anceSealPin;
        this.anceSealAlias = anceSealAlias;
        this.certificatePath = certificatePath;
    }
    
    // Getters and Setters
    public String getTtnUsername() {
        return ttnUsername;
    }
    
    public void setTtnUsername(String ttnUsername) {
        this.ttnUsername = ttnUsername;
    }
    
    public String getTtnPassword() {
        return ttnPassword;
    }
    
    public void setTtnPassword(String ttnPassword) {
        this.ttnPassword = ttnPassword;
    }
    
    public String getTtnMatriculeFiscal() {
        return ttnMatriculeFiscal;
    }
    
    public void setTtnMatriculeFiscal(String ttnMatriculeFiscal) {
        this.ttnMatriculeFiscal = ttnMatriculeFiscal;
    }
    
    public String getAnceSealPin() {
        return anceSealPin;
    }
    
    public void setAnceSealPin(String anceSealPin) {
        this.anceSealPin = anceSealPin;
    }
    
    public String getAnceSealAlias() {
        return anceSealAlias;
    }
    
    public void setAnceSealAlias(String anceSealAlias) {
        this.anceSealAlias = anceSealAlias;
    }
    
    public String getCertificatePath() {
        return certificatePath;
    }
    
    public void setCertificatePath(String certificatePath) {
        this.certificatePath = certificatePath;
    }
}
