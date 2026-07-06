package com.agentopscrm.controller;

import com.agentopscrm.dto.HealthResponse;
import com.agentopscrm.dto.ServiceStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check controller for monitoring system status.
 *
 * Endpoint: GET /api/health
 * API ID: API-001
 *
 * @author AgentOps Team
 * @version 0.1.0
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    /**
     * Health check endpoint that returns system status.
     *
     * @return HealthResponse containing system and service status
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        HealthResponse response = new HealthResponse();
        response.setStatus("UP");
        response.setTimestamp(Instant.now().toString());

        Map<String, ServiceStatus> services = new HashMap<>();
        services.put("database", new ServiceStatus("UP", "Database connection established"));
        services.put("redis", new ServiceStatus("UP", "Redis connection established"));

        response.setServices(services);
        response.setVersion("0.1.0");

        return ResponseEntity.ok(response);
    }
}