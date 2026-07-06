package com.agentopscrm.repository;

import com.agentopscrm.entity.Message;
import com.agentopscrm.entity.enums.MessageRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Message entity.
 *
 * @author AgentOps Team
 * @version 0.2.0
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByConversationId(UUID conversationId);

    Page<Message> findByConversationId(UUID conversationId, Pageable pageable);

    List<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId ORDER BY m.createdAt DESC")
    Page<Message> findLatestMessages(@Param("conversationId") UUID conversationId, Pageable pageable);

    long countByConversationId(UUID conversationId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :conversationId AND m.role = :role")
    long countByConversationIdAndRole(@Param("conversationId") UUID conversationId, @Param("role") MessageRole role);
}