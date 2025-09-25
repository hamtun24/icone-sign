package com.example.unifiedapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ValidationReportInterpreter {
    
    private static final Logger logger = LoggerFactory.getLogger(ValidationReportInterpreter.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Interpret ANCE validation report JSON and create human-readable summary
     */
    public ValidationSummary interpretValidationReport(String jsonReport) {
        try {
            JsonNode root = objectMapper.readTree(jsonReport);
            ValidationSummary summary = new ValidationSummary();
            
            // Extract basic information
            summary.setValidationDate(getTextValue(root, "validationTime"));
            summary.setDocumentName(getTextValue(root, "documentName"));
            
            // Analyze signatures
            JsonNode signatures = root.path("signatures");
            if (signatures.isArray()) {
                for (JsonNode signature : signatures) {
                    SignatureAnalysis sigAnalysis = analyzeSignature(signature);
                    summary.getSignatures().add(sigAnalysis);
                }
            }
            
            // Determine overall status
            summary.setOverallStatus(determineOverallStatus(summary.getSignatures()));
            
            return summary;
            
        } catch (Exception e) {
            logger.error("Failed to interpret validation report: {}", e.getMessage(), e);
            return createErrorSummary("Erreur lors de l'interprétation du rapport: " + e.getMessage());
        }
    }
    
    private SignatureAnalysis analyzeSignature(JsonNode signature) {
        SignatureAnalysis analysis = new SignatureAnalysis();
        
        // Basic signature info
        analysis.setSignatureId(getTextValue(signature, "id"));
        analysis.setSignatureType(getTextValue(signature, "type"));
        
        // Analyze conclusion
        JsonNode conclusion = signature.path("conclusion");
        analysis.setIndication(getTextValue(conclusion, "indication"));
        analysis.setSubIndication(getTextValue(conclusion, "subIndication"));
        
        // Process errors
        JsonNode errors = conclusion.path("errors");
        if (errors.isArray()) {
            for (JsonNode error : errors) {
                String errorCode = getTextValue(error, "nameId");
                String errorMessage = getTextValue(error, "value");
                analysis.getErrors().add(new ValidationIssue(errorCode, errorMessage, interpretErrorCode(errorCode)));
            }
        }
        
        // Process warnings
        JsonNode warnings = conclusion.path("warnings");
        if (warnings.isArray()) {
            for (JsonNode warning : warnings) {
                String warningCode = getTextValue(warning, "nameId");
                String warningMessage = getTextValue(warning, "value");
                analysis.getWarnings().add(new ValidationIssue(warningCode, warningMessage, interpretWarningCode(warningCode)));
            }
        }
        
        // Analyze certificate info
        analyzeCertificateInfo(signature, analysis);
        
        return analysis;
    }
    
    private void analyzeCertificateInfo(JsonNode signature, SignatureAnalysis analysis) {
        // Extract certificate information from the signature
        JsonNode signingCertificate = signature.path("signingCertificate");
        if (!signingCertificate.isMissingNode()) {
            analysis.setCertificateSubject(getTextValue(signingCertificate, "subjectDistinguishedName"));
            analysis.setCertificateIssuer(getTextValue(signingCertificate, "issuerDistinguishedName"));
            analysis.setCertificateSerialNumber(getTextValue(signingCertificate, "serialNumber"));
        }
    }
    
    private String interpretErrorCode(String errorCode) {
        Map<String, String> errorInterpretations = Map.of(
            "BBB_VCI_ISPA_ANS", "La politique de signature n'est pas disponible. Vérifiez que l'URL de la politique est accessible.",
            "BBB_XCV_CCCBB_SIG_ANS", "Le certificat de signature n'est pas valide ou a expiré.",
            "BBB_XCV_SUB_ANS", "Le certificat ne respecte pas les contraintes de la politique.",
            "BBB_SAV_ISQPSTP_ANS", "La signature ne respecte pas le format requis.",
            "BBB_CV_IRDOF_ANS", "Impossible de vérifier la révocation du certificat."
        );
        
        return errorInterpretations.getOrDefault(errorCode, "Erreur de validation non reconnue: " + errorCode);
    }
    
    private String interpretWarningCode(String warningCode) {
        Map<String, String> warningInterpretations = Map.of(
            "BBB_ICS_AIDNASNE_ANS", "L'attribut 'issuer-serial' est absent ou ne correspond pas. Cela peut affecter la vérification du certificat.",
            "BBB_SAV_DMICTSTMCMI_ANS", "L'horodatage de la signature pourrait ne pas être fiable.",
            "BBB_XCV_ICTIVRSC_ANS", "Impossible de vérifier complètement la chaîne de certificats.",
            "BBB_VCI_ISPK_ANS", "La clé publique de signature pourrait ne pas être fiable."
        );
        
        return warningInterpretations.getOrDefault(warningCode, "Avertissement non reconnu: " + warningCode);
    }
    
    private String determineOverallStatus(List<SignatureAnalysis> signatures) {
        if (signatures.isEmpty()) {
            return "AUCUNE_SIGNATURE";
        }
        
        boolean hasErrors = signatures.stream().anyMatch(s -> !s.getErrors().isEmpty());
        boolean hasWarnings = signatures.stream().anyMatch(s -> !s.getWarnings().isEmpty());
        
        if (hasErrors) {
            return "INVALIDE";
        } else if (hasWarnings) {
            return "VALIDE_AVEC_AVERTISSEMENTS";
        } else {
            return "VALIDE";
        }
    }
    
    private String getTextValue(JsonNode node, String fieldName) {
        JsonNode field = node.path(fieldName);
        return field.isMissingNode() ? "" : field.asText();
    }
    
    private ValidationSummary createErrorSummary(String errorMessage) {
        ValidationSummary summary = new ValidationSummary();
        summary.setOverallStatus("ERREUR");
        summary.setErrorMessage(errorMessage);
        return summary;
    }
    
    // Inner classes for structured data
    public static class ValidationSummary {
        private String validationDate;
        private String documentName;
        private String overallStatus;
        private String errorMessage;
        private List<SignatureAnalysis> signatures = new ArrayList<>();
        
        // Getters and setters
        public String getValidationDate() { return validationDate; }
        public void setValidationDate(String validationDate) { this.validationDate = validationDate; }
        
        public String getDocumentName() { return documentName; }
        public void setDocumentName(String documentName) { this.documentName = documentName; }
        
        public String getOverallStatus() { return overallStatus; }
        public void setOverallStatus(String overallStatus) { this.overallStatus = overallStatus; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public List<SignatureAnalysis> getSignatures() { return signatures; }
        public void setSignatures(List<SignatureAnalysis> signatures) { this.signatures = signatures; }
    }
    
    public static class SignatureAnalysis {
        private String signatureId;
        private String signatureType;
        private String indication;
        private String subIndication;
        private String certificateSubject;
        private String certificateIssuer;
        private String certificateSerialNumber;
        private List<ValidationIssue> errors = new ArrayList<>();
        private List<ValidationIssue> warnings = new ArrayList<>();
        
        // Getters and setters
        public String getSignatureId() { return signatureId; }
        public void setSignatureId(String signatureId) { this.signatureId = signatureId; }
        
        public String getSignatureType() { return signatureType; }
        public void setSignatureType(String signatureType) { this.signatureType = signatureType; }
        
        public String getIndication() { return indication; }
        public void setIndication(String indication) { this.indication = indication; }
        
        public String getSubIndication() { return subIndication; }
        public void setSubIndication(String subIndication) { this.subIndication = subIndication; }
        
        public String getCertificateSubject() { return certificateSubject; }
        public void setCertificateSubject(String certificateSubject) { this.certificateSubject = certificateSubject; }
        
        public String getCertificateIssuer() { return certificateIssuer; }
        public void setCertificateIssuer(String certificateIssuer) { this.certificateIssuer = certificateIssuer; }
        
        public String getCertificateSerialNumber() { return certificateSerialNumber; }
        public void setCertificateSerialNumber(String certificateSerialNumber) { this.certificateSerialNumber = certificateSerialNumber; }
        
        public List<ValidationIssue> getErrors() { return errors; }
        public void setErrors(List<ValidationIssue> errors) { this.errors = errors; }
        
        public List<ValidationIssue> getWarnings() { return warnings; }
        public void setWarnings(List<ValidationIssue> warnings) { this.warnings = warnings; }
    }
    
    public static class ValidationIssue {
        private String code;
        private String message;
        private String interpretation;
        
        public ValidationIssue(String code, String message, String interpretation) {
            this.code = code;
            this.message = message;
            this.interpretation = interpretation;
        }
        
        // Getters and setters
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getInterpretation() { return interpretation; }
        public void setInterpretation(String interpretation) { this.interpretation = interpretation; }
    }
}
