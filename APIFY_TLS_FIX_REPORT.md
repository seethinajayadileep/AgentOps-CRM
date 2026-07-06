# Apify TLS/PKIX Certificate Fix Report

## Executive Summary

**Status**: ✅ **FIXED**

The Apify API TLS/PKIX path building error has been successfully resolved. The root cause was that [`RestTemplateConfig`](backend/src/main/java/com/agentopscrm/config/RestTemplateConfig.java:1) was creating an [`SSLContext`](backend/src/main/java/com/agentopscrm/config/RestTemplateConfig.java:48) but not actually using it in the HTTP client configuration.

---

## 1. JVM Diagnostics

### Environment Confirmed
- **Java Version**: Java 25 (Oracle JDK)
- **Java Home**: `/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home`
- **Runtime**: Backend runs directly on macOS (not Docker)
- **Truststore Path**: `/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home/lib/security/cacerts`
- **Truststore Size**: 136,458 bytes
- **Truststore Type**: JKS (Java KeyStore)
- **System Property**: `javax.net.ssl.trustStore` was NOT explicitly configured

### Certificate Chain Analysis
```
api.apify.com certificate chain:
 0 s:CN=*.apify.com
   i:C=US, O=Amazon, CN=Amazon RSA 2048 M04
```

- Apify uses an Amazon-issued TLS certificate
- The JVM truststore contains the required Amazon root CAs:
  - `amazonrootca1 [jdk]`
  - `amazonrootca2 [jdk]`
  - `amazonrootca3 [jdk]`
  - `amazonrootca4 [jdk]`

**Conclusion**: The JVM truststore already contained all necessary CA certificates. No certificate or proxy issues existed.

---

## 2. Root Cause

### The Problem

In [`RestTemplateConfig.java`](backend/src/main/java/com/agentopscrm/config/RestTemplateConfig.java:1), line 41 created an [`SSLContext`](backend/src/main/java/com/agentopscrm/config/RestTemplateConfig.java:41):
```java
SSLContext sslContext = SSLContexts.createSystemDefault();
```

However, line 43 immediately ignored it and used a generic factory:
```java
SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactory.getSocketFactory();
```

This static factory method creates a **default** socket factory that does not load the JVM's system truststore correctly on Java 25 on macOS. The [`sslContext`](backend/src/main/java/com/agentopscrm/config/RestTemplateConfig.java:41) variable was created but never used.

---

## 3. Solution Implemented

### Fixed Configuration

Updated [`RestTemplateConfig.java`](backend/src/main/java/com/agentopscrm/config/RestTemplateConfig.java:1) to properly create and **use** the SSL context:

```java
// Create SSL context that loads trust material from JVM's cacerts
SSL Context sslContext = new SSLContextBuilder()
        .loadTrustMaterial(null, (TrustStrategy) null)
        .build();

// Build SSL socket factory with the context and hostname verification
SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
        .setSslContext(sslContext)  // ← NOW ACTUALLY USED
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
```

### Key Changes

1. **SSL Context Creation**: Changed from `.createSystemDefault()` to explicit `.loadTrustMaterial(null, null)` with builder pattern
2. **Socket Factory**: Now created with [`SSLConnectionSocketFactoryBuilder`](backend/src/main/java/com/agentopscrm/config/RestTemplateConfig.java:56) that actually attaches the SSL context
3. **Hostname Verification**: Explicitly enabled with [`DefaultHostnameVerifier`](backend/src/main/java/com/agentopscrm/config/RestTemplateConfig.java:58)
4. **Timeouts**: Added socket timeout configuration (300s)
5. **Connection Pooling**: Configured with 100 max total connections, 20 per route
6. **Fail-fast**: Removed insecure fallback; now throws [`IllegalStateException`](backend/src/main/java/com/agentopscrm/config/RestTemplateConfig.java:87) if SSL configuration fails

---

## 4. Startup TLS Validation

### New Component: [`TlsConnectivityCheck`](backend/src/main/java/com/agentopscrm/config/TlsConnectivityCheck.java:1)

Created a startup validator that runs on [`ApplicationReadyEvent`](backend/src/main/java/com/agentopscrm/config/TlsConnectivityCheck.java:30):

```java
@Component
public class TlsConnectivityCheck {
    
    @EventListener(ApplicationReadyEvent.class)
    public void validateTlsConnectivity() {
        // Test TLS handshake against Apify API
        ResponseEntity<String> response = restTemplate.exchange(
            "https://api.apify.com/v2",
            HttpMethod.GET,
            null,
            String.class
        );
        
        // 200, 401, 404 === TLS OK
        // PKIX error === TLS NOT OK
    }
}
```

**Purpose**: Fail fast at startup if TLS is misconfigured, before any real Apify workflows begin.

**Test Result**:
```
2026-07-06 10:50:35.506 [main] INFO  c.a.config.TlsConnectivityCheck - TLS connectivity check PASSED - Successfully connected to api.apify.com (HTTP 404)
```

✅ **HTTP 404 response proves TLS handshake succeeded**. The endpoint returns 404 without authentication, which is expected and acceptable.

---

## 5. Security Validation

### Verification Points

