package com.example.unifiedapi.dto.user;

import com.example.unifiedapi.entity.User;
import java.time.LocalDateTime;

public class UserSummaryDto {
    
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String companyName;
    private String role;
    private boolean isActive;
    private boolean isVerified;
    private boolean hasCredentials;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private long totalSessions;
    private long completedSessions;
    private long failedSessions;
    private long totalFilesProcessed;
    
    // Constructors
    public UserSummaryDto() {}
    
    public UserSummaryDto(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.companyName = user.getCompanyName();
        this.role = user.getRole().name();
        this.isActive = user.getIsActive();
        this.isVerified = user.getIsVerified();
        this.hasCredentials = hasValidCredentials(user);
        this.createdAt = user.getCreatedAt();
        this.lastLogin = user.getLastLogin();
    }
    
    private boolean hasValidCredentials(User user) {
        return user.getTtnUsername() != null && !user.getTtnUsername().isEmpty() &&
               user.getTtnPassword() != null && !user.getTtnPassword().isEmpty() &&
               user.getAnceSealPin() != null && !user.getAnceSealPin().isEmpty() &&
               user.getCertificatePath() != null && !user.getCertificatePath().isEmpty();
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
    
    public boolean isHasCredentials() { return hasCredentials; }
    public void setHasCredentials(boolean hasCredentials) { this.hasCredentials = hasCredentials; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
    
    public long getTotalSessions() { return totalSessions; }
    public void setTotalSessions(long totalSessions) { this.totalSessions = totalSessions; }
    
    public long getCompletedSessions() { return completedSessions; }
    public void setCompletedSessions(long completedSessions) { this.completedSessions = completedSessions; }
    
    public long getFailedSessions() { return failedSessions; }
    public void setFailedSessions(long failedSessions) { this.failedSessions = failedSessions; }
    
    public long getTotalFilesProcessed() { return totalFilesProcessed; }
    public void setTotalFilesProcessed(long totalFilesProcessed) { this.totalFilesProcessed = totalFilesProcessed; }
}
