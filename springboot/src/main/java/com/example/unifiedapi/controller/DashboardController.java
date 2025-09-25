package com.example.unifiedapi.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.unifiedapi.dto.user.UserDetailDto;
import com.example.unifiedapi.dto.user.UserSessionDto;
import com.example.unifiedapi.dto.user.UserStatisticsDto;
import com.example.unifiedapi.dto.user.UserSummaryDto;
import com.example.unifiedapi.repository.InvoiceProcessingRecordRepository;
import com.example.unifiedapi.repository.UserRepository;
import com.example.unifiedapi.repository.WorkflowSessionRepository;
import com.example.unifiedapi.service.OperationLogService;
import com.example.unifiedapi.service.UserManagementService;

import jakarta.validation.Valid;

@Controller
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private InvoiceProcessingRecordRepository invoiceProcessingRecordRepository;

    @Autowired
    private UserManagementService userManagementService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkflowSessionRepository workflowSessionRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Map<String, Object> stats = new HashMap<>();
        
        // TTN Operations Stats
        long ttnSaveCount = operationLogService.countByOperationType("TTN_SAVE");
        long ttnConsultCount = operationLogService.countByOperationType("TTN_CONSULT");
        long totalTtnOps = ttnSaveCount + ttnConsultCount;
        
        // ANCE SEAL Operations Stats
        long anceSignCount = operationLogService.countByOperationType("ANCE_SIGN");
        long anceValidateCount = operationLogService.countByOperationType("ANCE_VALIDATE");
        long anceBatchSignCount = operationLogService.countByOperationType("ANCE_BATCH_SIGN");
        long totalAnceSealOps = anceSignCount + anceValidateCount + anceBatchSignCount;
        
        // Overall Stats
        long totalOperations = operationLogService.countAll();
        long successfulOperations = operationLogService.countByStatus("SUCCESS");
        long failedOperations = operationLogService.countByStatus("FAILURE");
        double successRate = totalOperations > 0 ? (100.0 * successfulOperations / totalOperations) : 0.0;
        
        // TTN Stats
        stats.put("ttnSaveCount", ttnSaveCount);
        stats.put("ttnConsultCount", ttnConsultCount);
        stats.put("totalTtnOps", totalTtnOps);
        
        // ANCE SEAL Stats
        stats.put("anceSignCount", anceSignCount);
        stats.put("anceValidateCount", anceValidateCount);
        stats.put("anceBatchSignCount", anceBatchSignCount);
        stats.put("totalAnceSealOps", totalAnceSealOps);
        
        // Overall Stats
        stats.put("totalOperations", totalOperations);
        stats.put("successfulOperations", successfulOperations);
        stats.put("failedOperations", failedOperations);
        stats.put("successRate", String.format("%.1f%%", successRate));
        
        // Workflow Statistics
        long workflowCompleted = invoiceProcessingRecordRepository.countByStatus("COMPLETED");
        long workflowFailed = invoiceProcessingRecordRepository.countByStatus("FAILED");
        long workflowProcessing = invoiceProcessingRecordRepository.countByStatus("PROCESSING");
        long totalWorkflows = workflowCompleted + workflowFailed + workflowProcessing;

        stats.put("workflowCompleted", workflowCompleted);
        stats.put("workflowFailed", workflowFailed);
        stats.put("workflowProcessing", workflowProcessing);
        stats.put("totalWorkflows", totalWorkflows);

        // Workflow stage statistics
        stats.put("signCompleted", invoiceProcessingRecordRepository.countSignCompleted());
        stats.put("saveCompleted", invoiceProcessingRecordRepository.countSaveCompleted());
        stats.put("validateCompleted", invoiceProcessingRecordRepository.countValidateCompleted());
        stats.put("transformCompleted", invoiceProcessingRecordRepository.countTransformCompleted());

        // Recent Operations
        stats.put("recentOperations", operationLogService.getRecentOperations(10));
        stats.put("recentWorkflows", invoiceProcessingRecordRepository.findAllOrderByStartTimeDesc().stream().limit(5).toList());

        model.addAttribute("stats", stats);
        return "dashboard";
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/dashboard";
    }

    // =====================================================
    // API DASHBOARD ENDPOINTS - /dashboard/*
    // =====================================================

    /**
     * Dashboard API Overview - Main statistics and metrics
     */
    @GetMapping("/dashboard/overview")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getDashboardOverview() {
        try {
            Map<String, Object> overview = new HashMap<>();

            // User Statistics
            long totalUsers = userRepository.count();
            long activeUsers = userRepository.findByIsActiveTrue().size();
            long verifiedUsers = userRepository.findByIsVerifiedFalse().size();

            // Session Statistics
            long totalSessions = workflowSessionRepository.count();
            long completedSessions = workflowSessionRepository.findByStatus(com.example.unifiedapi.entity.WorkflowSession.Status.COMPLETED).size();
            long failedSessions = workflowSessionRepository.findByStatus(com.example.unifiedapi.entity.WorkflowSession.Status.FAILED).size();
            long processingSessions = workflowSessionRepository.findByStatus(com.example.unifiedapi.entity.WorkflowSession.Status.PROCESSING).size();

            // Operation Statistics
            long totalOperations = operationLogService.countAll();
            long successfulOperations = operationLogService.countByStatus("SUCCESS");
            long failedOperations = operationLogService.countByStatus("FAILURE");
            double successRate = totalOperations > 0 ? (100.0 * successfulOperations / totalOperations) : 0.0;

            // Build overview response
            overview.put("users", Map.of(
                "total", totalUsers,
                "active", activeUsers,
                "verified", totalUsers - verifiedUsers,
                "inactive", totalUsers - activeUsers
            ));

            overview.put("sessions", Map.of(
                "total", totalSessions,
                "completed", completedSessions,
                "failed", failedSessions,
                "processing", processingSessions
            ));

            overview.put("operations", Map.of(
                "total", totalOperations,
                "successful", successfulOperations,
                "failed", failedOperations,
                "successRate", String.format("%.1f%%", successRate)
            ));

            overview.put("timestamp", java.time.LocalDateTime.now());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", overview
            ));

        } catch (Exception e) {
            logger.error("Error fetching dashboard overview", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "Erreur lors de la récupération du tableau de bord"));
        }
    }

    /**
     * User Management API - List all users with filtering
     */
    @GetMapping("/dashboard/users")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
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
     * User Details API - Get specific user information
     */
    @GetMapping("/dashboard/users/{id}")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserDetails(@PathVariable Long id) {
        try {
            UserDetailDto user = userManagementService.getUserById(id);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", user
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error fetching user details: {}", id, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "Erreur lors de la récupération des détails utilisateur"));
        }
    }

    /**
     * User Sessions API - Get user workflow sessions
     */
    @GetMapping("/dashboard/users/{id}/sessions")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
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
     * System Statistics API - Detailed system metrics
     */
    @GetMapping("/dashboard/statistics")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getSystemStatistics() {
        try {
            UserStatisticsDto userStats = userManagementService.getUserStatistics();

            // Additional system statistics
            Map<String, Object> systemStats = new HashMap<>();
            systemStats.put("userStatistics", userStats);

            // TTN Operations Stats
            systemStats.put("ttnOperations", Map.of(
                "saveCount", operationLogService.countByOperationType("TTN_SAVE"),
                "consultCount", operationLogService.countByOperationType("TTN_CONSULT")
            ));

            // ANCE SEAL Operations Stats
            systemStats.put("anceSealOperations", Map.of(
                "signCount", operationLogService.countByOperationType("ANCE_SIGN"),
                "validateCount", operationLogService.countByOperationType("ANCE_VALIDATE"),
                "batchSignCount", operationLogService.countByOperationType("ANCE_BATCH_SIGN")
            ));

            // Workflow Statistics
            systemStats.put("workflowStatistics", Map.of(
                "completed", invoiceProcessingRecordRepository.countByStatus("COMPLETED"),
                "failed", invoiceProcessingRecordRepository.countByStatus("FAILED"),
                "processing", invoiceProcessingRecordRepository.countByStatus("PROCESSING")
            ));

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", systemStats
            ));

        } catch (Exception e) {
            logger.error("Error fetching system statistics", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "Erreur lors de la récupération des statistiques"));
        }
    }

    /**
     * Recent Activity API - System-wide recent activity
     */
    @GetMapping("/dashboard/activity")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getRecentActivity(
            @RequestParam(defaultValue = "50") int limit) {

        try {
            // Get recent operations
            var recentOperations = operationLogService.getRecentOperations(limit);

            // Get recent workflows
            var recentWorkflows = invoiceProcessingRecordRepository.findAllOrderByStartTimeDesc()
                .stream().limit(limit).toList();

            Map<String, Object> activity = Map.of(
                "recentOperations", recentOperations,
                "recentWorkflows", recentWorkflows,
                "timestamp", java.time.LocalDateTime.now()
            );

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", activity
            ));

        } catch (Exception e) {
            logger.error("Error fetching recent activity", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "Erreur lors de la récupération de l'activité"));
        }
    }

    /**
     * Create new user
     */
    @PostMapping("/dashboard/users")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(@Valid @RequestBody com.example.unifiedapi.dto.user.CreateUserRequest request) {
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
    @PutMapping("/dashboard/users/{id}")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @Valid @RequestBody com.example.unifiedapi.dto.user.UpdateUserRequest request) {
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
    @PutMapping("/dashboard/users/{id}/credentials")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUserCredentials(@PathVariable Long id, @Valid @RequestBody com.example.unifiedapi.dto.user.UpdateCredentialsRequest request) {
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
    @PutMapping("/dashboard/users/{id}/toggle-status")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
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
    @PutMapping("/dashboard/users/{id}/reset-password")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
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
    @DeleteMapping("/dashboard/users/{id}")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
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
     * API Dashboard Documentation Page
     */
    @GetMapping("/api-dashboard")
    public String apiDashboard() {
        return "api-dashboard";
    }
}
