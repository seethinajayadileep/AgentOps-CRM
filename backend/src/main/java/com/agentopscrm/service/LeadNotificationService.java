package com.agentopscrm.service;

import com.agentopscrm.client.CalComClient;
import com.agentopscrm.client.SlackWebhookClient;
import com.agentopscrm.client.TelegramClient;
import com.agentopscrm.entity.AgentLog;
import com.agentopscrm.entity.Business;
import com.agentopscrm.entity.Lead;
import com.agentopscrm.entity.enums.AgentActionStatus;
import com.agentopscrm.repository.AgentLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Pings Telegram and/or Slack when a CRM lead is first created. Cal.com is not a
 * chat channel — its booking URL is appended to those messages when configured.
 * HTTP runs after the lead-create transaction commits.
 */
@Service
public class LeadNotificationService {

    private static final Logger log = LoggerFactory.getLogger(LeadNotificationService.class);
    public static final String AGENT_NAME = "LeadNotificationService";
    public static final String ACTION_NOTIFIED = "FRESH_LEAD_NOTIFIED";
    public static final String ACTION_FAILED = "FRESH_LEAD_NOTIFY_FAILED";

    private final TelegramClient telegramClient;
    private final SlackWebhookClient slackWebhookClient;
    private final CalComClient calComClient;
    private final AgentLogRepository agentLogRepository;
    private final String frontendBaseUrl;

    public LeadNotificationService(
            TelegramClient telegramClient,
            SlackWebhookClient slackWebhookClient,
            CalComClient calComClient,
            AgentLogRepository agentLogRepository,
            @Value("${app.frontend-base-url:http://localhost:5173}") String frontendBaseUrl) {
        this.telegramClient = telegramClient;
        this.slackWebhookClient = slackWebhookClient;
        this.calComClient = calComClient;
        this.agentLogRepository = agentLogRepository;
        this.frontendBaseUrl = frontendBaseUrl == null ? "" : frontendBaseUrl.trim().replaceAll("/+$", "");
    }

    public void scheduleFreshLeadNotice(Lead lead, String source) {
        if (!telegramClient.isConfigured() && !slackWebhookClient.isConfigured()) {
            return;
        }
        if (lead == null || lead.getId() == null) {
            return;
        }
        FreshLeadNotice notice = FreshLeadNotice.from(lead, source, calComClient.getBookingUrl(), frontendBaseUrl);
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch(notice);
                }
            });
        } else {
            dispatch(notice);
        }
    }

    void dispatch(FreshLeadNotice notice) {
        long start = System.currentTimeMillis();
        List<String> sent = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        String text = notice.plainText();
        if (telegramClient.isConfigured()) {
            try {
                telegramClient.sendMessage(text);
                sent.add("Telegram");
            } catch (Exception e) {
                failed.add("Telegram");
                log.warn("Telegram fresh-lead notify failed: {}", e.getMessage());
            }
        }
        if (slackWebhookClient.isConfigured()) {
            try {
                slackWebhookClient.sendMessage(text);
                sent.add("Slack");
            } catch (Exception e) {
                failed.add("Slack");
                log.warn("Slack fresh-lead notify failed: {}", e.getMessage());
            }
        }
        long duration = System.currentTimeMillis() - start;
        persistLog(notice, sent, failed, duration);
    }

    private void persistLog(FreshLeadNotice notice, List<String> sent, List<String> failed, long durationMs) {
        try {
            AgentLog entry = new AgentLog();
            if (notice.businessId() != null) {
                entry.setBusiness(new Business(notice.businessId()));
            }
            entry.setLead(new Lead(notice.leadId()));
            entry.setAgentName(AGENT_NAME);
            if (failed.isEmpty() && !sent.isEmpty()) {
                entry.setAction(ACTION_NOTIFIED);
                entry.setStatus(AgentActionStatus.SUCCESS);
                entry.setOutputJson("{\"channels\":" + toJsonArray(sent) + "}");
            } else if (!sent.isEmpty()) {
                entry.setAction(ACTION_NOTIFIED);
                entry.setStatus(AgentActionStatus.PARTIAL);
                entry.setOutputJson("{\"sent\":" + toJsonArray(sent) + ",\"failed\":" + toJsonArray(failed) + "}");
                entry.setErrorMessage("Some channels failed: " + String.join(", ", failed));
            } else {
                entry.setAction(ACTION_FAILED);
                entry.setStatus(AgentActionStatus.FAILED);
                entry.setErrorMessage(failed.isEmpty() ? "No notification channel sent" : String.join(", ", failed));
            }
            entry.setDurationMs(durationMs);
            agentLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("Could not persist fresh-lead notification log: {}", e.getMessage());
        }
    }

    private static String toJsonArray(List<String> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(values.get(i)).append('"');
        }
        return sb.append(']').toString();
    }

    record FreshLeadNotice(
            UUID leadId,
            UUID businessId,
            String businessName,
            String leadName,
            String email,
            String phone,
            String status,
            String score,
            String source,
            String openUrl,
            String bookingUrl) {

        static FreshLeadNotice from(Lead lead, String source, String bookingUrl, String frontendBaseUrl) {
            UUID businessId = null;
            String businessName = "";
            try {
                if (lead.getBusiness() != null) {
                    businessId = lead.getBusiness().getId();
                    String name = lead.getBusiness().getName();
                    businessName = name == null ? "" : name;
                }
            } catch (RuntimeException e) {
                log.debug("Could not read business on lead {}", lead.getId());
            }
            String openUrl = "";
            if (frontendBaseUrl != null && !frontendBaseUrl.isBlank() && lead.getId() != null) {
                openUrl = frontendBaseUrl + "/leads/" + lead.getId();
            }
            String score = lead.getLeadScore() == null ? "" : lead.getLeadScore().stripTrailingZeros().toPlainString();
            String status = lead.getStatus() == null ? "" : lead.getStatus().name();
            return new FreshLeadNotice(
                    lead.getId(),
                    businessId,
                    businessName,
                    blankToDash(lead.getName()),
                    blankToDash(lead.getEmail()),
                    blankToDash(lead.getPhone()),
                    status,
                    score,
                    source == null || source.isBlank() ? "CRM" : source,
                    openUrl,
                    bookingUrl);
        }

        String plainText() {
            StringBuilder sb = new StringBuilder();
            sb.append("New lead");
            if (businessName != null && !businessName.isBlank()) {
                sb.append(" · ").append(businessName);
            }
            sb.append('\n');
            sb.append("Name: ").append(leadName).append('\n');
            sb.append("Email: ").append(email).append('\n');
            sb.append("Phone: ").append(phone).append('\n');
            if (status != null && !status.isBlank()) {
                sb.append("Status: ").append(status).append('\n');
            }
            if (score != null && !score.isBlank()) {
                sb.append("Score: ").append(score).append('\n');
            }
            sb.append("Source: ").append(source).append('\n');
            if (openUrl != null && !openUrl.isBlank()) {
                sb.append("Open: ").append(openUrl).append('\n');
            }
            if (bookingUrl != null && !bookingUrl.isBlank()) {
                sb.append("Book a call: ").append(bookingUrl);
            }
            return sb.toString().trim();
        }

        private static String blankToDash(String value) {
            return value == null || value.isBlank() ? "—" : value.trim();
        }
    }
}
