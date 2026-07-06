package com.agentopscrm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for AgentOps CRM Spring Boot application.
 *
 * This is a multi-agent voice AI platform for CRM automation.
 *
 * {@code @EnableScheduling} powers periodic reconciliation jobs (e.g. Apify Lead Finder
 * stale RUNNING run recovery). {@code @EnableAsync} powers background jobs such as the
 * asynchronous knowledge-base build workflow.
 *
 * @author AgentOps Team
 * @version 0.1.0
 * @since 2026-07-01
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class AgentOpsCrmApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentOpsCrmApplication.class, args);
    }
}