package com.agentopscrm.repository;

import com.agentopscrm.entity.Conversation;
import com.agentopscrm.entity.enums.ConversationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Conversation entity.
 *
 * @author AgentOps Team
 * @version 0.2.0
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    List<Conversation> findByBusinessId(UUID businessId);

    Page<Conversation> findByBusinessId(UUID businessId, Pageable pageable);

    List<Conversation> findByBusinessIdAndStatus(UUID businessId, ConversationStatus status);

    Page<Conversation> findByBusinessIdAndStatus(UUID businessId, ConversationStatus status, Pageable pageable);

    Optional<Conversation> findByCustomerEmail(String customerEmail);

    @Query("SELECT c FROM Conversation c WHERE c.customerEmail = :email AND c.business.id = :businessId")
    Optional<Conversation> findByCustomerEmailAndBusiness(@Param("email") String email, @Param("businessId") UUID businessId);

    @Query("SELECT c FROM Conversation c WHERE c.business.id = :businessId AND c.status IN :statuses")
    Page<Conversation> findByBusinessIdAndStatusIn(@Param("businessId") UUID businessId, @Param("statuses") List<ConversationStatus> statuses, Pageable pageable);

    long countByBusinessId(UUID businessId);

    long countByBusinessIdAndStatus(UUID businessId, ConversationStatus status);

    long countByStatus(ConversationStatus status);
}