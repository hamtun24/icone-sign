package com.example.unifiedapi.service;
import com.example.unifiedapi.entity.User;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.xml.security.c14n.Canonicalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.example.unifiedapi.util.SignatureUtil;

@Service
public class XmlSignatureService {
    private static final Logger logger = LoggerFactory.getLogger(XmlSignatureService.class);
   
    private final AnceSealClient anceSealClient;
    private final OperationLogService operationLogService;
    private final CertificatePathService certificatePathService;
    private final ProgressTrackingService progressTrackingService;
    private final UserCredentialsService userCredentialsService;

    // Executor for async processing
    private final ExecutorService executorService = Executors.newFixedThreadPool(
        Math.max(2, Runtime.getRuntime().availableProcessors())
    );

    @Autowired
    public XmlSignatureService(AnceSealClient anceSealClient, OperationLogService operationLogService,
                              CertificatePathService certificatePathService, ProgressTrackingService progressTrackingService ,UserCredentialsService userCredentialsService) {
        this.anceSealClient = anceSealClient;
        this.operationLogService = operationLogService;
        this.certificatePathService = certificatePathService;
        this.progressTrackingService = progressTrackingService;
        this.userCredentialsService = userCredentialsService;
    }
    
    public String signXml(MultipartFile file, User user) throws Exception {
        org.apache.xml.security.Init.init();
        String originalFilename = file.getOriginalFilename();
        
        if (file.isEmpty() || originalFilename == null || !originalFilename.endsWith(".xml")) {
            throw new IllegalArgumentException("Invalid file. Only XML files are allowed.");
        }
        if (file.getSize() > 16 * 1024 * 1024) {
            throw new IllegalArgumentException("File too large. Max 16MB allowed.");
        }

        boolean success = false;
        String result = null;
        
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(file.getBytes()));

            List<X509Certificate> certChain = loadCertificateChain();
            X509Certificate cert = certChain.get(0);
            
            byte[] certEncoded = cert.getEncoded();

            // Calculate document digest for Reference[1] BEFORE adding signature
            String base64DocDigest = computeDocumentReferenceDigest(doc);

            // Generate UUID-based signature ID like working signature
            String signatureId = "id-" + java.util.UUID.randomUUID().toString().replace("-", "");

            // Create SignedProperties and build signature structure
            logger.debug("Creating SignedProperties...");
            Element signedProps = SignatureUtil.createSignedProperties(doc, cert, certEncoded, signatureId);
            if (signedProps == null) {
                throw new RuntimeException("Failed to create SignedProperties");
            }

            logger.debug("Creating SignedInfo...");
            Element signedInfo = SignatureUtil.createSignedInfo(doc, base64DocDigest, signatureId);
            if (signedInfo == null) {
                throw new RuntimeException("Failed to create SignedInfo");
            }

            // Build temporary complete signature structure
            logger.debug("Building complete signature structure...");
            Element tempSignatureElem = SignatureUtil.buildCompleteSignature(doc, signedInfo, "", certChain, signedProps, signatureId);
            if (tempSignatureElem == null) {
                throw new RuntimeException("Failed to build complete signature");
            }

            // Attach to document to get the right namespace context
            logger.debug("Attaching signature to document...");
            doc.getDocumentElement().appendChild(tempSignatureElem);

            // Now calculate SignedProperties digest in its final context
            NodeList signedPropsList = doc.getElementsByTagNameNS(SignatureUtil.XADES_NS, "SignedProperties");
            if (signedPropsList.getLength() == 0) {
                throw new RuntimeException("SignedProperties element not found in document");
            }

            Element signedPropsInContext = (Element) signedPropsList.item(0);
            if (signedPropsInContext == null) {
                throw new RuntimeException("SignedProperties element is null");
            }

            signedPropsInContext.setIdAttribute("Id", true);

            // Calculate digest using the element in its final document context
            String signedPropsDigest = calculateSignedPropertiesDigestInContext(signedPropsInContext);
            logger.debug("Calculated SignedProperties digest: {}", signedPropsDigest);

            // Update SignedInfo with the correct digest for Reference[2]
            SignatureUtil.updateSignedInfoWithPropertiesDigest(signedInfo, signedPropsDigest, signatureId);

            // CRITICAL: Now canonicalize the SignedInfo from its FINAL document context
            // Find the SignedInfo element in the document (not the standalone element)
            NodeList signedInfoList = doc.getElementsByTagNameNS(SignatureUtil.DS_NS, "SignedInfo");
            if (signedInfoList.getLength() == 0) {
                throw new RuntimeException("SignedInfo element not found in document");
            }
            Element signedInfoInContext = (Element) signedInfoList.item(0);

