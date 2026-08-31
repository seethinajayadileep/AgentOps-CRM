package com.agentopscrm.service;

import com.agentopscrm.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthRateLimiterTest {

    @Mock private ObjectProvider<StringRedisTemplate> redisProvider;
    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> values;

    @BeforeEach
    void setUp() {
        lenient().when(redisProvider.getIfAvailable()).thenReturn(redis);
        lenient().when(redis.opsForValue()).thenReturn(values);
    }

    @Test
    void allowsUntilMaxAttempts() {
        when(values.increment(anyString())).thenReturn(3L);
        AuthRateLimiter limiter = new AuthRateLimiter(redisProvider, 10, 900);
        assertDoesNotThrow(() -> limiter.check("login", "demo@agentcrm.app", "1.1.1.1"));
    }

    @Test
    void blocksAfterMaxAttempts() {
        when(values.increment(anyString())).thenReturn(11L);
        AuthRateLimiter limiter = new AuthRateLimiter(redisProvider, 10, 900);
        assertThrows(RateLimitExceededException.class, () -> limiter.check("login", "demo@agentcrm.app", "1.1.1.1"));
    }

    @Test
    void skipsWhenRedisMissing() {
        when(redisProvider.getIfAvailable()).thenReturn(null);
        AuthRateLimiter limiter = new AuthRateLimiter(redisProvider, 10, 900);
        assertDoesNotThrow(() -> limiter.check("login", "demo@agentcrm.app", "1.1.1.1"));
    }
}
