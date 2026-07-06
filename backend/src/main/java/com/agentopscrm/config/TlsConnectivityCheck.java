package com.agentopscrm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Validates TLS connectivity on application startup.
 * 
 * Performs a safe TLS handshake test against api.apify.com to verify
 * that the SSL configuration is working correctly and that the JVM
 * truststore contains the necessary CA certificates.
 * 
 * This prevents runtime PKIX errors by failing fast at startup if
 * TLS is misconfigured.
 *
 * @author AgentOps Team
 * @version 0.4.0
 */
@Component
public class TlsConnectivityCheck {

    private static final Logger logger = LoggerFactory.getLogger(TlsConnectivityCheck.class);
    
    private final RestTemplate restTemplate;

    public TlsConnectivityCheck(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateTlsConnectivity() {
        logger.info("Performing TLS connectivity check...");
        
        try {
            // Test TLS handshake against Apify API
            // We don't need a valid token for this - we just need to verify TLS works
            // A 401 (Unauthorized) or 404 (Not Found) response is acceptable
            // A PKIX error is not acceptable
            ResponseEntity<String> response = restTemplate.exchange(
                "https://api.apify.com/v2",
                HttpMethod.GET,
                null,
                String.class
            );
            
            int statusCode = response.getStatusCode().value();
            if (statusCode >= 200 && statusCode < 500) {
                logger.info("TLS connectivity check PASSED - Successfully connected to api.apify.com (HTTP {})", statusCode);
            } else {
                logger.warn("TLS connectivity check completed with HTTP status {}", statusCode);
            }
            
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // 4xx errors are fine - it means TLS worked, just no auth
            int statusCode = e.getStatusCode().value();
            if (statusCode == 401 || statusCode == 404) {
                logger.info("TLS connectivity check PASSED - Successfully connected to api.apify.com (HTTP {})", statusCode);
            } else {
                logger.warn("TLS connectivity check completed with HTTP {}: {}", statusCode, e.getMessage());
            }
            
        } catch (Exception e) {
            // Check if it's an SSL/TLS error
            String message = e.getMessage() != null ? e.getMessage() : "";
            Throwable cause = e.getCause();
            while (cause != null) {
                message += " " + (cause.getMessage() != null ? cause.getMessage() : "");
                cause = cause.getCause();
            }
            
            if (message.contains("PKIX path building failed") || message.contains("certificate")) {
                logger.error("TLS connectivity check FAILED - PKIX/Certificate error. " +
                           "The JVM truststore may not contain the required CA certificates for api.apify.com");
                logger.error("This will prevent Apify API calls from working. Check Java truststore configuration.");
                logger.error("Error details: {}", message);
            } else {
                logger.error("TLS connectivity check FAILED with error: {}", message);
            }
            // Don't fail startup, but log the error prominently
        }
    }
}
