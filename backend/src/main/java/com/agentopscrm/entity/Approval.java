package com.agentopscrm.entity;

import com.agentopscrm.entity.enums.ApprovalStatus;
import com.agentopscrm.entity.enums.ApprovalType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Approval entity representing an approval request.
 *
 * Why exists: Workflow control for sensitive actions like voice calls and
 * follow-up messages that require human review before execution.
 *
 * @author AgentOps Team
 * @version 0.2.0
 */
@Entity
@Table(name = "approvals", indexes = {
    @Index(name = "idx_approvals_business_id", columnList = "business_id"),
    @Index(name = "idx_approvals_lead_id", columnList = "lead_id"),
    @Index(name = "idx_approvals_status", columnList = "status"),
    @Index(name = "idx_approvals_created_at", columnList = "created_at")
})
public class Approval extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_type", nullable = false, length = 30)
    private ApprovalType approvalType;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "style", length = 50)
    private String style; // PROFESSIONAL, FRIENDLY, SHORT_WHATSAPP

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Column(name = "reviewed_by", length = 255)
    private String reviewedBy;

    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    public Approval() {
        super();
    }

    public Approval(UUID id) {
        super();
        this.id = id;
    }

    public Business getBusiness() {
        return business;
    }

    public void setBusiness(Business business) {
        this.business = business;
    }

    public Lead getLead() {
        return lead;
    }

    public void setLead(Lead lead) {
        this.lead = lead;
    }

    public ApprovalType getApprovalType() {
        return approvalType;
    }

    public void setApprovalType(ApprovalType approvalType) {
        this.approvalType = approvalType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public ApprovalStatus getStatus() {
        return status;
    }

    public void setStatus(ApprovalStatus status) {
        this.status = status;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }
}