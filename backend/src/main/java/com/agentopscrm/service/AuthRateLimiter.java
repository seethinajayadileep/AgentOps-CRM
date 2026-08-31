package com.agentopscrm.service;

import com.agentopscrm.exception.RateLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-backed login/signup throttle. If Redis is not enabled, requests pass through.
 */
@Service
public class AuthRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(AuthRateLimiter.class);

    private final StringRedisTemplate redis;
    private final int maxAttempts;
    private final Duration window;

    public AuthRateLimiter(
            ObjectProvider<StringRedisTemplate> redis,
            @Value("${app.auth.rate-limit.max-attempts:10}") int maxAttempts,
            @Value("${app.auth.rate-limit.window-seconds:900}") long windowSeconds) {
        this.redis = redis.getIfAvailable();
        this.maxAttempts = maxAttempts;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    public void check(String action, String email, String clientIp) {
        if (redis == null) {
            return;
        }
        String safeEmail = email == null ? "unknown" : email.trim().toLowerCase();
        String safeIp = clientIp == null || clientIp.isBlank() ? "unknown" : clientIp;
        String key = "crm:auth:" + action + ":" + safeEmail + ":" + safeIp;
        try {
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redis.expire(key, window);
            }
            if (count != null && count > maxAttempts) {
                throw new RateLimitExceededException(
                        "Too many attempts. Wait a few minutes and try again.");
            }
        } catch (RateLimitExceededException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Redis rate limit skipped: {}", ex.getMessage());
        }
    }
}
