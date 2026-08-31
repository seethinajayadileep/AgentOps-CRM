package com.agentopscrm.service;

import com.agentopscrm.controller.DashboardController.DashboardStats;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardStatsCacheTest {

    @Mock private ObjectProvider<StringRedisTemplate> redisProvider;
    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> values;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private DashboardStatsCache cache;

    @BeforeEach
    void setUp() {
        when(redisProvider.getIfAvailable()).thenReturn(redis);
        when(redis.opsForValue()).thenReturn(values);
        cache = new DashboardStatsCache(redisProvider, objectMapper, 20);
    }

    @Test
    void roundTripsStatsJson() throws Exception {
        DashboardStats stats = new DashboardStats();
        stats.totalLeads = 19;
        cache.put(stats);

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(values).set(eq(DashboardStatsCache.KEY), json.capture(), eq(Duration.ofSeconds(20)));

        when(values.get(DashboardStatsCache.KEY)).thenReturn(json.getValue());
        Optional<DashboardStats> loaded = cache.get();
        assertTrue(loaded.isPresent());
        assertEquals(19, loaded.get().totalLeads);
    }
}
