package com.agentopscrm.repository;

import com.agentopscrm.entity.Approval;
import com.agentopscrm.entity.enums.ApprovalStatus;
import com.agentopscrm.entity.enums.ApprovalType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Approval entity.
 *
 * @author AgentOps Team
 * @version 0.2.0
 */
@Repository
public interface ApprovalRepository extends JpaRepository<Approval, UUID> {

    List<Approval> findByBusinessId(UUID businessId);

    Page<Approval> findByBusinessId(UUID businessId, Pageable pageable);

    List<Approval> findByLeadId(UUID leadId);

    Page<Approval> findAllByLeadId(UUID leadId, Pageable pageable);

    Page<Approval> findAllByBusinessId(UUID businessId, Pageable pageable);

    Page<Approval> findAllByBusinessIdAndStatus(UUID businessId, ApprovalStatus status, Pageable pageable);

    Page<Approval> findAllByBusinessIdAndApprovalType(UUID businessId, ApprovalType type, Pageable pageable);

    Page<Approval> findAllByBusinessIdAndApprovalTypeAndStatus(UUID businessId, ApprovalType type, ApprovalStatus status, Pageable pageable);

    Page<Approval> findAllByBusinessIdAndLeadIdAndApprovalTypeAndStatus(UUID businessId, UUID leadId, ApprovalType type, ApprovalStatus status, Pageable pageable);

    Page<Approval> findByStatus(ApprovalStatus status, Pageable pageable);

    Page<Approval> findByBusinessIdAndStatus(UUID businessId, ApprovalStatus status, Pageable pageable);

    Page<Approval> findByApprovalType(ApprovalType approvalType, Pageable pageable);

    @Query("SELECT a FROM Approval a WHERE a.status = :status ORDER BY a.createdAt ASC")
    Page<Approval> findByStatusOrderByCreatedAtAsc(@Param("status") ApprovalStatus status, Pageable pageable);

    @Query("SELECT a FROM Approval a WHERE a.business.id = :businessId AND a.status = :status ORDER BY a.createdAt ASC")
    Page<Approval> findByBusinessIdAndStatusOrderByCreatedAtAsc(@Param("businessId") UUID businessId, @Param("status") ApprovalStatus status, Pageable pageable);

    long countByBusinessId(UUID businessId);

    long countByStatus(ApprovalStatus status);

    @Query("SELECT COUNT(a) FROM Approval a WHERE a.lead.id = :leadId AND a.status = :status")
    long countByLeadIdAndStatus(@Param("leadId") UUID leadId, @Param("status") ApprovalStatus status);
}