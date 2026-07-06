package com.agentopscrm.repository;

import com.agentopscrm.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Document entity.
 *
 * @author AgentOps Team
 * @version 0.2.0
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByBusinessId(UUID businessId);

    List<Document> findByBusinessIdOrderByCreatedAtDesc(UUID businessId);

    boolean existsByUrl(String url);

    boolean existsByBusinessIdAndUrl(UUID businessId, String url);
}