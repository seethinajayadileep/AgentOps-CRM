package com.agentopscrm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;

/**
 * Configuration for RestTemplate with secure TLS/SSL handling.
 * 
 * Uses Apache HttpClient 5 with properly configured SSL context that loads
 * certificates from the JVM's system truststore (cacerts).
 * 
 * This fixes PKIX path building failures by:
 * 1. Creating an SSLContext that loads trust material from the JVM truststore
 * 2. Building an SSLConnectionSocketFactory with this context
 * 3. Actually using the factory in the connection manager
 * 4. Maintaining hostname verification for security
 * 
 * The previous implementation created an SSLContext but used the default
 * socket factory instead, which didn't properly load the system trust material.
 *
 * @author AgentOps Team
 * @version 0.4.0
 */
@Configuration
public class RestTemplateConfig {

    private static final Logger logger = LoggerFactory.getLogger(RestTemplateConfig.class);

    @Bean
    public RestTemplate restTemplate() {
        try {
            // Create SSL context that loads trust material from JVM's default cacerts
            // Passing null for KeyStore uses the JVM's default truststore
            SSLContext sslContext = new SSLContextBuilder()
                    .loadTrustMaterial(null, (TrustStrategy) null)
                    .build();

            // Build SSL socket factory with the context and hostname verification
            SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
                    .setSslContext(sslContext)
                    .setHostnameVerifier(new DefaultHostnameVerifier())
                    .build();

            // Configure socket timeouts
            SocketConfig socketConfig = SocketConfig.custom()
                    .setSoTimeout(Timeout.ofSeconds(300))
                    .build();

            // Build connection manager with the SSL socket factory
            HttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                    .setSSLSocketFactory(sslSocketFactory)
                    .setDefaultSocketConfig(socketConfig)
                    .setMaxConnTotal(100)
                    .setMaxConnPerRoute(20)
                    .build();

            // Build HTTP client with connection manager
            CloseableHttpClient httpClient = HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    .build();

            // Create factory with configured client
            HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
            factory.setConnectTimeout(30000);           // Connection timeout: 30 seconds
            factory.setConnectionRequestTimeout(30000);  // Connection request timeout: 30 seconds

            logger.info("RestTemplate configured with secure SSL context, hostname verification enabled, and system truststore");
            return new RestTemplate(factory);
            
        } catch (Exception e) {
            // Fail fast - do not fall back to insecure configuration
            logger.error("CRITICAL: Failed to configure secure SSL/TLS for RestTemplate. Application cannot start.", e);
            throw new IllegalStateException("Failed to configure secure SSL/TLS for RestTemplate", e);
        }
    }
}
