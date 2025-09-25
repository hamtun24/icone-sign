package com.example.unifiedapi.service;

import com.example.unifiedapi.dto.user.*;
import com.example.unifiedapi.entity.User;
import com.example.unifiedapi.entity.WorkflowSession;
import com.example.unifiedapi.repository.UserRepository;
import com.example.unifiedapi.repository.WorkflowSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserManagementService.class);
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom random = new SecureRandom();
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private WorkflowSessionRepository workflowSessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private EncryptionService encryptionService;
    
    /**
     * Get all users with filtering and pagination
     */
    public Page<UserSummaryDto> getAllUsers(Pageable pageable, String search, String role, Boolean isActive) {
        Specification<User> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                Predicate searchPredicate = criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), searchPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), searchPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), searchPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), searchPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("companyName")), searchPattern)
                );
                predicates.add(searchPredicate);
            }
            
            if (role != null && !role.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("role"), User.Role.valueOf(role.toUpperCase())));
            }
            
            if (isActive != null) {
                predicates.add(criteriaBuilder.equal(root.get("isActive"), isActive));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        
        Page<User> users = userRepository.findAll(spec, pageable);
        
        // Convert to DTOs with statistics
        List<UserSummaryDto> userDtos = users.getContent().stream()
            .map(user -> {
                UserSummaryDto dto = new UserSummaryDto(user);
                // Add statistics
                addUserStatistics(dto, user);
                return dto;
            })
            .collect(Collectors.toList());
        
        return new PageImpl<>(userDtos, pageable, users.getTotalElements());
    }
    
    /**
     * Get user by ID with full details
     */
    public UserDetailDto getUserById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + id));
        
        UserDetailDto dto = new UserDetailDto(user);
        addUserStatistics(dto, user);
        return dto;
    }
    
    /**
     * Create new user
     */
    public UserDetailDto createUser(CreateUserRequest request) {
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Ce nom d'utilisateur est déjà utilisé");
        }
        
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Cette adresse email est déjà utilisée");
        }
        
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setCompanyName(request.getCompanyName());
        user.setRole(User.Role.valueOf(request.getRole().toUpperCase()));
        user.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        user.setIsVerified(request.getIsVerified() != null ? request.getIsVerified() : true);
        
        // Encrypt and store credentials if provided
        if (request.getTtnUsername() != null && !request.getTtnUsername().isEmpty()) {
            user.setTtnUsername(encryptionService.encrypt(request.getTtnUsername()));
        }
        if (request.getTtnPassword() != null && !request.getTtnPassword().isEmpty()) {
            user.setTtnPassword(encryptionService.encrypt(request.getTtnPassword()));
        }
        if (request.getTtnMatriculeFiscal() != null && !request.getTtnMatriculeFiscal().isEmpty()) {
            user.setTtnMatriculeFiscal(encryptionService.encrypt(request.getTtnMatriculeFiscal()));
        }
        if (request.getAnceSealPin() != null && !request.getAnceSealPin().isEmpty()) {
            user.setAnceSealPin(encryptionService.encrypt(request.getAnceSealPin()));
        }
        if (request.getAnceSealAlias() != null && !request.getAnceSealAlias().isEmpty()) {
            user.setAnceSealAlias(encryptionService.encrypt(request.getAnceSealAlias()));
        }
        if (request.getCertificatePath() != null && !request.getCertificatePath().isEmpty()) {
            user.setCertificatePath(request.getCertificatePath());
        }
        
        user = userRepository.save(user);
        logger.info("User created: {} by admin", user.getUsername());
        
        return new UserDetailDto(user);
    }
    
    /**
     * Update user
     */
    public UserDetailDto updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + id));
        
        // Check if email is being changed and if it's already used
        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Cette adresse email est déjà utilisée");
        }
        
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setCompanyName(request.getCompanyName());
        
        if (request.getRole() != null) {
            user.setRole(User.Role.valueOf(request.getRole().toUpperCase()));
        }
        
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }
        
        if (request.getIsVerified() != null) {
            user.setIsVerified(request.getIsVerified());
        }
        
        user = userRepository.save(user);
        logger.info("User updated: {} by admin", user.getUsername());
        
        return new UserDetailDto(user);
    }
    
    /**
     * Update user credentials
     */
    public void updateUserCredentials(Long id, UpdateCredentialsRequest request) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + id));
        
        // Update TTN credentials
        if (request.getTtnUsername() != null) {
            user.setTtnUsername(request.getTtnUsername().isEmpty() ? null : encryptionService.encrypt(request.getTtnUsername()));
        }
        if (request.getTtnPassword() != null) {
            user.setTtnPassword(request.getTtnPassword().isEmpty() ? null : encryptionService.encrypt(request.getTtnPassword()));
        }
        if (request.getTtnMatriculeFiscal() != null) {
            user.setTtnMatriculeFiscal(request.getTtnMatriculeFiscal().isEmpty() ? null : encryptionService.encrypt(request.getTtnMatriculeFiscal()));
        }
        
        // Update ANCE credentials
        if (request.getAnceSealPin() != null) {
            user.setAnceSealPin(request.getAnceSealPin().isEmpty() ? null : encryptionService.encrypt(request.getAnceSealPin()));
        }
        if (request.getAnceSealAlias() != null) {
            user.setAnceSealAlias(request.getAnceSealAlias().isEmpty() ? null : encryptionService.encrypt(request.getAnceSealAlias()));
        }
        if (request.getCertificatePath() != null) {
            user.setCertificatePath(request.getCertificatePath().isEmpty() ? null : request.getCertificatePath());
        }
        
        userRepository.save(user);
        logger.info("User credentials updated: {} by admin", user.getUsername());
    }
    
    /**
     * Toggle user active status
     */
    public UserDetailDto toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + id));
        
        user.setIsActive(!user.getIsActive());
        user = userRepository.save(user);
        
        logger.info("User status toggled: {} - Active: {}", user.getUsername(), user.getIsActive());
        
        return new UserDetailDto(user);
    }
    
    /**
     * Reset user password
     */
    public String resetUserPassword(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + id));
        
        String newPassword = generateRandomPassword(12);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        logger.info("Password reset for user: {} by admin", user.getUsername());
        
        return newPassword;
    }
    
    /**
     * Delete user
     */
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + id));
        
        // Check if user has active sessions
        long activeSessions = workflowSessionRepository.countUserSessionsSince(user, LocalDateTime.now().minusHours(24));
        if (activeSessions > 0) {
            throw new RuntimeException("Impossible de supprimer l'utilisateur: il a des sessions actives récentes");
        }
        
        userRepository.delete(user);
        logger.info("User deleted: {} by admin", user.getUsername());
    }
    
    /**
     * Get user statistics
     */
    public UserStatisticsDto getUserStatistics() {
        UserStatisticsDto stats = new UserStatisticsDto();
        
        stats.setTotalUsers(userRepository.count());
        stats.setActiveUsers(userRepository.findByIsActiveTrue().size());
        stats.setVerifiedUsers(userRepository.findByIsVerifiedFalse().size());
        
        // Count users with credentials
        long usersWithCredentials = userRepository.findAll().stream()
            .mapToLong(user -> hasValidCredentials(user) ? 1 : 0)
            .sum();
        stats.setUsersWithCredentials(usersWithCredentials);
        
        // Session statistics
        stats.setTotalSessions(workflowSessionRepository.count());
        stats.setCompletedSessions(workflowSessionRepository.findByStatus(WorkflowSession.Status.COMPLETED).size());
        stats.setFailedSessions(workflowSessionRepository.findByStatus(WorkflowSession.Status.FAILED).size());
        
        // File processing statistics
        long totalFiles = workflowSessionRepository.findAll().stream()
            .mapToLong(session -> session.getTotalFiles() != null ? session.getTotalFiles() : 0)
            .sum();
        stats.setTotalFilesProcessed(totalFiles);
        
        return stats;
    }
    
    /**
     * Get user sessions
     */
    public Page<UserSessionDto> getUserSessions(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));
        
        List<WorkflowSession> sessions = workflowSessionRepository.findByUserOrderByCreatedAtDesc(user);
        
        List<UserSessionDto> sessionDtos = sessions.stream()
            .map(this::convertToSessionDto)
            .collect(Collectors.toList());
        
        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), sessionDtos.size());
        List<UserSessionDto> pageContent = sessionDtos.subList(start, end);
        
        return new PageImpl<>(pageContent, pageable, sessionDtos.size());
    }
    
    /**
     * Get user activity logs
     */
    public Page<UserActivityDto> getUserActivity(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));
        
        // This would need OperationLogRepository implementation
        // For now, return empty page
        return new PageImpl<>(new ArrayList<>(), pageable, 0);
    }
    
    // Helper methods
    private void addUserStatistics(UserSummaryDto dto, User user) {
        List<WorkflowSession> sessions = workflowSessionRepository.findByUser(user);
        dto.setTotalSessions(sessions.size());
        dto.setCompletedSessions(sessions.stream().mapToLong(s -> s.getStatus() == WorkflowSession.Status.COMPLETED ? 1 : 0).sum());
        dto.setFailedSessions(sessions.stream().mapToLong(s -> s.getStatus() == WorkflowSession.Status.FAILED ? 1 : 0).sum());
        dto.setTotalFilesProcessed(sessions.stream().mapToLong(s -> s.getTotalFiles() != null ? s.getTotalFiles() : 0).sum());
    }
    
    private void addUserStatistics(UserDetailDto dto, User user) {
        List<WorkflowSession> sessions = workflowSessionRepository.findByUser(user);
        dto.setTotalSessions(sessions.size());
        dto.setCompletedSessions(sessions.stream().mapToLong(s -> s.getStatus() == WorkflowSession.Status.COMPLETED ? 1 : 0).sum());
        dto.setFailedSessions(sessions.stream().mapToLong(s -> s.getStatus() == WorkflowSession.Status.FAILED ? 1 : 0).sum());
        dto.setTotalFilesProcessed(sessions.stream().mapToLong(s -> s.getTotalFiles() != null ? s.getTotalFiles() : 0).sum());
    }
    
    private boolean hasValidCredentials(User user) {
        return user.getTtnUsername() != null && !user.getTtnUsername().isEmpty() &&
               user.getTtnPassword() != null && !user.getTtnPassword().isEmpty() &&
               user.getAnceSealPin() != null && !user.getAnceSealPin().isEmpty() &&
               user.getCertificatePath() != null && !user.getCertificatePath().isEmpty();
    }
    
    private UserSessionDto convertToSessionDto(WorkflowSession session) {
        UserSessionDto dto = new UserSessionDto();
        dto.setSessionId(session.getSessionId());
        dto.setStatus(session.getStatus().name());
        dto.setCurrentStage(session.getCurrentStage().name());
        dto.setOverallProgress(session.getOverallProgress());
        dto.setTotalFiles(session.getTotalFiles());
        dto.setSuccessfulFiles(session.getSuccessfulFiles());
        dto.setFailedFiles(session.getFailedFiles());
        dto.setCreatedAt(session.getCreatedAt());
        dto.setCompletedAt(session.getCompletedAt());
        return dto;
    }
    
    private String generateRandomPassword(int length) {
        StringBuilder password = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            password.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return password.toString();
    }
}
