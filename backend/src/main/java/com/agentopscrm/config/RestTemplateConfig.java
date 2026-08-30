package com.agentopscrm.config;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;

/**
 * RestTemplate with Apache HttpClient 5 using the JVM default truststore.
 *
 * {@code SSLContexts.createSystemDefault()} loads the platform/JSSE cacerts so public
 * CA chains (including Let's Encrypt used by api.apify.com) validate. Hostname
 * verification stays enabled.
 */
@Configuration
public class RestTemplateConfig {

    private static final Logger logger = LoggerFactory.getLogger(RestTemplateConfig.class);

    @Bean
    public RestTemplate restTemplate() {
        try {
            SSLContext sslContext = SSLContexts.createSystemDefault();

            SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
                    .setSslContext(sslContext)
                    .setHostnameVerifier(new DefaultHostnameVerifier())
                    .build();

            SocketConfig socketConfig = SocketConfig.custom()
                    .setSoTimeout(Timeout.ofSeconds(30))
                    .build();

            HttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                    .setSSLSocketFactory(sslSocketFactory)
                    .setDefaultSocketConfig(socketConfig)
                    .setMaxConnTotal(100)
                    .setMaxConnPerRoute(20)
                    .build();

            CloseableHttpClient httpClient = HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    .build();

            HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
            factory.setConnectTimeout(15000);
            factory.setConnectionRequestTimeout(15000);

            logger.info("RestTemplate configured with JVM system TLS truststore and hostname verification");
            return new RestTemplate(factory);
        } catch (Exception e) {
            logger.error("CRITICAL: Failed to configure secure SSL/TLS for RestTemplate.", e);
            throw new IllegalStateException("Failed to configure secure SSL/TLS for RestTemplate", e);
        }
    }
}
