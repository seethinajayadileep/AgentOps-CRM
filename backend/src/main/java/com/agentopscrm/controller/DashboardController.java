package com.agentopscrm.controller;

import com.agentopscrm.entity.AgentLog;
import com.agentopscrm.entity.enums.ApprovalStatus;
import com.agentopscrm.repository.AgentLogRepository;
import com.agentopscrm.repository.ApprovalRepository;
import com.agentopscrm.repository.BusinessRepository;
import com.agentopscrm.repository.ConversationRepository;
import com.agentopscrm.repository.LeadRepository;
import com.agentopscrm.repository.VoiceCallRepository;
import com.agentopscrm.service.DashboardStatsCache;
import com.agentopscrm.util.TimestampJson;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
    private final DashboardStatsCache dashboardStatsCache;

    public DashboardController(
        BusinessRepository businessRepository,
        LeadRepository leadRepository,
        ConversationRepository conversationRepository,
        VoiceCallRepository voiceCallRepository,
        ApprovalRepository approvalRepository,
        AgentLogRepository agentLogRepository,
        DashboardStatsCache dashboardStatsCache
    ) {
        this.businessRepository = businessRepository;
        this.leadRepository = leadRepository;
        this.conversationRepository = conversationRepository;
        this.voiceCallRepository = voiceCallRepository;
        this.approvalRepository = approvalRepository;
        this.agentLogRepository = agentLogRepository;
        this.dashboardStatsCache = dashboardStatsCache;
    }

    /**
     * Get live dashboard statistics and recent activity.
     *
     * GET /api/dashboard/stats
     */
    @GetMapping("/stats")
    @Transactional(readOnly = true)
    public ResponseEntity<DashboardStats> getStats() {
        var cached = dashboardStatsCache.get();
        if (cached.isPresent()) {
            return ResponseEntity.ok(cached.get());
        }

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

        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfWeek = LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime startOfYesterday = LocalDate.now().minusDays(1).atStartOfDay();
        LocalDateTime endOfYesterday = startOfDay.minusNanos(1);

        long businessesPrevious = businessRepository.countByCreatedAtBefore(startOfMonth);
        long leadsPrevious = leadRepository.countByCreatedAtBefore(startOfMonth);
        long conversationsPrevious = conversationRepository.countByCreatedAtBefore(startOfWeek);
        long voicePrevious = voiceCallRepository.countByCreatedAtBefore(startOfWeek);
        long actionsYesterday = agentLogRepository.countByCreatedAtBetween(startOfYesterday, endOfYesterday);

        stats.businessesTrend = trend(stats.activeBusinesses, businessesPrevious, "last month");
        stats.leadsTrend = trend(stats.totalLeads, leadsPrevious, "last month");
        stats.conversationsTrend = trend(stats.conversations, conversationsPrevious, "last week");
        stats.voiceCallsTrend = trend(stats.voiceCalls, voicePrevious, "last week");
        stats.agentActionsTrend = trend(stats.agentActionsToday, actionsYesterday, "yesterday");
        stats.pendingApprovalsTrend = stats.pendingApprovals > 0
                ? new Trend("alert", stats.pendingApprovals + " awaiting action")
                : new Trend("flat", "None awaiting");

        // Recent activity feed from the latest agent logs.
        List<ActivityItem> activity = new ArrayList<>();
        for (AgentLog log : agentLogRepository.findTop8ByOrderByCreatedAtDesc()) {
            ActivityItem item = new ActivityItem();
            item.agentName = log.getAgentName();
            item.action = log.getAction();
            item.status = log.getStatus() != null ? log.getStatus().name() : null;
            item.createdAt = TimestampJson.toIsoInstant(log.getCreatedAt());
            activity.add(item);
        }
        stats.recentActivity = activity;
        dashboardStatsCache.put(stats);

        return ResponseEntity.ok(stats);
    }

    /**
     * Aggregated dashboard statistics response.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static class DashboardStats {
        public long activeBusinesses;
        public long totalLeads;
        public long conversations;
        public long voiceCalls;
        public long pendingApprovals;
        public long agentActionsToday;
        public Trend businessesTrend;
        public Trend leadsTrend;
        public Trend conversationsTrend;
        public Trend voiceCallsTrend;
        public Trend pendingApprovalsTrend;
        public Trend agentActionsTrend;
        public List<ActivityItem> recentActivity = new ArrayList<>();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static class Trend {
        public String direction;
        public String label;

        public Trend() {
        }

        public Trend(String direction, String label) {
            this.direction = direction;
            this.label = label;
        }
    }

    static Trend trend(long current, long previous, String period) {
        if (previous <= 0 && current <= 0) {
            return new Trend("flat", "No change");
        }
        if (previous <= 0) {
            return new Trend("up", "New this period");
        }
        long delta = current - previous;
        long pct = Math.round((100.0 * delta) / previous);
        if (pct == 0) {
            return new Trend("flat", "No change vs " + period);
        }
        String dir = pct > 0 ? "up" : "down";
        return new Trend(dir, Math.abs(pct) + "% vs " + period);
    }

    /**
     * A single recent-activity entry derived from an agent log.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static class ActivityItem {
        public String agentName;
        public String action;
        public String status;
        public String createdAt;
    }
}
