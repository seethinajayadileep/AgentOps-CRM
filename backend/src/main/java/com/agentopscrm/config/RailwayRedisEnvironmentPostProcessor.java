package com.agentopscrm.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps Railway {@code REDIS_URL} onto Spring Data Redis properties and enables
 * {@code app.redis.enabled} when a Redis service is linked.
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RailwayRedisEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String explicit = environment.getProperty("REDIS_ENABLED");
        String raw = firstNonBlank(
                environment.getProperty("REDIS_PRIVATE_URL"),
                environment.getProperty("REDIS_URL"));
        Map<String, Object> properties = new LinkedHashMap<>();

        if (raw != null) {
            RedisUrl.Parsed parsed = RedisUrl.parse(raw);
            properties.put("spring.data.redis.host", parsed.host());
            properties.put("spring.data.redis.port", String.valueOf(parsed.port()));
            if (parsed.password() != null) {
                properties.put("spring.data.redis.password", parsed.password());
            }
            if (parsed.username() != null) {
                properties.put("spring.data.redis.username", parsed.username());
            }
            properties.put("spring.data.redis.ssl.enabled", String.valueOf(parsed.ssl()));
            if (explicit == null || explicit.isBlank()) {
                properties.put("app.redis.enabled", "true");
                properties.put("REDIS_ENABLED", "true");
            }
        }

        if (explicit == null && firstNonBlank(
                environment.getProperty("REDISHOST"),
                environment.getProperty("REDIS_HOST")) != null) {
            String host = firstNonBlank(environment.getProperty("REDISHOST"), environment.getProperty("REDIS_HOST"));
            properties.put("spring.data.redis.host", host);
            String port = firstNonBlank(environment.getProperty("REDISPORT"), environment.getProperty("REDIS_PORT"));
            if (port != null) {
                properties.put("spring.data.redis.port", port);
            }
            String password = firstNonBlank(
                    environment.getProperty("REDISPASSWORD"),
                    environment.getProperty("REDIS_PASSWORD"));
            if (password != null) {
                properties.put("spring.data.redis.password", password);
            }
            if (!properties.containsKey("app.redis.enabled")) {
                properties.put("app.redis.enabled", "true");
                properties.put("REDIS_ENABLED", "true");
            }
        }

        if (!properties.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("railwayRedis", properties));
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
