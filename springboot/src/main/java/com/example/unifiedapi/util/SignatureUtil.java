package com.example.unifiedapi.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.apache.xml.security.c14n.Canonicalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;

public class SignatureUtil {
    private static final Logger logger = LoggerFactory.getLogger(SignatureUtil.class);

    public static final String DS_NS = "http://www.w3.org/2000/09/xmldsig#";
    public static final String XADES_NS = "http://uri.etsi.org/01903/v1.3.2#";
    
    // Updated policy URL
    private static final String POLICY_URL = "https://www.tradenet.com.tn/Politique_Signature_Electronique_Tunisie_TradeNet.pdf";
    
    // Policy hash from working signature that passes TTN validation
    private static final String POLICY_HASH = "m+58sM7PAVahMytFBzze1uLe8013XGecAFPSqqOEspU=";

    // Cache for policy hash to avoid repeated downloads
    private static volatile String cachedPolicyHash = null;
    private static volatile long lastPolicyCheck = 0;
    private static final long POLICY_CACHE_DURATION = 24 * 60 * 60 * 1000; // 24 hours

    public static Element createSignedInfo(Document doc, String documentDigest, String signatureId) {
        Element signedInfo = doc.createElementNS(DS_NS, "ds:SignedInfo");

        Element canonMethod = doc.createElementNS(DS_NS, "ds:CanonicalizationMethod");
        canonMethod.setAttribute("Algorithm", "http://www.w3.org/2001/10/xml-exc-c14n#");

        Element sigMethod = doc.createElementNS(DS_NS, "ds:SignatureMethod");
        sigMethod.setAttribute("Algorithm", "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256");

        Element reference1 = doc.createElementNS(DS_NS, "ds:Reference");
        reference1.setAttribute("Id", "r-id-frs");  // Match working signature pattern
        reference1.setAttribute("Type", "text/xml");   // Add missing Type attribute
        reference1.setAttribute("URI", "");

        Element transforms1 = doc.createElementNS(DS_NS, "ds:Transforms");

        // Only include the signature exclusion XPath (match working signature)
        Element transformXPath1 = doc.createElementNS(DS_NS, "ds:Transform");
        transformXPath1.setAttribute("Algorithm", "http://www.w3.org/TR/1999/REC-xpath-19991116");
        Element xpath1 = doc.createElementNS(DS_NS, "ds:XPath");
        xpath1.setTextContent("not(ancestor-or-self::ds:Signature)");
        transformXPath1.appendChild(xpath1);

        // Remove the RefTtnVal XPath transform - it's not in the working signature
        // Element transformXPath2 = doc.createElementNS(DS_NS, "ds:Transform");
        // transformXPath2.setAttribute("Algorithm", "http://www.w3.org/TR/1999/REC-xpath-19991116");
        // Element xpath2 = doc.createElementNS(DS_NS, "ds:XPath");
        // xpath2.setTextContent("not(ancestor-or-self::RefTtnVal)");
        // transformXPath2.appendChild(xpath2);

        Element transformCanon = doc.createElementNS(DS_NS, "ds:Transform");
        transformCanon.setAttribute("Algorithm", "http://www.w3.org/2001/10/xml-exc-c14n#");

        transforms1.appendChild(transformXPath1);
        // transforms1.appendChild(transformXPath2);  // Remove this - not in working signature
        transforms1.appendChild(transformCanon);
        reference1.appendChild(transforms1);

        Element digestMethod1 = doc.createElementNS(DS_NS, "ds:DigestMethod");
        digestMethod1.setAttribute("Algorithm", "http://www.w3.org/2001/04/xmlenc#sha256");
        Element digestValue1 = doc.createElementNS(DS_NS, "ds:DigestValue");
        digestValue1.setTextContent(documentDigest);
        reference1.appendChild(digestMethod1);
        reference1.appendChild(digestValue1);

        Element reference2 = doc.createElementNS(DS_NS, "ds:Reference");
        reference2.setAttribute("Type", "http://uri.etsi.org/01903#SignedProperties");
        //reference2.setAttribute("URI", "#xades-" + signatureId);
        reference2.setAttribute("URI", "#xades-SigFrs");

        Element transforms2 = doc.createElementNS(DS_NS, "ds:Transforms");
        Element transform2 = doc.createElementNS(DS_NS, "ds:Transform");
        transform2.setAttribute("Algorithm", "http://www.w3.org/2001/10/xml-exc-c14n#");
        transforms2.appendChild(transform2);
        reference2.appendChild(transforms2);

        Element digestMethod2 = doc.createElementNS(DS_NS, "ds:DigestMethod");
        digestMethod2.setAttribute("Algorithm", "http://www.w3.org/2001/04/xmlenc#sha256");
        Element digestValue2 = doc.createElementNS(DS_NS, "ds:DigestValue");
        reference2.appendChild(digestMethod2);
        reference2.appendChild(digestValue2);

        signedInfo.appendChild(canonMethod);
        signedInfo.appendChild(sigMethod);
        signedInfo.appendChild(reference1);
        signedInfo.appendChild(reference2);

        return signedInfo;
    }