            // Canonicalize the SignedInfo from its final document context
            Canonicalizer canon = Canonicalizer.getInstance("http://www.w3.org/2001/10/xml-exc-c14n#");
            ByteArrayOutputStream baosSignedInfo = new ByteArrayOutputStream();
            canon.canonicalizeSubtree(signedInfoInContext, baosSignedInfo);
            byte[] canonicalSignedInfo = baosSignedInfo.toByteArray();

            // Debug: Log the canonicalized SignedInfo
            logger.debug("Canonicalized SignedInfo from document context: {}", new String(canonicalSignedInfo, StandardCharsets.UTF_8));

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String base64SignedInfoDigest = Base64.getEncoder().encodeToString(digest.digest(canonicalSignedInfo));

            // Get signature from ANCE (requires User for dynamic alias)
            logger.debug("Sending hash to ANCE SEAL for signing: {}", base64SignedInfoDigest);
            String signatureResponse = anceSealClient.signHash(base64SignedInfoDigest, user);
            logger.debug("Received signature response from ANCE SEAL: {}", signatureResponse);

            String signatureValue = parseSignatureResponse(signatureResponse);
            logger.debug("Parsed signature value: {}", signatureValue.substring(0, Math.min(50, signatureValue.length())) + "...");

            // Validate that the signature value is proper base64
            try {
                Base64.getDecoder().decode(signatureValue);
                logger.debug("Signature value is valid base64");
            } catch (IllegalArgumentException e) {
                logger.error("Signature value is not valid base64: {}", e.getMessage());
                throw new RuntimeException("Invalid signature value received from ANCE SEAL", e);
            }

            // Update signature value in the document context (not just the temp element)
            NodeList signatureValueList = doc.getElementsByTagNameNS(SignatureUtil.DS_NS, "SignatureValue");
            if (signatureValueList.getLength() == 0) {
                throw new RuntimeException("SignatureValue element not found in document");
            }
            Element signatureValueElem = (Element) signatureValueList.item(0);
            signatureValueElem.setTextContent(signatureValue);

            // Verify the signature value was set correctly
            logger.debug("Signature value set in document context: {}", signatureValue.substring(0, Math.min(50, signatureValue.length())) + "...");

            // VERIFICATION: Ensure the SignedInfo in the final document matches what we signed
            NodeList finalSignedInfoList = doc.getElementsByTagNameNS(SignatureUtil.DS_NS, "SignedInfo");
            Element finalSignedInfo = (Element) finalSignedInfoList.item(0);

            ByteArrayOutputStream verifyBaos = new ByteArrayOutputStream();
            canon.canonicalizeSubtree(finalSignedInfo, verifyBaos);
            byte[] finalCanonicalSignedInfo = verifyBaos.toByteArray();

            // Compare the two canonicalizations
            boolean signedInfoMatches = java.util.Arrays.equals(canonicalSignedInfo, finalCanonicalSignedInfo);
            logger.info("SignedInfo canonicalization verification: {}", signedInfoMatches ? "MATCH" : "MISMATCH");

            if (!signedInfoMatches) {
                logger.warn("SignedInfo mismatch detected - this may cause signature validation failures");
                logger.debug("Original SignedInfo length: {}, Final SignedInfo length: {}",
                    canonicalSignedInfo.length, finalCanonicalSignedInfo.length);
            }

            // Output the signed document
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty("omit-xml-declaration", "no");
            transformer.setOutputProperty("indent", "no");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            result = writer.toString();
            success = true;
            
            logger.info("XML signing completed successfully with XAdES-compliant format");

