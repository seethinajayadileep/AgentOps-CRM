package com.agentopscrm.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for conversation messages.
 *
 * @author AgentOps Team
 * @version 0.3.0
 */
public class ConversationMessageResponse {
    private UUID id;
    private String role;
    private String content;
    private LocalDateTime createdAt;

    public ConversationMessageResponse() {
    }

    public ConversationMessageResponse(UUID id, String role, String content, LocalDateTime createdAt) {
        this.id = id;
        this.role = role;
        this.content = content;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
