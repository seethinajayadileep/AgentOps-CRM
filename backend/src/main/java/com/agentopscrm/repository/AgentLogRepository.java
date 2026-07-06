package com.agentopscrm.repository;

import com.agentopscrm.entity.AgentLog;
import com.agentopscrm.entity.enums.AgentActionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for AgentLog entity.
 *
 * @author AgentOps Team
 * @version 0.3.0
 * Feature: F-012 - Agent Logs Observability
 */
@Repository
public interface AgentLogRepository extends JpaRepository<AgentLog, UUID>, JpaSpecificationExecutor<AgentLog> {

    Page<AgentLog> findByBusinessId(UUID businessId, Pageable pageable);

    Page<AgentLog> findByConversationId(UUID conversationId, Pageable pageable);

    Page<AgentLog> findByLeadId(UUID leadId, Pageable pageable);

    Page<AgentLog> findByAgentName(String agentName, Pageable pageable);

    Page<AgentLog> findByAction(String action, Pageable pageable);

    Page<AgentLog> findByStatus(AgentActionStatus status, Pageable pageable);

    @Query("SELECT a FROM AgentLog a WHERE a.createdAt BETWEEN :startDate AND :endDate")
    Page<AgentLog> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);

    @Query("SELECT a FROM AgentLog a WHERE a.business.id = :businessId AND a.createdAt BETWEEN :startDate AND :endDate")
    Page<AgentLog> findByBusinessIdAndCreatedAtBetween(@Param("businessId") UUID businessId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);

    @Query("SELECT a FROM AgentLog a WHERE a.agentName = :agentName AND a.action = :action ORDER BY a.createdAt DESC")
    List<AgentLog> findByAgentNameAndAction(@Param("agentName") String agentName, @Param("action") String action);

    long countByBusinessId(UUID businessId);

    long countByStatus(AgentActionStatus status);

    /** Count agent actions logged within a time window (used for "today"). */
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /** Most recent agent actions for the dashboard "Recent Activity" feed. */
    List<AgentLog> findTop8ByOrderByCreatedAtDesc();

    @Query("SELECT COUNT(a) FROM AgentLog a WHERE a.status = 'ERROR' AND a.createdAt > :since")
    long countErrorsSince(@Param("since") LocalDateTime since);
}