            // Log certificate details for verification
            logger.info("Certificate details - Subject: {}, Serial: {}, Issuer: {}", 
                cert.getSubjectX500Principal().getName(),
                cert.getSerialNumber().toString(16).toUpperCase(),
                cert.getIssuerX500Principal().getName());

        } catch (Exception e) {
            logger.error("XML signing failed", e);
            throw e;
        } finally {
            // Log operation
            operationLogService.logOperation(
                "ANCE_SIGN", 
                success ? "SUCCESS" : "FAILURE", 
                file.getSize(), 
                originalFilename
            );
        }

        return result;
    }

    private String calculateSignedPropertiesDigestInContext(Element signedPropsElem) throws Exception {
        Canonicalizer canon = Canonicalizer.getInstance("http://www.w3.org/2001/10/xml-exc-c14n#");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        canon.canonicalizeSubtree(signedPropsElem, baos);
        byte[] canonicalXml = baos.toByteArray();

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String result = Base64.getEncoder().encodeToString(digest.digest(canonicalXml));
        return result;
    }

    public byte[] validateXmlSignature(MultipartFile file) throws Exception {
        return validateXmlSignature(file, null);
    }

    public byte[] validateXmlSignature(MultipartFile file, String ttnInvoiceId) throws Exception {
        org.apache.xml.security.Init.init();
        String xmlContent = new String(file.getBytes(), StandardCharsets.UTF_8);
        byte[] jsonData = null;

        try {
            // Remote validation with ANCE
            String responsePath = anceSealClient.validateSignatureWithResponse(xmlContent, ttnInvoiceId);

            // Read the validation report
            java.nio.file.Path reportPath = java.nio.file.Paths.get(responsePath);
            jsonData = java.nio.file.Files.readAllBytes(reportPath);
            
            logger.info("XML signature validation completed");
            
            // Log successful operation
            operationLogService.logOperation(
                "ANCE_VALIDATE", 
                "SUCCESS", 
                file.getSize(), 
                file.getOriginalFilename()
            );
            
        } catch (Exception e) {
            logger.error("XML signature validation failed", e);
            
            // Log failed operation
            operationLogService.logOperation(
                "ANCE_VALIDATE", 
                "FAILURE", 
                file.getSize(), 
                file.getOriginalFilename()
            );
            
            throw e;
        }

        return jsonData;
    }

    public CompletableFuture<List<SigningResult>> signMultipleFilesAsync(List<MultipartFile> files, User user) {
        return CompletableFuture.supplyAsync(() -> {
            List<SigningResult> results = new ArrayList<>();
            for (MultipartFile file : files) {
                SigningResult result = new SigningResult();
                result.setFilename(file.getOriginalFilename());
                try {
                    String signedXml = signXml(file, user);
                    result.setSuccess(true);
                    result.setSignedXml(signedXml);
                } catch (Exception e) {
                    result.setSuccess(false);
                    result.setError(e.getMessage());
                }
                results.add(result);
            }
            return results;
        }, executorService);
    }

    // Removed duplicate/incorrect overload

    public String signXmlWithCredentials(MultipartFile file, String certificatePath, String pin,
                                       String sessionId, String filename ,User user) throws Exception {
        org.apache.xml.security.Init.init();
        String originalFilename = file.getOriginalFilename();

        // Debug logging for file validation
        logger.debug("File validation - isEmpty: {}, filename: '{}', size: {}",
                    file.isEmpty(), originalFilename, file.getSize());

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty.");
        }
        if (originalFilename == null) {
            throw new IllegalArgumentException("Filename is null.");
        }
        if (!originalFilename.toLowerCase().endsWith(".xml")) {
            throw new IllegalArgumentException("Invalid file extension. Only XML files are allowed. Got: " + originalFilename);
        }
        if (file.getSize() > 16 * 1024 * 1024) {
            throw new IllegalArgumentException("File too large. Max 16MB allowed.");
        }

        // Resolve certificate path using the certificate path service
        String resolvedCertificatePath = certificatePathService.resolveCertificatePath(certificatePath);
        logger.info("Starting XML signing for file: {} with resolved certificate: {}", originalFilename, resolvedCertificatePath);

        // Update progress: Starting signature process
        if (sessionId != null && filename != null) {
            progressTrackingService.updateFileProgress(sessionId, filename, "PROCESSING", "SIGN", 15, null);
        }

        boolean success = false;
        String result = null;

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(file.getBytes()));

            // Update progress: Document parsed
            if (sessionId != null && filename != null) {
                progressTrackingService.updateFileProgress(sessionId, filename, "PROCESSING", "SIGN", 25, null);
            }

            List<X509Certificate> certChain = loadCertificateChain(resolvedCertificatePath);
            X509Certificate cert = certChain.get(0);

            // Update progress: Certificate loaded
            if (sessionId != null && filename != null) {
                progressTrackingService.updateFileProgress(sessionId, filename, "PROCESSING", "SIGN", 35, null);
            }

            byte[] certEncoded = cert.getEncoded();

            // Calculate document digest for Reference[1] BEFORE adding signature
            String base64DocDigest = computeDocumentReferenceDigest(doc);

            // Update progress: Document digest calculated
            if (sessionId != null && filename != null) {
                progressTrackingService.updateFileProgress(sessionId, filename, "PROCESSING", "SIGN", 45, null);
            }

            // Generate UUID-based signature ID like working signature
            String signatureId = "id-" + java.util.UUID.randomUUID().toString().replace("-", "");

            // Create SignedProperties and build signature structure
            Element signedProps = SignatureUtil.createSignedProperties(doc, cert, certEncoded, signatureId);
            Element signedInfo = SignatureUtil.createSignedInfo(doc, base64DocDigest, signatureId);

            // Update progress: Signature structure created
            if (sessionId != null && filename != null) {
                progressTrackingService.updateFileProgress(sessionId, filename, "PROCESSING", "SIGN", 55, null);
            }

            // Build temporary complete signature structure
            Element tempSignatureElem = SignatureUtil.buildCompleteSignature(doc, signedInfo, "", certChain, signedProps, signatureId);

            // Attach to document to get the right namespace context
            doc.getDocumentElement().appendChild(tempSignatureElem);

            // Calculate SignedProperties digest and update SignedInfo
            // The signedProps IS the SignedProperties element, not a container
            String signedPropsDigest = SignatureUtil.calculateSignedPropertiesDigest(signedProps);
            String alias = userCredentialsService.getAnceSealAlias(user);
            if (sessionId != null && filename != null) {
                progressTrackingService.updateFileProgress(sessionId, filename, "PROCESSING", "SIGN", 65, null);
            }

            // Canonicalize SignedInfo and create signature
            Canonicalizer canon = Canonicalizer.getInstance("http://www.w3.org/2001/10/xml-exc-c14n#");
            ByteArrayOutputStream baosSignedInfo = new ByteArrayOutputStream();
            canon.canonicalizeSubtree(signedInfo, baosSignedInfo);
            byte[] canonicalSignedInfo = baosSignedInfo.toByteArray();

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String base64SignedInfoDigest = Base64.getEncoder().encodeToString(digest.digest(canonicalSignedInfo));

            // Update progress: Preparing to sign with ANCE
            if (sessionId != null && filename != null) {
                progressTrackingService.updateFileProgress(sessionId, filename, "PROCESSING", "SIGN", 75, null);
            }

            // Get signature from ANCE using provided pin and dynamic signUrl
            String signUrl = anceSealClient.buildSignUrl(alias);
            logger.info("Using ANCE SEAL signUrl: {}", signUrl);
            String signatureResponse = anceSealClient.signHashWithPin(base64SignedInfoDigest, pin, signUrl);
            String signatureValue = parseSignatureResponse(signatureResponse);

            // Update progress: Signature received from ANCE
            if (sessionId != null && filename != null) {
                progressTrackingService.updateFileProgress(sessionId, filename, "PROCESSING", "SIGN", 85, null);
            }

            // Update signature value in the document
            Element signatureValueElem = (Element) tempSignatureElem.getElementsByTagNameNS(SignatureUtil.DS_NS, "SignatureValue").item(0);
            signatureValueElem.setTextContent(signatureValue);

            // Output the signed document
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty("omit-xml-declaration", "no");
            transformer.setOutputProperty("indent", "no");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            result = writer.toString();
            success = true;

            // Update progress: Signature completed
            if (sessionId != null && filename != null) {
                progressTrackingService.updateFileProgress(sessionId, filename, "PROCESSING", "SIGN", 95, null);
            }

            logger.info("XML signing completed successfully with XAdES-compliant format");

            // Log certificate details for verification
            logger.info("Certificate details - Subject: {}, Serial: {}, Issuer: {}",
                cert.getSubjectX500Principal().getName(),
                cert.getSerialNumber().toString(16).toUpperCase(),
                cert.getIssuerX500Principal().getName());

        } catch (Exception e) {
            logger.error("XML signing failed", e);
            throw e;
        } finally {
            // Log operation
            operationLogService.logOperation(
                "ANCE_SIGN",
                success ? "SUCCESS" : "FAILURE",
                file.getSize(),
                originalFilename
            );
        }

        return result;
    }

    private List<X509Certificate> loadCertificateChain(String certificatePath) throws Exception {
        List<X509Certificate> certChain = new ArrayList<>();

        try {
            // Load certificate from file path
            java.nio.file.Path certPath = java.nio.file.Paths.get(certificatePath);
            if (!java.nio.file.Files.exists(certPath)) {
                throw new IllegalArgumentException("Certificate file not found: " + certificatePath);
            }

            byte[] certBytes = java.nio.file.Files.readAllBytes(certPath);

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream certStream = new ByteArrayInputStream(certBytes);
            X509Certificate cert = (X509Certificate) cf.generateCertificate(certStream);

            certChain.add(cert);

            logger.info("Loaded certificate from: {}", certificatePath);
            logger.debug("Certificate subject: {}", cert.getSubjectX500Principal().getName());

        } catch (Exception e) {
            logger.error("Failed to load certificate from {}: {}", certificatePath, e.getMessage(), e);
            throw new RuntimeException("Failed to load certificate: " + e.getMessage(), e);
        }

        return certChain;
    }

    private List<X509Certificate> loadCertificateChain() throws Exception {
        // Fallback method - should not be used in the new workflow
        throw new UnsupportedOperationException("Use loadCertificateChain(String certificatePath) instead");
    }

    private String computeDocumentReferenceDigest(Document doc) throws Exception {
        // Apply enveloped signature transform and canonicalization
        Canonicalizer canon = Canonicalizer.getInstance("http://www.w3.org/2001/10/xml-exc-c14n#");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        canon.canonicalizeSubtree(doc.getDocumentElement(), baos);

        // Debug: Log the canonicalized document (first 500 chars)
        String canonicalXml = new String(baos.toByteArray(), StandardCharsets.UTF_8);
        logger.debug("Canonicalized document (first 500 chars): {}",
            canonicalXml.length() > 500 ? canonicalXml.substring(0, 500) + "..." : canonicalXml);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(baos.toByteArray());
        String base64Hash = Base64.getEncoder().encodeToString(hash);

        logger.debug("Document digest: {}", base64Hash);
        return base64Hash;
    }

    private String parseSignatureResponse(String response) {
        logger.debug("Parsing signature response: {}", response);

        // Handle ANCE SEAL JSON format: {"algorithm":"RSA_SHA256","value":"ACTUAL_SIGNATURE_VALUE"}
        Pattern anceJsonPattern = Pattern.compile("\"value\"\\s*:\\s*\"([^\"]+)\"");
        Matcher anceJsonMatcher = anceJsonPattern.matcher(response);

        if (anceJsonMatcher.find()) {
            String signatureValue = anceJsonMatcher.group(1);
            logger.debug("Extracted signature value from ANCE JSON format: {}", signatureValue.substring(0, Math.min(50, signatureValue.length())) + "...");
            return signatureValue;
        }

        // Handle ANCE SEAL format: {algorithm:RSA_SHA256,value:ACTUAL_SIGNATURE_VALUE}
        Pattern anceFormatPattern = Pattern.compile("\\{algorithm:[^,]+,value:([^}]+)\\}");
        Matcher anceMatcher = anceFormatPattern.matcher(response);

        if (anceMatcher.find()) {
            String signatureValue = anceMatcher.group(1);
            logger.debug("Extracted signature value from ANCE format: {}", signatureValue.substring(0, Math.min(50, signatureValue.length())) + "...");
            return signatureValue;
        }

        // Handle JSON format: {"signature": "ACTUAL_SIGNATURE_VALUE"}
        Pattern jsonPattern = Pattern.compile("\"signature\"\\s*:\\s*\"([^\"]+)\"");
        Matcher jsonMatcher = jsonPattern.matcher(response);

        if (jsonMatcher.find()) {
            String signatureValue = jsonMatcher.group(1);
            logger.debug("Extracted signature value from JSON format: {}", signatureValue.substring(0, Math.min(50, signatureValue.length())) + "...");
            return signatureValue;
        }

        // Handle plain base64 signature (fallback)
        String cleanResponse = response.replaceAll("[\"\n\r]", "").trim();

        // Check if it looks like a base64 signature (only contains base64 characters)
        if (cleanResponse.matches("^[A-Za-z0-9+/]+=*$") && cleanResponse.length() > 100) {
            logger.debug("Using response as plain base64 signature: {}", cleanResponse.substring(0, Math.min(50, cleanResponse.length())) + "...");
            return cleanResponse;
        }

        // If we can't parse it, log the issue and return as-is
        logger.warn("Could not parse signature response format. Response: {}", response);
        return cleanResponse;
    }

    // Inner class for batch signing results
    public static class SigningResult {
        private String filename;
        private boolean success;
        private String signedXml;
        private String error;

        // Getters and setters
        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getSignedXml() { return signedXml; }
        public void setSignedXml(String signedXml) { this.signedXml = signedXml; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}
