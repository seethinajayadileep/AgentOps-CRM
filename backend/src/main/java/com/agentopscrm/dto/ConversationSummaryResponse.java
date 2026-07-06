package com.agentopscrm.dto;

/**
 * Response DTO for conversation summary statistics.
 *
 * @author AgentOps Team
 * @version 0.3.0
 */
public class ConversationSummaryResponse {
    private long totalConversations;
    private long activeConversations;
    private long conversationsToday;
    private long leadsCaptured;
    private double averageMessagesPerConversation;

    public ConversationSummaryResponse() {
    }

    public ConversationSummaryResponse(
            long totalConversations,
            long activeConversations,
            long conversationsToday,
            long leadsCaptured,
            double averageMessagesPerConversation) {
        this.totalConversations = totalConversations;
        this.activeConversations = activeConversations;
        this.conversationsToday = conversationsToday;
        this.leadsCaptured = leadsCaptured;
        this.averageMessagesPerConversation = averageMessagesPerConversation;
    }

    public long getTotalConversations() {
        return totalConversations;
    }

    public void setTotalConversations(long totalConversations) {
        this.totalConversations = totalConversations;
    }

    public long getActiveConversations() {
        return activeConversations;
    }

    public void setActiveConversations(long activeConversations) {
        this.activeConversations = activeConversations;
    }

    public long getConversationsToday() {
        return conversationsToday;
    }

    public void setConversationsToday(long conversationsToday) {
        this.conversationsToday = conversationsToday;
    }

    public long getLeadsCaptured() {
        return leadsCaptured;
    }

    public void setLeadsCaptured(long leadsCaptured) {
        this.leadsCaptured = leadsCaptured;
    }

    public double getAverageMessagesPerConversation() {
        return averageMessagesPerConversation;
    }

    public void setAverageMessagesPerConversation(double averageMessagesPerConversation) {
        this.averageMessagesPerConversation = averageMessagesPerConversation;
    }
}
