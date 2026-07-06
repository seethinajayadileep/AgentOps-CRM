package com.agentopscrm.repository;

import com.agentopscrm.entity.Lead;
import com.agentopscrm.entity.enums.LeadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Lead entity.
 *
 * @author AgentOps Team
 * @version 0.2.0
 */
@Repository
public interface LeadRepository extends JpaRepository<Lead, UUID> {

    List<Lead> findByBusinessId(UUID businessId);

    Page<Lead> findByBusinessId(UUID businessId, Pageable pageable);

    List<Lead> findByBusinessIdAndStatus(UUID businessId, LeadStatus status);

    Page<Lead> findByBusinessIdAndStatus(UUID businessId, LeadStatus status, Pageable pageable);

    Optional<Lead> findByEmail(String email);

    @Query("SELECT l FROM Lead l WHERE l.email = :email AND l.business.id = :businessId")
    Optional<Lead> findByEmailAndBusiness(@Param("email") String email, @Param("businessId") UUID businessId);

    @Query("SELECT l FROM Lead l WHERE l.business.id = :businessId AND l.leadScore >= :minScore ORDER BY l.leadScore DESC")
    List<Lead> findByBusinessIdAndMinLeadScore(@Param("businessId") UUID businessId, @Param("minScore") BigDecimal minScore);

    @Query("SELECT l FROM Lead l WHERE l.business.id = :businessId AND l.status IN :statuses")
    Page<Lead> findByBusinessIdAndStatusIn(@Param("businessId") UUID businessId, @Param("statuses") List<LeadStatus> statuses, Pageable pageable);

    long countByBusinessId(UUID businessId);

    long countByBusinessIdAndStatus(UUID businessId, LeadStatus status);

    @Query("SELECT AVG(l.leadScore) FROM Lead l WHERE l.business.id = :businessId")
    Double getAverageLeadScore(@Param("businessId") UUID businessId);

    Optional<Lead> findByConversationId(UUID conversationId);

    // --- Duplicate detection for outbound imports (F-010 Apify Lead Finder) ---

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);

    @Query("SELECT COUNT(l) > 0 FROM Lead l WHERE LOWER(l.name) = LOWER(:name) AND l.business.id = :businessId")
    boolean existsByNameAndBusiness(@Param("name") String name, @Param("businessId") UUID businessId);
}