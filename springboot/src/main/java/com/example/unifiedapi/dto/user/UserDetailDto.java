package com.example.unifiedapi.dto.user;

import com.example.unifiedapi.entity.User;
import java.time.LocalDateTime;

public class UserDetailDto {
    
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String companyName;
    private String role;
    private boolean isActive;
    private boolean isVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLogin;
    
    // Credential status (not the actual credentials for security)
    private boolean hasTtnCredentials;
    private boolean hasAnceCredentials;
    private String certificatePath;
    
    // Statistics
    private long totalSessions;
    private long completedSessions;
    private long failedSessions;
    private long totalFilesProcessed;
    
    // Constructors
    public UserDetailDto() {}
    
    public UserDetailDto(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.companyName = user.getCompanyName();
        this.role = user.getRole().name();
        this.isActive = user.getIsActive();
        this.isVerified = user.getIsVerified();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
        this.lastLogin = user.getLastLogin();
        
        // Credential status (not actual values for security)
        this.hasTtnCredentials = user.getTtnUsername() != null && !user.getTtnUsername().isEmpty() &&
                                user.getTtnPassword() != null && !user.getTtnPassword().isEmpty() &&
                                user.getTtnMatriculeFiscal() != null && !user.getTtnMatriculeFiscal().isEmpty();
        
        this.hasAnceCredentials = user.getAnceSealPin() != null && !user.getAnceSealPin().isEmpty();
        this.certificatePath = user.getCertificatePath();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
    
    public boolean isHasTtnCredentials() { return hasTtnCredentials; }
    public void setHasTtnCredentials(boolean hasTtnCredentials) { this.hasTtnCredentials = hasTtnCredentials; }
    
    public boolean isHasAnceCredentials() { return hasAnceCredentials; }
    public void setHasAnceCredentials(boolean hasAnceCredentials) { this.hasAnceCredentials = hasAnceCredentials; }
    
    public String getCertificatePath() { return certificatePath; }
    public void setCertificatePath(String certificatePath) { this.certificatePath = certificatePath; }
    
    public long getTotalSessions() { return totalSessions; }
    public void setTotalSessions(long totalSessions) { this.totalSessions = totalSessions; }
    
    public long getCompletedSessions() { return completedSessions; }
    public void setCompletedSessions(long completedSessions) { this.completedSessions = completedSessions; }
    
    public long getFailedSessions() { return failedSessions; }
    public void setFailedSessions(long failedSessions) { this.failedSessions = failedSessions; }
    
    public long getTotalFilesProcessed() { return totalFilesProcessed; }
    public void setTotalFilesProcessed(long totalFilesProcessed) { this.totalFilesProcessed = totalFilesProcessed; }
}
