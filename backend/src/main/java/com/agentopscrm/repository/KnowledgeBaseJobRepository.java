package com.agentopscrm.repository;

import com.agentopscrm.entity.KnowledgeBaseJob;
import com.agentopscrm.entity.enums.KnowledgeBaseJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link KnowledgeBaseJob} (Bug 2: async knowledge-base build).
 *
 * @author AgentOps Team
 * @version 0.1.0
 */
@Repository
public interface KnowledgeBaseJobRepository extends JpaRepository<KnowledgeBaseJob, UUID> {

    List<KnowledgeBaseJob> findByBusinessIdAndStatusIn(UUID businessId, List<KnowledgeBaseJobStatus> statuses);

    Optional<KnowledgeBaseJob> findFirstByBusinessIdOrderByStartedAtDesc(UUID businessId);

    List<KnowledgeBaseJob> findByStatusIn(List<KnowledgeBaseJobStatus> statuses);
}
