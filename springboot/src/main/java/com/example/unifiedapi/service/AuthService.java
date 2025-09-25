package com.example.unifiedapi.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.unifiedapi.dto.auth.AuthResponse;
import com.example.unifiedapi.dto.auth.SignInRequest;
import com.example.unifiedapi.dto.auth.SignUpRequest;
import com.example.unifiedapi.entity.User;
import com.example.unifiedapi.repository.UserRepository;
import com.example.unifiedapi.util.JwtUtil;

@Service
@Transactional
public class AuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private EncryptionService encryptionService;
    
    public AuthResponse signIn(SignInRequest request) throws AuthenticationException {
        logger.info("Sign in attempt for user: {}", request.getUsernameOrEmail());
        
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsernameOrEmail(),
                    request.getPassword()
                )
            );
            
            User user = (User) authentication.getPrincipal();
            
            // Update last login
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
            
            // Generate JWT token with user info
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", user.getId());
            claims.put("role", user.getRole().name());
            claims.put("email", user.getEmail());
            
            String token = jwtUtil.generateToken(user, claims);
            
            logger.info("User {} signed in successfully", user.getUsername());
            return new AuthResponse(token, user);
            
        } catch (AuthenticationException e) {
            logger.warn("Sign in failed for user: {} - {}", request.getUsernameOrEmail(), e.getMessage());
            throw e;
        }
    }
    
    public AuthResponse signUp(SignUpRequest request) {
        logger.info("Sign up attempt for user: {}", request.getUsername());
        
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Ce nom d'utilisateur est déjà utilisé");
        }
        
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Cette adresse email est déjà utilisée");
        }
        
        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setCompanyName(request.getCompanyName());
        user.setIsVerified(true); // Auto-verify for now
        
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
        
        // Generate JWT token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().name());
        claims.put("email", user.getEmail());
        
        String token = jwtUtil.generateToken(user, claims);
        
        logger.info("User {} registered successfully", user.getUsername());
        return new AuthResponse(token, user);
    }
    
 public User getCurrentUser(String usernameOrEmail) {
    return userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
}

}
