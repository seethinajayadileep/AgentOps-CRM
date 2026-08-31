package com.agentopscrm.service;

import com.agentopscrm.controller.DashboardController.DashboardStats;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Short-lived Redis cache for dashboard counts so the overview query is not
 * repeated on every page load.
 */
@Service
public class DashboardStatsCache {

    static final String KEY = "crm:dashboard:stats";
    private static final Logger log = LoggerFactory.getLogger(DashboardStatsCache.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public DashboardStatsCache(
            ObjectProvider<StringRedisTemplate> redis,
            ObjectMapper objectMapper,
            @Value("${app.dashboard.cache-ttl-seconds:20}") long ttlSeconds) {
        this.redis = redis.getIfAvailable();
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofSeconds(Math.max(1, ttlSeconds));
    }

    public Optional<DashboardStats> get() {
        if (redis == null) {
            return Optional.empty();
        }
        try {
            String json = redis.opsForValue().get(KEY);
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, DashboardStats.class));
        } catch (Exception ex) {
            log.warn("Dashboard Redis cache read skipped: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public void put(DashboardStats stats) {
        if (redis == null || stats == null) {
            return;
        }
        try {
            redis.opsForValue().set(KEY, objectMapper.writeValueAsString(stats), ttl);
        } catch (Exception ex) {
            log.warn("Dashboard Redis cache write skipped: {}", ex.getMessage());
        }
    }
}
