package com.agentopscrm.service;

import com.agentopscrm.entity.AgentLog;
import com.agentopscrm.entity.Business;
import com.agentopscrm.entity.enums.AgentActionStatus;
import com.agentopscrm.repository.AgentLogRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Audit logging service with rollback-safe transaction propagation.
 * 
 * Uses REQUIRES_NEW propagation to ensure audit logs are committed even if the
 * parent transaction rolls back. This is critical for failure logging where the
 * Knowledge Base build fails and rolls back, but we still need the failure log.
 * 
 * @author AgentOps Team
 * @version 0.4.0
 */
@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AgentLogRepository agentLogRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public AuditLogService(AgentLogRepository agentLogRepository) {
        this.agentLogRepository = agentLogRepository;
    }

    /**
     * Log an agent action in a separate transaction that survives parent rollback.
     * 
     * REQUIRES_NEW propagation ensures this log is committed independently, even if
     * the calling transaction fails and rolls back. This is essential for failure logs.
     * 
     * @param businessId the business ID
     * @param agentName the agent name
     * @param action the action being performed
     * @param inputJson sanitized input JSON (no sensitive data)
     * @param outputJson sanitized output JSON (no sensitive data)
     * @param status the action status
     * @param durationMs duration in milliseconds
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAgentAction(UUID businessId, String agentName, String action,
                                String inputJson, String outputJson,
                                AgentActionStatus status, Long durationMs) {
        try {
            AgentLog logEntry = new AgentLog();
            logEntry.setAgentName(agentName);
            logEntry.setAction(action);
            logEntry.setInputJson(inputJson);
            logEntry.setOutputJson(outputJson);
            logEntry.setStatus(status);
            logEntry.setDurationMs(durationMs);
            
            if (businessId != null) {
                // Use entity reference to avoid loading the entire Business entity
                logEntry.setBusiness(entityManager.getReference(Business.class, businessId));
            }
            
            agentLogRepository.save(logEntry);
            log.debug("Logged agent action: {} - {} for business {}", agentName, action, businessId);
        } catch (Exception e) {
            // Audit failure must not hide the original error
            log.error("Failed to log agent action {} for business {}. Original operation may have succeeded or failed independently.", 
                    action, businessId, e);
        }
    }

    /**
     * Log an agent action with error message in a separate transaction.
     * 
     * @param businessId the business ID  
     * @param agentName the agent name
     * @param action the action being performed
     * @param inputJson sanitized input JSON
     * @param outputJson sanitized output JSON
     * @param status the action status
     * @param errorMessage sanitized error message (no stack traces or sensitive data)
     * @param durationMs duration in milliseconds
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAgentActionWithError(UUID businessId, String agentName, String action,
                                         String inputJson, String outputJson,
                                         AgentActionStatus status, String errorMessage,
                                         Long durationMs) {
        try {
            AgentLog logEntry = new AgentLog();
            logEntry.setAgentName(agentName);
            logEntry.setAction(action);
            logEntry.setInputJson(inputJson);
            logEntry.setOutputJson(outputJson);
            logEntry.setStatus(status);
            logEntry.setErrorMessage(errorMessage);
            logEntry.setDurationMs(durationMs);
            
            if (businessId != null) {
                logEntry.setBusiness(entityManager.getReference(Business.class, businessId));
            }
            
            agentLogRepository.save(logEntry);
            log.debug("Logged agent action with error: {} - {} for business {}", agentName, action, businessId);
        } catch (Exception e) {
            // Audit failure must not hide the original error
            log.error("Failed to log agent action {} with error for business {}. Original operation may have succeeded or failed independently.", 
                    action, businessId, e);
        }
    }
}
