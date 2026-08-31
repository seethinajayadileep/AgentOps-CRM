package com.agentopscrm.controller;

import com.agentopscrm.dto.HealthResponse;
import com.agentopscrm.dto.ServiceStatus;
import com.agentopscrm.util.AppVersion;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lightweight liveness endpoint for local checks and the marketing/API clients.
 * Railway uses {@code /api/health} as the deploy probe (liveness).
 * {@code /actuator/health} includes the database and is for diagnostics.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        HealthResponse response = new HealthResponse();
        response.setStatus("UP");
        response.setTimestamp(Instant.now().toString());

        Map<String, ServiceStatus> services = new LinkedHashMap<>();
        services.put("application", new ServiceStatus("UP", "AgentOps CRM API"));
        services.put("redis", new ServiceStatus(
                "OPTIONAL",
                "Used for auth rate limits and dashboard cache when REDIS_URL is set"));

        response.setServices(services);
        response.setVersion(AppVersion.VALUE);

        return ResponseEntity.ok(response);
    }
}
