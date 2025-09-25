package com.example.unifiedapi.service;

import com.example.unifiedapi.entity.User;
import com.example.unifiedapi.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UserCredentialsService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserCredentialsService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EncryptionService encryptionService;
    
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Utilisateur non authentifié");
        }

        // Get user directly from authentication principal if available
        if (authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }

        // Fallback to username lookup
        String username = authentication.getName();
        return userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + username));
    }
    
    public String getTtnUsername() {
        User user = getCurrentUser();
        if (user.getTtnUsername() == null || user.getTtnUsername().isEmpty()) {
            throw new RuntimeException("Nom d'utilisateur TTN non configuré pour l'utilisateur: " + user.getUsername());
        }
        return encryptionService.decrypt(user.getTtnUsername());
    }
    
    public String getTtnPassword() {
        User user = getCurrentUser();
        if (user.getTtnPassword() == null || user.getTtnPassword().isEmpty()) {
            throw new RuntimeException("Mot de passe TTN non configuré pour l'utilisateur: " + user.getUsername());
        }
        return encryptionService.decrypt(user.getTtnPassword());
    }
    
    public String getTtnMatriculeFiscal() {
        User user = getCurrentUser();
        if (user.getTtnMatriculeFiscal() == null || user.getTtnMatriculeFiscal().isEmpty()) {
            throw new RuntimeException("Matricule fiscal TTN non configuré pour l'utilisateur: " + user.getUsername());
        }
        return encryptionService.decrypt(user.getTtnMatriculeFiscal());
    }
    
    public String getAnceSealPin() {
        User user = getCurrentUser();
        if (user.getAnceSealPin() == null || user.getAnceSealPin().isEmpty()) {
            throw new RuntimeException("PIN ANCE SEAL non configuré pour l'utilisateur: " + user.getUsername());
        }
        return encryptionService.decrypt(user.getAnceSealPin());
    }
    
    public String getAnceSealAlias() {
        User user = getCurrentUser();
        if (user.getAnceSealAlias() == null || user.getAnceSealAlias().isEmpty()) {
            return null; // Optional field
        }
        return encryptionService.decrypt(user.getAnceSealAlias());
    }
    
    public String getCertificatePath() {
        User user = getCurrentUser();
        if (user.getCertificatePath() == null || user.getCertificatePath().isEmpty()) {
            throw new RuntimeException("Chemin du certificat non configuré pour l'utilisateur: " + user.getUsername());
        }
        return user.getCertificatePath(); // Not encrypted
    }
    
    public boolean hasValidCredentials() {
        try {
            User user = getCurrentUser();
            return user.getTtnUsername() != null && !user.getTtnUsername().isEmpty() &&
                   user.getTtnPassword() != null && !user.getTtnPassword().isEmpty() &&
                   user.getAnceSealPin() != null && !user.getAnceSealPin().isEmpty() &&
                   user.getCertificatePath() != null && !user.getCertificatePath().isEmpty();
        } catch (Exception e) {
            logger.warn("Error checking user credentials: {}", e.getMessage());
            return false;
        }
    }
    
    public void validateCredentials() {
        if (!hasValidCredentials()) {
            throw new RuntimeException("Identifiants TTN et ANCE SEAL incomplets. Veuillez mettre à jour votre profil.");
        }
    }

    // Methods that accept a User parameter to avoid security context issues in async threads
    public boolean hasValidCredentials(User user) {
        return user.getTtnUsername() != null && !user.getTtnUsername().isEmpty() &&
               user.getTtnPassword() != null && !user.getTtnPassword().isEmpty() &&
               user.getAnceSealPin() != null && !user.getAnceSealPin().isEmpty() &&
               user.getCertificatePath() != null && !user.getCertificatePath().isEmpty();
    }

    public void validateCredentials(User user) {
        if (!hasValidCredentials(user)) {
            throw new RuntimeException("Identifiants TTN et ANCE SEAL incomplets. Veuillez mettre à jour votre profil.");
        }
    }

    public String getTtnUsername(User user) {
        if (user.getTtnUsername() == null || user.getTtnUsername().isEmpty()) {
            throw new RuntimeException("Nom d'utilisateur TTN non configuré pour l'utilisateur: " + user.getUsername());
        }
        return encryptionService.decrypt(user.getTtnUsername());
    }

    public String getTtnPassword(User user) {
        if (user.getTtnPassword() == null || user.getTtnPassword().isEmpty()) {
            throw new RuntimeException("Mot de passe TTN non configuré pour l'utilisateur: " + user.getUsername());
        }
        return encryptionService.decrypt(user.getTtnPassword());
    }

    public String getTtnMatriculeFiscal(User user) {
        if (user.getTtnMatriculeFiscal() == null || user.getTtnMatriculeFiscal().isEmpty()) {
            throw new RuntimeException("Matricule fiscal TTN non configuré pour l'utilisateur: " + user.getUsername());
        }
        return encryptionService.decrypt(user.getTtnMatriculeFiscal());
    }

    public String getAnceSealPin(User user) {
        if (user.getAnceSealPin() == null || user.getAnceSealPin().isEmpty()) {
            throw new RuntimeException("PIN ANCE SEAL non configuré pour l'utilisateur: " + user.getUsername());
        }
        return encryptionService.decrypt(user.getAnceSealPin());
    }

    public String getAnceSealAlias(User user) {
        if (user.getAnceSealAlias() == null || user.getAnceSealAlias().isEmpty()) {
            return null; // Optional field
        }
        return encryptionService.decrypt(user.getAnceSealAlias());
    }

    public String getCertificatePath(User user) {
        if (user.getCertificatePath() == null || user.getCertificatePath().isEmpty()) {
            throw new RuntimeException("Chemin du certificat non configuré pour l'utilisateur: " + user.getUsername());
        }
        return user.getCertificatePath(); // Not encrypted
    }
}
