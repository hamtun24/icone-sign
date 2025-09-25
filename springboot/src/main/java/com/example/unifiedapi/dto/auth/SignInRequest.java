package com.example.unifiedapi.dto.auth;

import jakarta.validation.constraints.NotBlank;

public class SignInRequest {
    
    @NotBlank(message = "Le nom d'utilisateur ou l'email est requis")
    private String usernameOrEmail;
    
    @NotBlank(message = "Le mot de passe est requis")
    private String password;
    
    // Constructors
    public SignInRequest() {}
    
    public SignInRequest(String usernameOrEmail, String password) {
        this.usernameOrEmail = usernameOrEmail;
        this.password = password;
    }
    
    // Getters and Setters
    public String getUsernameOrEmail() {
        return usernameOrEmail;
    }
    
    public void setUsernameOrEmail(String usernameOrEmail) {
        this.usernameOrEmail = usernameOrEmail;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
}
