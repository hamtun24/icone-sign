package com.example.unifiedapi.dto;

import jakarta.validation.constraints.NotBlank;

public class TtnSaveEfactRequest {
    
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Password is required")
    private String password;
    
    @NotBlank(message = "Matricule fiscal is required")
    private String matriculeFiscal;
    
    // Constructors
    public TtnSaveEfactRequest() {}
    
    public TtnSaveEfactRequest(String username, String password, String matriculeFiscal) {
        this.username = username;
        this.password = password;
        this.matriculeFiscal = matriculeFiscal;
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
}
