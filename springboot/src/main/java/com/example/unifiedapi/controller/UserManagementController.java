package com.example.unifiedapi.controller;

import com.example.unifiedapi.dto.user.*;
import com.example.unifiedapi.entity.User;
import com.example.unifiedapi.service.UserManagementService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('ADMIN')")
public class UserManagementController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserManagementController.class);
    
    @Autowired
    private UserManagementService userManagementService;
    
    /**
     * Get all users with pagination and filtering
     */
    @GetMapping
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean isActive) {
        
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<UserSummaryDto> users = userManagementService.getAllUsers(
                pageable, search, role, isActive);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", users.getContent());
            response.put("pagination", Map.of(
                "currentPage", users.getNumber(),
                "totalPages", users.getTotalPages(),
                "totalElements", users.getTotalElements(),
                "size", users.getSize(),
                "hasNext", users.hasNext(),
                "hasPrevious", users.hasPrevious()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching users", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "Erreur lors de la récupération des utilisateurs"));
        }
    }
    
    /**
     * Get user by ID with full details
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            UserDetailDto user = userManagementService.getUserById(id);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", user
            ));
            
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error fetching user by ID: {}", id, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "Erreur lors de la récupération de l'utilisateur"));
        }
    }
    
    /**
     * Create new user
     */
    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request) {
        try {
            UserDetailDto user = userManagementService.createUser(request);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Utilisateur créé avec succès",
                "data", user
            ));
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating user", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "Erreur lors de la création de l'utilisateur"));
        }
    }
    
    /**
     * Update user
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        try {
            UserDetailDto user = userManagementService.updateUser(id, request);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Utilisateur mis à jour avec succès",
                "data", user
            ));
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating user: {}", id, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "Erreur lors de la mise à jour de l'utilisateur"));
        }
    }
    
    /**
     * Update user credentials
     */
    @PutMapping("/{id}/credentials")
    public ResponseEntity<?> updateUserCredentials(@PathVariable Long id, @Valid @RequestBody UpdateCredentialsRequest request) {
        try {
            userManagementService.updateUserCredentials(id, request);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Identifiants mis à jour avec succès"
            ));
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating user credentials: {}", id, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "Erreur lors de la mise à jour des identifiants"));
        }
    }
    
    /**
     * Toggle user active status
     */
    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long id) {
        try {
            UserDetailDto user = userManagementService.toggleUserStatus(id);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", user.isActive() ? "Utilisateur activé" : "Utilisateur désactivé",
                "data", user
            ));
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error toggling user status: {}", id, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "Erreur lors du changement de statut"));
        }
    }
    
    /**
     * Reset user password
     */
    @PutMapping("/{id}/reset-password")
    public ResponseEntity<?> resetUserPassword(@PathVariable Long id) {
        try {
            String newPassword = userManagementService.resetUserPassword(id);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Mot de passe réinitialisé avec succès",
                "newPassword", newPassword
            ));
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error resetting user password: {}", id, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "Erreur lors de la réinitialisation du mot de passe"));
        }
    }
    
    /**
     * Delete user
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userManagementService.deleteUser(id);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Utilisateur supprimé avec succès"
            ));
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error deleting user: {}", id, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "Erreur lors de la suppression de l'utilisateur"));
        }
    }
    
    /**
     * Get user statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getUserStatistics() {
        try {
            UserStatisticsDto stats = userManagementService.getUserStatistics();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", stats
            ));
            
        } catch (Exception e) {
            logger.error("Error fetching user statistics", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "Erreur lors de la récupération des statistiques"));
        }
    }
    
    /**
     * Get user sessions
     */
    @GetMapping("/{id}/sessions")
    public ResponseEntity<?> getUserSessions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<UserSessionDto> sessions = userManagementService.getUserSessions(id, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", sessions.getContent());
            response.put("pagination", Map.of(
                "currentPage", sessions.getNumber(),
                "totalPages", sessions.getTotalPages(),
                "totalElements", sessions.getTotalElements(),
                "size", sessions.getSize()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching user sessions: {}", id, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "Erreur lors de la récupération des sessions"));
        }
    }
    
    /**
     * Get user activity logs
     */
    @GetMapping("/{id}/activity")
    public ResponseEntity<?> getUserActivity(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<UserActivityDto> activities = userManagementService.getUserActivity(id, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", activities.getContent());
            response.put("pagination", Map.of(
                "currentPage", activities.getNumber(),
                "totalPages", activities.getTotalPages(),
                "totalElements", activities.getTotalElements(),
                "size", activities.getSize()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching user activity: {}", id, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "Erreur lors de la récupération de l'activité"));
        }
    }
}
