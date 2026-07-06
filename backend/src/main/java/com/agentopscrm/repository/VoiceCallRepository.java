package com.agentopscrm.repository;

import com.agentopscrm.entity.VoiceCall;
import com.agentopscrm.entity.enums.VoiceCallStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for VoiceCall entity.
 *
 * @author AgentOps Team
 * @version 0.2.0
 */
@Repository
public interface VoiceCallRepository extends JpaRepository<VoiceCall, UUID> {

    List<VoiceCall> findByBusinessId(UUID businessId);

    Page<VoiceCall> findByBusinessId(UUID businessId, Pageable pageable);

    List<VoiceCall> findByLeadId(UUID leadId);

    Page<VoiceCall> findByLeadId(UUID leadId, Pageable pageable);

    @Query("SELECT v FROM VoiceCall v WHERE v.business.id = :businessId ORDER BY v.createdAt DESC")
    Page<VoiceCall> findByBusinessIdOrderByCreatedAtDesc(@Param("businessId") UUID businessId, Pageable pageable);

    @Query("SELECT v FROM VoiceCall v WHERE v.lead.id = :leadId ORDER BY v.createdAt DESC")
    Page<VoiceCall> findByLeadIdOrderByCreatedAtDesc(@Param("leadId") UUID leadId, Pageable pageable);

    Page<VoiceCall> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<VoiceCall> findByConversationId(UUID conversationId, Pageable pageable);

    Page<VoiceCall> findByStatus(VoiceCallStatus status, Pageable pageable);

    Optional<VoiceCall> findByVapiCallId(String vapiCallId);

    @Query("SELECT v FROM VoiceCall v WHERE v.phoneNumber = :phoneNumber ORDER BY v.createdAt DESC")
    List<VoiceCall> findByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    @Query("SELECT v FROM VoiceCall v WHERE v.startedAt BETWEEN :startDate AND :endDate")
    Page<VoiceCall> findByStartedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);

    @Query("SELECT v FROM VoiceCall v WHERE v.business.id = :businessId AND v.startedAt BETWEEN :startDate AND :endDate")
    Page<VoiceCall> findByBusinessIdAndStartedAtBetween(@Param("businessId") UUID businessId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);

    long countByBusinessId(UUID businessId);

    long countByStatus(VoiceCallStatus status);

    @Query("SELECT AVG(v.durationSeconds) FROM VoiceCall v WHERE v.business.id = :businessId AND v.status = 'COMPLETED'")
    Double getAverageDuration(@Param("businessId") UUID businessId);
}