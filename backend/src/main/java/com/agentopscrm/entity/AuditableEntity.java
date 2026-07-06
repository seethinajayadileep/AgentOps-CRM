package com.agentopscrm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;

/**
 * Base entity with audit fields (created_at and updated_at).
 *
 * @author AgentOps Team
 * @version 0.2.0
 */
@MappedSuperclass
public abstract class AuditableEntity extends BaseEntity {

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public AuditableEntity() {
        super();
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public void setCreatedAt(LocalDateTime createdAt) {
        super.setCreatedAt(createdAt);
        if (this.updatedAt == null) {
            this.updatedAt = createdAt;
        }
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}