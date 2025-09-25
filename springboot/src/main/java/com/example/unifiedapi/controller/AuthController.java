package com.example.unifiedapi.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.example.unifiedapi.dto.auth.AuthResponse;
import com.example.unifiedapi.dto.auth.SignInRequest;
import com.example.unifiedapi.dto.auth.SignUpRequest;
import com.example.unifiedapi.entity.User;
import com.example.unifiedapi.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")  // ✅ FIXED: Added proper API mapping
@CrossOrigin(
    origins = "*",
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS},
    allowedHeaders = "*",
    maxAge = 3600
)
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private AuthService authService;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private com.example.unifiedapi.repository.UserRepository userRepository;

    /**
     * Test endpoint to verify controller is working
     */
    @GetMapping("/test")
    public ResponseEntity<?> test() {
        logger.info("Test endpoint called - AuthController is working!");
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "AuthController is working!",
            "timestamp", java.time.LocalDateTime.now()
        ));
    }

    @PostMapping("/signin")
    public ResponseEntity<?> signIn(@Valid @RequestBody SignInRequest signInRequest) {
        logger.info("Sign in request received for user: {}", signInRequest.getUsernameOrEmail());
        
        try {
            AuthResponse authResponse = authService.signIn(signInRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Connexion réussie");
            response.put("data", authResponse);
            
            return ResponseEntity.ok(response);
            
        } catch (BadCredentialsException e) {
            logger.warn("Invalid credentials for user: {}", signInRequest.getUsernameOrEmail());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Nom d'utilisateur ou mot de passe incorrect");
            
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            logger.error("Sign in error for user: {}", signInRequest.getUsernameOrEmail(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erreur lors de la connexion: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@Valid @RequestBody SignUpRequest signUpRequest) {
        logger.info("Sign up request received for user: {}", signUpRequest.getUsername());
        
        try {
            AuthResponse authResponse = authService.signUp(signUpRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Inscription réussie");
            response.put("data", authResponse);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            logger.warn("Sign up failed for user: {} - {}", signUpRequest.getUsername(), e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            logger.error("Sign up error for user: {}", signUpRequest.getUsername(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erreur lors de l'inscription: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // Get the user directly from the authentication principal instead of looking up by username
            User user = (User) authentication.getPrincipal();
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("email", user.getEmail());
            userInfo.put("firstName", user.getFirstName());
            userInfo.put("lastName", user.getLastName());
            userInfo.put("companyName", user.getCompanyName());
            userInfo.put("role", user.getRole().name());
            userInfo.put("hasCredentials", hasValidCredentials(user));
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", userInfo);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting current user", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erreur lors de la récupération des informations utilisateur");
            
            return ResponseEntity.internalServerError().body(response);
        }
    } ;
    
    private boolean hasValidCredentials(User user) {
        return user.getTtnUsername() != null && !user.getTtnUsername().isEmpty() &&
               user.getTtnPassword() != null && !user.getTtnPassword().isEmpty() &&
               user.getAnceSealPin() != null && !user.getAnceSealPin().isEmpty() &&
               user.getCertificatePath() != null && !user.getCertificatePath().isEmpty();
    }

    /**
     * TEMPORARY: Fix admin password - REMOVE IN PRODUCTION
     */
    @PostMapping("/fix-admin-password")
    public ResponseEntity<?> fixAdminPassword() {
        try {
            // Find admin user
            var adminUser = userRepository.findByEmail("admin@iconeSign.com");

            if (adminUser.isPresent()) {
                var user = adminUser.get();
                // Set the correct password hash for "admin123"
                String hashedPassword = passwordEncoder.encode("admin123");
                user.setPassword(hashedPassword);
                userRepository.save(user);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Admin password fixed successfully");
                response.put("username", user.getUsername());
                response.put("email", user.getEmail());

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Admin user not found"
                ));
            }

        } catch (Exception e) {
            logger.error("Error fixing admin password", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error fixing admin password: " + e.getMessage()
            ));
        }
    }

    /**
     * TEMPORARY: Check user info - REMOVE IN PRODUCTION
     */
    @GetMapping("/check-user/{email}")
    public ResponseEntity<?> checkUser(@PathVariable String email) {
        try {
            var user = userRepository.findByEmail(email);

            if (user.isPresent()) {
                var u = user.get();
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("found", true);
                response.put("id", u.getId());
                response.put("username", u.getUsername());
                response.put("email", u.getEmail());
                response.put("role", u.getRole());
                response.put("isActive", u.getIsActive());
                response.put("isVerified", u.getIsVerified());
                response.put("passwordLength", u.getPassword() != null ? u.getPassword().length() : 0);

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "found", false,
                    "message", "User not found"
                ));
            }

        } catch (Exception e) {
            logger.error("Error checking user", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error checking user: " + e.getMessage()
            ));
        }
    }

    /**
     * TEMPORARY: Debug user status - REMOVE IN PRODUCTION
     */
    @GetMapping("/debug-user/{email}")
    public ResponseEntity<?> debugUser(@PathVariable String email) {
        try {
            var user = userRepository.findByEmail(email);

            if (user.isPresent()) {
                var u = user.get();
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("found", true);
                response.put("id", u.getId());
                response.put("username", u.getUsername());
                response.put("email", u.getEmail());
                response.put("role", u.getRole());
                response.put("isActive", u.getIsActive());
                response.put("isVerified", u.getIsVerified());
                response.put("isEnabled", u.isEnabled());
                response.put("isAccountNonLocked", u.isAccountNonLocked());
                response.put("isAccountNonExpired", u.isAccountNonExpired());
                response.put("isCredentialsNonExpired", u.isCredentialsNonExpired());

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "found", false,
                    "message", "User not found"
                ));
            }

        } catch (Exception e) {
            logger.error("Error debugging user", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error debugging user: " + e.getMessage()
            ));
        }
    }

    /**
     * TEMPORARY: Test password verification - REMOVE IN PRODUCTION
     */
    @PostMapping("/test-password")
    public ResponseEntity<?> testPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String password = request.get("password");

            var userOpt = userRepository.findByEmail(email);

            if (userOpt.isPresent()) {
                var user = userOpt.get();
                boolean passwordMatches = passwordEncoder.matches(password, user.getPassword());

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("email", user.getEmail());
                response.put("username", user.getUsername());
                response.put("isActive", user.getIsActive());
                response.put("isVerified", user.getIsVerified());
                response.put("isEnabled", user.isEnabled());
                response.put("passwordMatches", passwordMatches);
                response.put("role", user.getRole().name());

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "User not found"
                ));
            }

        } catch (Exception e) {
            logger.error("Error testing password", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error testing password: " + e.getMessage()
            ));
        }
    }
}
