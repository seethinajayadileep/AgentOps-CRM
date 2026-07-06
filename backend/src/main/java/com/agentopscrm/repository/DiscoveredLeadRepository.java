package com.agentopscrm.repository;

import com.agentopscrm.entity.DiscoveredLead;
import com.agentopscrm.entity.enums.DiscoveredLeadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for DiscoveredLead entity (F-010 Apify Lead Finder).
 *
 * @author AgentOps Team
 * @version 0.10.0
 */
@Repository
public interface DiscoveredLeadRepository extends JpaRepository<DiscoveredLead, UUID> {

    List<DiscoveredLead> findByLeadSourceRunIdOrderByScoreDesc(UUID leadSourceRunId);

    List<DiscoveredLead> findByLeadSourceRunIdAndStatus(UUID leadSourceRunId, DiscoveredLeadStatus status);

    long countByLeadSourceRunId(UUID leadSourceRunId);

    long countByLeadSourceRunIdAndStatus(UUID leadSourceRunId, DiscoveredLeadStatus status);

    // --- Duplicate detection within a run (avoid re-inserting same prospect on re-sync) ---

    boolean existsByLeadSourceRunIdAndEmailIgnoreCase(UUID leadSourceRunId, String email);

    boolean existsByLeadSourceRunIdAndPhone(UUID leadSourceRunId, String phone);

    boolean existsByLeadSourceRunIdAndWebsiteUrlIgnoreCase(UUID leadSourceRunId, String websiteUrl);

    @Query("SELECT COUNT(d) > 0 FROM DiscoveredLead d WHERE d.leadSourceRun.id = :runId "
        + "AND LOWER(d.businessName) = LOWER(:businessName) "
        + "AND LOWER(COALESCE(d.location, '')) = LOWER(COALESCE(:location, ''))")
    boolean existsByRunAndBusinessNameAndLocation(@Param("runId") UUID runId,
                                                  @Param("businessName") String businessName,
                                                  @Param("location") String location);
}
