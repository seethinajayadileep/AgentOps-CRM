package com.agentopscrm.controller;

import com.agentopscrm.entity.AgentLog;
import com.agentopscrm.entity.enums.ApprovalStatus;
import com.agentopscrm.repository.AgentLogRepository;
import com.agentopscrm.repository.ApprovalRepository;
import com.agentopscrm.repository.BusinessRepository;
import com.agentopscrm.repository.ConversationRepository;
import com.agentopscrm.repository.LeadRepository;
import com.agentopscrm.repository.VoiceCallRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * REST controller providing aggregated metrics for the main dashboard.
 *
 * Why exists: The dashboard overview needs live counts (businesses, leads,
 * conversations, voice calls, pending approvals, agent actions today) plus a
 * recent-activity feed, all in a single call so the UI stays simple and fast.
 *
 * @author AgentOps Team
 * @version 0.3.0
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    private final BusinessRepository businessRepository;
    private final LeadRepository leadRepository;
    private final ConversationRepository conversationRepository;
    private final VoiceCallRepository voiceCallRepository;
    private final ApprovalRepository approvalRepository;
    private final AgentLogRepository agentLogRepository;

    public DashboardController(
        BusinessRepository businessRepository,
        LeadRepository leadRepository,
        ConversationRepository conversationRepository,
        VoiceCallRepository voiceCallRepository,
        ApprovalRepository approvalRepository,
        AgentLogRepository agentLogRepository
    ) {
        this.businessRepository = businessRepository;
        this.leadRepository = leadRepository;
        this.conversationRepository = conversationRepository;
        this.voiceCallRepository = voiceCallRepository;
        this.approvalRepository = approvalRepository;
        this.agentLogRepository = agentLogRepository;
    }

    /**
     * Get live dashboard statistics and recent activity.
     *
     * GET /api/dashboard/stats
     */
    @GetMapping("/stats")
    @Transactional(readOnly = true)
    public ResponseEntity<DashboardStats> getStats() {
        logger.info("Fetching dashboard stats");

        DashboardStats stats = new DashboardStats();
        stats.activeBusinesses = businessRepository.count();
        stats.totalLeads = leadRepository.count();
        stats.conversations = conversationRepository.count();
        stats.voiceCalls = voiceCallRepository.count();
        stats.pendingApprovals = approvalRepository.countByStatus(ApprovalStatus.PENDING);

        // Agent actions logged since midnight today (server time).
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        stats.agentActionsToday = agentLogRepository.countByCreatedAtBetween(startOfDay, endOfDay);

        // Recent activity feed from the latest agent logs.
        List<ActivityItem> activity = new ArrayList<>();
        for (AgentLog log : agentLogRepository.findTop8ByOrderByCreatedAtDesc()) {
            ActivityItem item = new ActivityItem();
            item.agentName = log.getAgentName();
            item.action = log.getAction();
            item.status = log.getStatus() != null ? log.getStatus().name() : null;
            item.createdAt = log.getCreatedAt() != null ? log.getCreatedAt().toString() : null;
            activity.add(item);
        }
        stats.recentActivity = activity;

        return ResponseEntity.ok(stats);
    }

    /**
     * Aggregated dashboard statistics response.
     */
    public static class DashboardStats {
        public long activeBusinesses;
        public long totalLeads;
        public long conversations;
        public long voiceCalls;
        public long pendingApprovals;
        public long agentActionsToday;
        public List<ActivityItem> recentActivity = new ArrayList<>();
    }

    /**
     * A single recent-activity entry derived from an agent log.
     */
    public static class ActivityItem {
        public String agentName;
        public String action;
        public String status;
        public String createdAt;
    }
}
