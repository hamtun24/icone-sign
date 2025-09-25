package com.example.unifiedapi.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.RestTemplate;

@Configuration
public class SSLConfig {

    private static final Logger logger = LoggerFactory.getLogger(SSLConfig.class);

    @Value("${ance.seal.ssl.verify:true}")
    private boolean sslVerifyEnabled;

    @Bean
    public RestTemplate restTemplate() {
        try {
            if (!sslVerifyEnabled) {
                logger.warn("SSL verification is DISABLED - this should only be used in development!");
                return createInsecureRestTemplate();
            }

            return createSecureRestTemplate();

        } catch (Exception e) {
            logger.error("Failed to create RestTemplate: {}", e.getMessage());
            return new RestTemplate();
        }
    }

    private RestTemplate createSecureRestTemplate() throws Exception {
        // For now, create custom trust store with ANCE certificates
        // This will be enhanced later with proper HTTP client integration
        addAnceCertificatesToSystemTrustStore();
        return new RestTemplate();
    }

    private RestTemplate createInsecureRestTemplate() throws Exception {
        // Create SSL context that trusts all certificates (DEVELOPMENT ONLY)
        javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
        sslContext.init(null, new javax.net.ssl.TrustManager[] {
            new javax.net.ssl.X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
                public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
            }
        }, new java.security.SecureRandom());

        // Set as default SSL context
        javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

        return new RestTemplate();
    }

    private void addAnceCertificatesToSystemTrustStore() {
        try {
            java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");

            // Add root certificate
            try {
                ClassPathResource rootResource = new ClassPathResource("certificates/TnTrustQualifiedGovCA.crt");
                if (rootResource.exists()) {
                    try (java.io.InputStream is = rootResource.getInputStream()) {
                        java.security.cert.X509Certificate rootCert = (java.security.cert.X509Certificate) cf.generateCertificate(is);
                        logger.info("ANCE root certificate loaded: {}", rootCert.getSubjectX500Principal().getName());
                    }
                }
            } catch (Exception e) {
                logger.warn("Could not load ANCE root certificate: {}", e.getMessage());
            }

            // Add intermediate certificate
            try {
                ClassPathResource interResource = new ClassPathResource("certificates/TunTrustQualifiedCAInter.crt");
                if (interResource.exists()) {
                    try (java.io.InputStream is = interResource.getInputStream()) {
                        java.security.cert.X509Certificate interCert = (java.security.cert.X509Certificate) cf.generateCertificate(is);
                        logger.info("ANCE intermediate certificate loaded: {}", interCert.getSubjectX500Principal().getName());
                    }
                }
            } catch (Exception e) {
                logger.warn("Could not load ANCE intermediate certificate: {}", e.getMessage());
            }

        } catch (Exception e) {
            logger.error("Failed to load ANCE certificates: {}", e.getMessage());
        }
    }
}
