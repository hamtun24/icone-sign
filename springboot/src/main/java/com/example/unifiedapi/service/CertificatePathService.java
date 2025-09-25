package com.example.unifiedapi.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class CertificatePathService {

    private static final Logger logger = LoggerFactory.getLogger(CertificatePathService.class);

    @Value("${ance.seal.certificate.default-path:classpath:certificates/icone.cer}")
    private String defaultCertificatePath;

    @Value("${ance.seal.certificate.fallback-path:./certificates/icone.cer}")
    private String fallbackCertificatePath;

    /**
     * Resolves the certificate path, checking user-provided path first, then defaults
     */
    public String resolveCertificatePath(String userProvidedPath) {
        logger.info("Resolving certificate path. User provided: {}", userProvidedPath);

        // 1. If user provided a path, try to use it
        if (userProvidedPath != null && !userProvidedPath.trim().isEmpty() && 
            !userProvidedPath.equals("/path/to/certificate.crt")) {
            
            String resolvedPath = resolveAndValidatePath(userProvidedPath.trim());
            if (resolvedPath != null) {
                logger.info("Using user-provided certificate path: {}", resolvedPath);
                return resolvedPath;
            }
        }

        // 2. Try default classpath resource
        String resolvedDefault = resolveClasspathResource(defaultCertificatePath);
        if (resolvedDefault != null) {
            logger.info("Using default certificate path: {}", resolvedDefault);
            return resolvedDefault;
        }

        // 2.1. Try auto-detecting certificate with different extensions
        String autoDetected = autoDetectCertificate();
        if (autoDetected != null) {
            logger.info("Auto-detected certificate path: {}", autoDetected);
            return autoDetected;
        }

        // 3. Try fallback file path
        String resolvedFallback = resolveAndValidatePath(fallbackCertificatePath);
        if (resolvedFallback != null) {
            logger.info("Using fallback certificate path: {}", resolvedFallback);
            return resolvedFallback;
        }

        // 4. If nothing works, throw exception with helpful message
        throw new IllegalArgumentException(
            String.format("Certificate not found. Tried:\n" +
                "1. User path: %s\n" +
                "2. Default: %s\n" +
                "3. Fallback: %s\n" +
                "Please place 'icone.crt' in src/main/resources/certificates/ or provide a valid path.",
                userProvidedPath, defaultCertificatePath, fallbackCertificatePath)
        );
    }

    /**
     * Resolves classpath resource and extracts to temp file if needed
     */
    private String resolveClasspathResource(String classpathPath) {
        if (!classpathPath.startsWith("classpath:")) {
            return resolveAndValidatePath(classpathPath);
        }

        try {
            String resourcePath = classpathPath.substring("classpath:".length());
            ClassPathResource resource = new ClassPathResource(resourcePath);
            
            if (!resource.exists()) {
                logger.debug("Classpath resource not found: {}", classpathPath);
                return null;
            }

            // For classpath resources, we need to extract to a temporary file
            // because the certificate loading requires a file path
            Path tempFile = Files.createTempFile("cert_", ".crt");
            
            try (InputStream inputStream = resource.getInputStream()) {
                Files.copy(inputStream, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            
            // Mark for deletion on exit
            tempFile.toFile().deleteOnExit();
            
            String tempPath = tempFile.toAbsolutePath().toString();
            logger.debug("Extracted classpath certificate to temporary file: {}", tempPath);
            return tempPath;
            
        } catch (IOException e) {
            logger.error("Failed to extract classpath certificate: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Validates that a file path exists and is readable
     */
    private String resolveAndValidatePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }

        try {
            Path certPath = Paths.get(path);
            
            if (Files.exists(certPath) && Files.isReadable(certPath)) {
                return certPath.toAbsolutePath().toString();
            } else {
                logger.debug("Certificate file not found or not readable: {}", path);
                return null;
            }
        } catch (Exception e) {
            logger.debug("Invalid certificate path {}: {}", path, e.getMessage());
            return null;
        }
    }

    /**
     * Gets the default certificate path for display in UI
     */
    public String getDefaultCertificatePath() {
        return defaultCertificatePath;
    }

    /**
     * Lists available certificates in the resources directory
     */
    public String[] getAvailableCertificates() {
        try {
            // Try to detect known certificate files instead of listing directory
            // This works better when running from JAR files
            String[] possibleCerts = {
                "icone.crt", "icone.cer", "icone.pem",
                "certificate.crt", "certificate.cer", "certificate.pem",
                "TnTrustQualifiedGovCA.crt", "TunTrustQualifiedCAInter.crt"
            };
            java.util.List<String> foundCerts = new java.util.ArrayList<>();

            for (String certName : possibleCerts) {
                ClassPathResource resource = new ClassPathResource("certificates/" + certName);
                if (resource.exists()) {
                    foundCerts.add(certName);
                }
            }

            return foundCerts.toArray(new String[0]);

        } catch (Exception e) {
            logger.debug("Could not detect certificates: {}", e.getMessage());
        }

        return new String[0];
    }

    /**
     * Gets the paths to the ANCE certificate chain files
     */
    public String[] getAnceCertificateChain() {
        String[] chainFiles = {
            "classpath:certificates/icone.cer",                    // Signing certificate
            "classpath:certificates/TunTrustQualifiedCAInter.crt", // Intermediate
            "classpath:certificates/TnTrustQualifiedGovCA.crt"     // Root
        };

        java.util.List<String> existingChain = new java.util.ArrayList<>();

        for (String chainFile : chainFiles) {
            String resolved = resolveClasspathResource(chainFile);
            if (resolved != null) {
                existingChain.add(resolved);
            }
        }

        return existingChain.toArray(new String[0]);
    }

    /**
     * Auto-detects certificate file with different extensions
     */
    private String autoDetectCertificate() {
        String[] extensions = {".cer", ".crt", ".pem"};
        String[] baseNames = {"icone", "certificate", "cert"};

        for (String baseName : baseNames) {
            for (String ext : extensions) {
                String classpathPath = "classpath:certificates/" + baseName + ext;
                String resolved = resolveClasspathResource(classpathPath);
                if (resolved != null) {
                    logger.debug("Auto-detected certificate: {}", classpathPath);
                    return resolved;
                }
            }
        }

        return null;
    }

    /**
     * Validates certificate path format
     */
    public boolean isValidCertificatePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }

        String trimmedPath = path.trim();

        // Check for placeholder paths
        if (trimmedPath.equals("/path/to/certificate.crt") ||
            trimmedPath.equals("path/to/certificate.crt")) {
            return false;
        }

        // Check file extension
        return trimmedPath.toLowerCase().endsWith(".crt") ||
               trimmedPath.toLowerCase().endsWith(".pem") ||
               trimmedPath.toLowerCase().endsWith(".cer");
    }
}
