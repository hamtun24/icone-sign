package com.example.unifiedapi.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateUserRequest {
    
    @NotBlank(message = "Le nom d'utilisateur est requis")
    @Size(min = 3, max = 50, message = "Le nom d'utilisateur doit contenir entre 3 et 50 caractères")
    private String username;
    
    @NotBlank(message = "L'email est requis")
    @Email(message = "Format d'email invalide")
    private String email;
    
    @NotBlank(message = "Le mot de passe est requis")
    @Size(min = 6, max = 100, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String password;
    
    @NotBlank(message = "Le prénom est requis")
    private String firstName;
    
    @NotBlank(message = "Le nom de famille est requis")
    private String lastName;
    
    private String companyName;
    
    private String role = "USER"; // Default role

    private Boolean isActive = true; // Default active

    private Boolean isVerified = true; // Default verified
    
    // TTN Credentials (optional during creation)
    private String ttnUsername;
    private String ttnPassword;
    private String ttnMatriculeFiscal;
    
    // ANCE SEAL Credentials (optional during creation)
    private String anceSealPin;
    private String anceSealAlias;
    private String certificatePath;
    
    // Constructors
    public CreateUserRequest() {}
    
    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }
    
    public String getTtnUsername() { return ttnUsername; }
    public void setTtnUsername(String ttnUsername) { this.ttnUsername = ttnUsername; }
    
    public String getTtnPassword() { return ttnPassword; }
    public void setTtnPassword(String ttnPassword) { this.ttnPassword = ttnPassword; }
    
    public String getTtnMatriculeFiscal() { return ttnMatriculeFiscal; }
    public void setTtnMatriculeFiscal(String ttnMatriculeFiscal) { this.ttnMatriculeFiscal = ttnMatriculeFiscal; }
    
    public String getAnceSealPin() { return anceSealPin; }
    public void setAnceSealPin(String anceSealPin) { this.anceSealPin = anceSealPin; }
    
    public String getAnceSealAlias() { return anceSealAlias; }
    public void setAnceSealAlias(String anceSealAlias) { this.anceSealAlias = anceSealAlias; }
    
    public String getCertificatePath() { return certificatePath; }
    public void setCertificatePath(String certificatePath) { this.certificatePath = certificatePath; }
}