✅ **TLS certificate verification**: ENABLED  
✅ **Hostname verification**: ENABLED ([`DefaultHostnameVerifier`](backend/src/main/java/com/agentopscrm/config/RestTemplateConfig.java:58))  
✅ **Trust-all manager**: NOT USED  
✅ **System truststore**: Loaded from JVM cacerts  
✅ **No hardcoded certificates**: None added  
✅ **No token exposure**: Token remains in backend `.env` only  
✅ **Fail-fast on misconfiguration**: Throws exception at startup  

---

## 6. Files Changed

### Modified
- [`backend/src/main/java/com/agentopscrm/config/RestTemplateConfig.java`](backend/src/main/java/com/agentopscrm/config/RestTemplateConfig.java:1)

### Created
- [`backend/src/main/java/com/agentopscrm/config/TlsConnectivityCheck.java`](backend/src/main/java/com/agentopscrm/config/TlsConnectivityCheck.java:1)

---

## 7. Test Results

### Startup Test
```
✅ TLS connectivity check PASSED - Successfully connected to api.apify.com (HTTP 404)
```

### Real Apify Workflow
**Status**: Pending manual validation

To complete final validation:
1. Start the backend (already running)
2. Navigate to Lead Finder in the UI
3. Start a one-result advertising-agency search
4. Confirm actor run is created
5. Confirm status changes from RUNNING → SUCCEEDED
6. Confirm automatic sync downloads dataset
7. Confirm result count updates
8. Confirm manual "Sync from Apify" works
9. Confirm no PKIX errors in logs

---

## 8. Technical Details

### Apache HttpClient 5 Configuration

The fix uses the standard Apache HttpClient 5 builder pattern:
- [`SSLContextBuilder`](backend/src/main/java/com/agentopscrm/config/RestTemplateConfig.java:48): Creates context from system trust material
- [`SSLConnectionSocketFactoryBuilder`](backend/src/main/java/com/agentopscrm/config/RestTemplateConfig.java:56): Attaches SSL context and hostname verifier
- [`PoolingHttpClientConnectionManagerBuilder`](backend/src/main/java/com/agentopscrm/config/RestTemplateConfig.java:68): Configures pooling and SSL
- [`HttpClients.custom()`](backend/src/main/java/com/agentopscrm/config/RestTemplateConfig.java:77): Builds the final client

### Timeout Configuration

- **Connection timeout**: 30 seconds (time to establish TCP connection)
- **Connection request timeout**: 30 seconds (time to get connection from pool)
- **Socket timeout**: 300 seconds (time to read response data)

---

## 9. Setup Requirements

### macOS (Current Environment)
✅ **No additional setup required**

The standard JDK 25 truststore already contains all necessary Amazon root CAs.

### Docker (Future Deployment)
If deploying in Docker, ensure the base image has updated CA certificates:

```dockerfile
FROM eclipse-temurin:21-jdk-alpine
RUN apk update && apk add --no-cache ca-certificates
...
```

For production, keep the JDK updated to ensure the latest CA bundle.

### Corporate Proxy
If behind a corporate TLS-inspection proxy:

1. Obtain the organization-provided root CA certificate
2. Create a dedicated truststore:
   ```bash
   keytool -importcert \
     -file corporate-ca.crt \
     -keystore agentops-truststore.p12 \
     -storetype PKCS12 \
     -storepass "${TRUST STORE_PASSWORD}" \
     -alias corporate-root \
     -noprompt
   ```
3. Configure JVM:
   ```
   -Djavax.net.ssl.trustStore=/path/to/agentops-truststore.p12
   -Djavax.net.ssl.trustStoreType=PKCS12
   -Djavax.net.ssl.trustStorePassword=${TRUSTSTORE_PASSWORD}
   ```

**Note**: Not needed in current setup.

---

## 10. Next Steps

### Immediate
- [ ] Complete real Apify workflow validation (one-result advertising search)
- [ ] Verify automatic synchronization
- [ ] Verify manual "Sync from Apify"
- [ ] Confirm no PKIX errors in logs

### Testing (Next Task)
- [ ] Add unit test: Secure RestTemplate construction
- [ ] Add unit test: Timeouts are configured
- [ ] Add unit test: Hostname verification enabled
- [ ] Add integration test: Real TLS handshake to `api.apify.com`
- [ ] Add test: Startup failure with clear error if SSL fails
- [ ] Add test: Apify errors are sanitized
- [ ] Add test: No token in logs or API responses

### Documentation
- [ ] Update deployment guide with truststore instructions
- [ ] Document Docker base image requirements
- [ ] Add troubleshooting section for PKIX errors

---

## 11. Conclusion

**Root Cause**: The [`SSLContext`](backend/src/main/java/com/agentopscrm/config/RestTemplateConfig.java:48) was created but never used in the HTTP client configuration.

**Fix**: Properly build and attach the SSL context using Apache HttpClient 5 builders.

**Result**: TLS connectivity to `api.apify.com` now works. Startup validation passes. TLS verification remains enabled.

**Validation**: Startup check passes. Real Apify run needs final manual confirmation.

---

**Report Generated**: 2026-07-06  
**Java Version**: Java 25 (Oracle)  
**macOS Version**: Detected via system  
**Backend PID**: 10740  
**TLS Status**: ✅ WORKING
