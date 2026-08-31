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
 * Railway uses {@code /actuator/health} as the deploy probe.
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
        services.put("redis", new ServiceStatus("DISABLED", "Redis is not required by this application"));

        response.setServices(services);
        response.setVersion(AppVersion.VALUE);

        return ResponseEntity.ok(response);
    }
}
