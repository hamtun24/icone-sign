package com.example.unifiedapi.dto.user;

public class UserStatisticsDto {
    private long totalUsers;
    private long activeUsers;
    private long verifiedUsers;
    private long usersWithCredentials;
    private long totalSessions;
    private long completedSessions;
    private long failedSessions;
    private long totalFilesProcessed;
    
    // Constructors
    public UserStatisticsDto() {}
    
    // Getters and Setters
    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }
    
    public long getActiveUsers() { return activeUsers; }
    public void setActiveUsers(long activeUsers) { this.activeUsers = activeUsers; }
    
    public long getVerifiedUsers() { return verifiedUsers; }
    public void setVerifiedUsers(long verifiedUsers) { this.verifiedUsers = verifiedUsers; }
    
    public long getUsersWithCredentials() { return usersWithCredentials; }
    public void setUsersWithCredentials(long usersWithCredentials) { this.usersWithCredentials = usersWithCredentials; }
    
    public long getTotalSessions() { return totalSessions; }
    public void setTotalSessions(long totalSessions) { this.totalSessions = totalSessions; }
    
    public long getCompletedSessions() { return completedSessions; }
    public void setCompletedSessions(long completedSessions) { this.completedSessions = completedSessions; }
    
    public long getFailedSessions() { return failedSessions; }
    public void setFailedSessions(long failedSessions) { this.failedSessions = failedSessions; }
    
    public long getTotalFilesProcessed() { return totalFilesProcessed; }
    public void setTotalFilesProcessed(long totalFilesProcessed) { this.totalFilesProcessed = totalFilesProcessed; }
}