    public static void updateSignedInfoWithPropertiesDigest(Element signedInfo, String propertiesDigest, String signatureId) {
        var references = signedInfo.getElementsByTagNameNS(DS_NS, "Reference");
        if (references.getLength() >= 2) {
            Element reference2 = (Element) references.item(1);
            var digestValues = reference2.getElementsByTagNameNS(DS_NS, "DigestValue");
            if (digestValues.getLength() > 0) {
                digestValues.item(0).setTextContent(propertiesDigest);
            }
        }
    }

    public static String calculateSignedPropertiesDigest(Element signedProps) throws Exception {
        // Create a namespace-aware document builder
        javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document tmpDoc = dbf.newDocumentBuilder().newDocument();

        // Import the SignedProperties element with all its namespace context
        Element importedSignedProps = (Element) tmpDoc.importNode(signedProps, true);
        tmpDoc.appendChild(importedSignedProps);

        // Ensure proper namespace declarations are present
        if (!importedSignedProps.hasAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xades")) {
            importedSignedProps.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xades", XADES_NS);
        }
        if (!importedSignedProps.hasAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:ds")) {
            importedSignedProps.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:ds", DS_NS);
        }

        Canonicalizer canon = Canonicalizer.getInstance("http://www.w3.org/2001/10/xml-exc-c14n#");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        canon.canonicalizeSubtree(importedSignedProps, baos);

        // Debug: Log the canonicalized SignedProperties
        String canonicalXml = new String(baos.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
        logger.debug("Canonicalized SignedProperties: {}", canonicalXml);

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(baos.toByteArray());
        String base64Digest = Base64.getEncoder().encodeToString(digest);

        logger.debug("SignedProperties digest: {}", base64Digest);
        return base64Digest;
    }

    public static Element buildCompleteXAdESSignature(Document doc, String documentDigest, String signatureValue,
                                                      X509Certificate signingCert, byte[] certEncoded,
                                                      List<X509Certificate> certChain) throws Exception {
        // Generate UUID-based ID like working signature
        String signatureId = "id-" + UUID.randomUUID().toString().replace("-", "");

        Element signedProps = createSignedProperties(doc, signingCert, certEncoded, signatureId);
        String propsDigest = calculateSignedPropertiesDigest(signedProps);
        Element signedInfo = createSignedInfo(doc, documentDigest, signatureId);
        updateSignedInfoWithPropertiesDigest(signedInfo, propsDigest, signatureId);
        return buildCompleteSignature(doc, signedInfo, signatureValue, certChain, signedProps, signatureId);
    }

    /**
     * Creates a proper ASN.1 DER encoded IssuerSerial structure
     * This method creates the binary representation of IssuerAndSerialNumber structure
     * according to RFC 5652 and XAdES specification
     */
    public static String createIssuerSerialV2(X509Certificate cert) throws Exception {
        try {
            // Validate certificate
            if (cert == null) {
                throw new IllegalArgumentException("Certificate cannot be null");
            }

            BigInteger serial = cert.getSerialNumber();
            if (serial == null) {
                throw new IllegalArgumentException("Certificate serial number cannot be null");
            }

            logger.debug("Creating IssuerSerialV2 for certificate serial: {}", serial.toString(16));

            // Create X500Name from the certificate issuer
            X500Name issuer = new X500Name(cert.getIssuerX500Principal().getName());
            logger.debug("Certificate issuer: {}", issuer.toString());

            // Wrap issuer in GeneralNames as required by XAdES spec
            // IssuerSerial ::= SEQUENCE {
            //    issuer       GeneralNames,          -- issuer wrapped in GeneralNames
            //    serialNumber CertificateSerialNumber
            // }
            GeneralName generalName = new GeneralName(GeneralName.directoryName, issuer);
            GeneralNames generalNames = new GeneralNames(generalName);

            // Create the IssuerSerial SEQUENCE using BouncyCastle ASN.1 API
            ASN1EncodableVector vector = new ASN1EncodableVector();
            vector.add(generalNames);                    // issuer wrapped in GeneralNames
            vector.add(new ASN1Integer(serial));         // serial number as ASN.1 INTEGER

            DERSequence issuerSerialSequence = new DERSequence(vector);

            // Encode to DER
            byte[] issuerSerialDER = issuerSerialSequence.getEncoded(ASN1Encoding.DER);

            // Validate the generated structure
            try {
                ASN1InputStream asn1Stream = new ASN1InputStream(issuerSerialDER);
                ASN1Primitive parsed = asn1Stream.readObject();
                asn1Stream.close();

                if (!(parsed instanceof ASN1Sequence)) {
                    throw new IllegalStateException("Generated IssuerSerial is not a valid ASN.1 SEQUENCE");
                }

                ASN1Sequence sequence = (ASN1Sequence) parsed;
                if (sequence.size() != 2) {
                    throw new IllegalStateException("IssuerSerial SEQUENCE must have exactly 2 elements, got: " + sequence.size());
                }

                // Verify first element is GeneralNames (SEQUENCE)
                ASN1Primitive firstElement = sequence.getObjectAt(0).toASN1Primitive();
                if (!(firstElement instanceof ASN1Sequence)) {
                    throw new IllegalStateException("First element of IssuerSerial must be GeneralNames (SEQUENCE)");
                }

                // Verify second element is INTEGER
                ASN1Primitive secondElement = sequence.getObjectAt(1).toASN1Primitive();
                if (!(secondElement instanceof ASN1Integer)) {
                    throw new IllegalStateException("Second element of IssuerSerial must be ASN1Integer");
                }

                logger.debug("✅ IssuerSerialV2 structure validation successful - matches XAdES spec");

            } catch (Exception validationError) {
                logger.error("❌ Generated IssuerSerial validation failed: {}", validationError.getMessage());
                logger.error("Generated DER hex: {}", bytesToHex(issuerSerialDER));
                throw new IllegalStateException("Generated ASN.1 DER is not valid: " + validationError.getMessage(), validationError);
            }

            String base64Result = Base64.getEncoder().encodeToString(issuerSerialDER);
            logger.debug("✅ IssuerSerialV2 created successfully: {} bytes, Base64: {}",
                        issuerSerialDER.length, base64Result.substring(0, Math.min(50, base64Result.length())) + "...");

            return base64Result;

        } catch (Exception e) {
            logger.error("Failed to create IssuerSerialV2", e);
            throw new Exception("Could not create IssuerSerialV2: " + e.getMessage(), e);
        }
    }
    

    

    
    /**
     * Convert bytes to hex string for logging
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }

    public static Element createSignedProperties(Document doc, X509Certificate cert, byte[] certEncoded, String signatureId) throws Exception {
        Element signedProps = doc.createElementNS(XADES_NS, "xades:SignedProperties");
        signedProps.setAttribute("Id", "xades-" + signatureId);
        signedProps.setIdAttribute("Id", true);

        Element signedSigProps = doc.createElementNS(XADES_NS, "xades:SignedSignatureProperties");

        Element signingTime = doc.createElementNS(XADES_NS, "xades:SigningTime");
        signingTime.setTextContent(OffsetDateTime.now().withNano(0).withOffsetSameInstant(ZoneOffset.UTC).toString());
        signedSigProps.appendChild(signingTime);

        Element signingCertV2 = doc.createElementNS(XADES_NS, "xades:SigningCertificateV2");
        Element certElem = doc.createElementNS(XADES_NS, "xades:Cert");

        Element certDigest = doc.createElementNS(XADES_NS, "xades:CertDigest");
        Element digestMethod = doc.createElementNS(DS_NS, "ds:DigestMethod");
        // Changed to SHA-512 for consistency
        digestMethod.setAttribute("Algorithm", "http://www.w3.org/2001/04/xmlenc#sha512");

        Element digestValue = doc.createElementNS(DS_NS, "ds:DigestValue");
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        byte[] certHash = md.digest(certEncoded);
        digestValue.setTextContent(Base64.getEncoder().encodeToString(certHash));

        certDigest.appendChild(digestMethod);
        certDigest.appendChild(digestValue);

        Element issuerSerialV2 = doc.createElementNS(XADES_NS, "xades:IssuerSerialV2");
        // Use the proper ASN.1 DER encoding
        String base64IssuerSerial = createIssuerSerialV2(cert);
        issuerSerialV2.setTextContent(base64IssuerSerial);
      
        certElem.appendChild(certDigest);
        certElem.appendChild(issuerSerialV2);
        signingCertV2.appendChild(certElem);
        signedSigProps.appendChild(signingCertV2);

        Element sigPolicyId = doc.createElementNS(XADES_NS, "xades:SignaturePolicyIdentifier");
        Element sigPolicy = doc.createElementNS(XADES_NS, "xades:SignaturePolicyId");
        Element sigId = doc.createElementNS(XADES_NS, "xades:SigPolicyId");

        Element identifier = doc.createElementNS(XADES_NS, "xades:Identifier");
        identifier.setAttribute("Qualifier", "OIDAsURN");
        identifier.setTextContent("urn:2.16.788.1.2.1.3");

        Element description = doc.createElementNS(XADES_NS, "xades:Description");
        description.setTextContent("Politique de Signature Electronique de Tunisie TradeNet");

        sigId.appendChild(identifier);
        sigId.appendChild(description);
        sigPolicy.appendChild(sigId);

        Element policyHash = doc.createElementNS(XADES_NS, "xades:SigPolicyHash");
        Element hashMethod = doc.createElementNS(DS_NS, "ds:DigestMethod");
        hashMethod.setAttribute("Algorithm", "http://www.w3.org/2001/04/xmlenc#sha256");

        Element hashValue = doc.createElementNS(DS_NS, "ds:DigestValue");
        // Calculate the actual policy hash from the PDF document
        String calculatedPolicyHash = calculatePolicyHash();
        hashValue.setTextContent(calculatedPolicyHash);
        policyHash.appendChild(hashMethod);
        policyHash.appendChild(hashValue);
        sigPolicy.appendChild(policyHash);

        Element sigPolicyQualifiers = doc.createElementNS(XADES_NS, "xades:SigPolicyQualifiers");
        Element sigPolicyQualifier = doc.createElementNS(XADES_NS, "xades:SigPolicyQualifier");
        Element spUri = doc.createElementNS(XADES_NS, "xades:SPURI");
        // Updated policy URL
        spUri.setTextContent(POLICY_URL);

        sigPolicyQualifier.appendChild(spUri);
        sigPolicyQualifiers.appendChild(sigPolicyQualifier);
        sigPolicy.appendChild(sigPolicyQualifiers);
        sigPolicyId.appendChild(sigPolicy);
        signedSigProps.appendChild(sigPolicyId);

        Element signerRole = doc.createElementNS(XADES_NS, "xades:SignerRoleV2");
        Element claimedRoles = doc.createElementNS(XADES_NS, "xades:ClaimedRoles");
        Element claimedRole = doc.createElementNS(XADES_NS, "xades:ClaimedRole");
        claimedRole.setTextContent("Fournisseur");

        claimedRoles.appendChild(claimedRole);
        signerRole.appendChild(claimedRoles);
        signedSigProps.appendChild(signerRole);
        signedProps.appendChild(signedSigProps);

        Element signedDataProps = doc.createElementNS(XADES_NS, "xades:SignedDataObjectProperties");
        Element dataObjFormat = doc.createElementNS(XADES_NS, "xades:DataObjectFormat");
        dataObjFormat.setAttribute("ObjectReference", "#r-id-frs");

        Element mimeType = doc.createElementNS(XADES_NS, "xades:MimeType");
        mimeType.setTextContent("application/octet-stream");

        dataObjFormat.appendChild(mimeType);
        signedDataProps.appendChild(dataObjFormat);
        signedProps.appendChild(signedDataProps);

        return signedProps;
    }

    /**
     * Calculate the SHA-256 hash of the signature policy document.
     * Downloads the policy PDF from the official URL and computes its digest.
     * Uses caching to avoid repeated downloads.
     */
    private static String calculatePolicyHash() {
        // Check cache first
        long currentTime = System.currentTimeMillis();
        if (cachedPolicyHash != null && (currentTime - lastPolicyCheck) < POLICY_CACHE_DURATION) {
            logger.debug("Using cached policy hash");
            return cachedPolicyHash;
        }

        try {
            logger.info("Downloading signature policy document from: {}", POLICY_URL);

            // Download the policy document
            URL url = new URL(POLICY_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(30000); // 30 seconds
            connection.setReadTimeout(60000);    // 60 seconds

            // Set headers to ensure proper PDF download
            connection.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            connection.setRequestProperty("Accept", "application/pdf,application/octet-stream,*/*");
            connection.setRequestProperty("Accept-Language", "fr-FR,fr;q=0.9,en;q=0.8");

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                logger.warn("Failed to download policy document, HTTP {}: {}", responseCode, connection.getResponseMessage());
                logger.info("Using fallback policy hash");
                return POLICY_HASH;
            }

            // Verify content type is PDF
            String contentType = connection.getContentType();
            logger.debug("Policy document content type: {}", contentType);
            if (contentType != null && !contentType.toLowerCase().contains("pdf") && !contentType.toLowerCase().contains("octet-stream")) {
                logger.warn("Unexpected content type for policy document: {}", contentType);
            }

            // Read the document content with proper buffering
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (InputStream inputStream = connection.getInputStream()) {
                byte[] data = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(data)) != -1) {
                    buffer.write(data, 0, bytesRead);
                }
            }

            byte[] policyDocument = buffer.toByteArray();
            logger.info("Downloaded policy document: {} bytes", policyDocument.length);

            // Verify it's a valid PDF by checking magic bytes
            if (policyDocument.length >= 4) {
                String pdfHeader = new String(policyDocument, 0, 4, "ASCII");
                if (!pdfHeader.equals("%PDF")) {
                    logger.warn("Downloaded document does not appear to be a PDF (header: {})", pdfHeader);
                }
            }

            // Calculate SHA-256 hash of the PDF document
            // Note: PDF documents are binary files - no XML canonicalization needed
            // The hash is computed on the raw PDF bytes as downloaded
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(policyDocument);
            String calculatedHash = Base64.getEncoder().encodeToString(hashBytes);

            logger.info("✅ Calculated policy hash: {}", calculatedHash);
            logger.info("📋 Expected policy hash:   {}", POLICY_HASH);

            // Log first few bytes for debugging
            if (policyDocument.length >= 16) {
                StringBuilder hexStart = new StringBuilder();
                for (int i = 0; i < Math.min(16, policyDocument.length); i++) {
                    hexStart.append(String.format("%02X ", policyDocument[i]));
                }
                logger.debug("PDF first 16 bytes: {}", hexStart.toString().trim());
            }

            if (!calculatedHash.equals(POLICY_HASH)) {
                logger.warn("⚠️  Calculated hash differs from expected hash - policy document may have been updated");
                logger.warn("Consider updating POLICY_HASH constant to: {}", calculatedHash);
                logger.info("Using calculated hash for signature policy validation");
            } else {
                logger.info("✅ Policy hash matches expected value");
            }

            // Cache the result
            cachedPolicyHash = calculatedHash;
            lastPolicyCheck = System.currentTimeMillis();

            return calculatedHash;

        } catch (Exception e) {
            logger.error("Failed to download and hash policy document: {}", e.getMessage());

            // Use cached hash if available, otherwise fallback to constant
            if (cachedPolicyHash != null) {
                logger.info("Using cached policy hash: {}", cachedPolicyHash);
                return cachedPolicyHash;
            } else {
                logger.info("Using fallback policy hash: {}", POLICY_HASH);
                return POLICY_HASH;
            }
        }
    }

    public static Element buildCompleteSignature(Document doc, Element signedInfo, String signatureValue,
                                                 List<X509Certificate> certChain, Element signedProps, String signatureId) throws Exception {
        Element signatureElem = doc.createElementNS(DS_NS, "ds:Signature");
        signatureElem.setAttribute("xmlns:ds", DS_NS);
        signatureElem.setAttribute("Id", signatureId);

        Element signatureValueElem = doc.createElementNS(DS_NS, "ds:SignatureValue");
        signatureValueElem.setAttribute("Id", "value-" + signatureId);
        signatureValueElem.setTextContent(signatureValue);

        Element keyInfo = doc.createElementNS(DS_NS, "ds:KeyInfo");
        Element x509Data = doc.createElementNS(DS_NS, "ds:X509Data");
        for (X509Certificate c : certChain) {
            Element x509CertElem = doc.createElementNS(DS_NS, "ds:X509Certificate");
            x509CertElem.setTextContent(Base64.getEncoder().encodeToString(c.getEncoded()));
            x509Data.appendChild(x509CertElem);
        }
        keyInfo.appendChild(x509Data);

        Element objectElem = doc.createElementNS(DS_NS, "ds:Object");
        Element qualifyingProps = doc.createElementNS(XADES_NS, "xades:QualifyingProperties");
        qualifyingProps.setAttribute("xmlns:xades", XADES_NS);
        qualifyingProps.setAttribute("Target", "#" + signatureId);
        qualifyingProps.appendChild(signedProps);
        objectElem.appendChild(qualifyingProps);

        signatureElem.appendChild(signedInfo);
        signatureElem.appendChild(signatureValueElem);
        signatureElem.appendChild(keyInfo);
        signatureElem.appendChild(objectElem);

        return signatureElem;
    }

    // Nested static utility class
    public static class SerialEncoder {
        public static String encodeSerialToBase64(String hexSerial) {
            byte[] serialBytes = new BigInteger(hexSerial, 16).toByteArray();
            if (serialBytes[0] == 0) {
                byte[] tmp = new byte[serialBytes.length - 1];
                System.arraycopy(serialBytes, 1, tmp, 0, tmp.length);
                serialBytes = tmp;
            }
            return Base64.getEncoder().encodeToString(serialBytes);
        }

        public static void main(String[] args) {
            String hexSerial = "5B886D34419C717BC110FD704D9A0C833C239D27";
            String base64 = encodeSerialToBase64(hexSerial);
            System.out.println("Hex serial: " + hexSerial);
            System.out.println("Base64 of serial: " + base64);
            
            // Test the new formatting method
            BigInteger serialBigInt = new BigInteger(hexSerial, 16);
            System.out.println("BigInteger decimal: " + serialBigInt.toString());
            System.out.println("BigInteger hex: " + serialBigInt.toString(16).toUpperCase());
        }
    }
}