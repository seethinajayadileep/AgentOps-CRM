package com.agentopscrm.repository;

import com.agentopscrm.entity.LeadSourceRun;
import com.agentopscrm.entity.enums.LeadSourceRunStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for LeadSourceRun entity (F-010 Apify Lead Finder).
 *
 * @author AgentOps Team
 * @version 0.10.0
 */
@Repository
public interface LeadSourceRunRepository extends JpaRepository<LeadSourceRun, UUID> {

    List<LeadSourceRun> findAllByOrderByCreatedAtDesc();

    Page<LeadSourceRun> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<LeadSourceRun> findByStatusOrderByCreatedAtDesc(LeadSourceRunStatus status);

    Optional<LeadSourceRun> findByApifyRunId(String apifyRunId);
}
