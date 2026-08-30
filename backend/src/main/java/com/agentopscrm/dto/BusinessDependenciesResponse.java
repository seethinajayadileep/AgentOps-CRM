package com.agentopscrm.dto;

/**
 * Counts of records that will be removed when a business is deleted.
 */
public class BusinessDependenciesResponse {
    private String businessId;
    private String businessName;
    private long leads;
    private long conversations;
    private long messages;
    private long documents;
    private long knowledgeChunks;
    private long knowledgeBaseJobs;
    private long approvals;
    private long agentLogs;
    private long voiceCalls;

    public long getTotal() {
        return leads + conversations + messages + documents + knowledgeChunks
                + knowledgeBaseJobs + approvals + agentLogs + voiceCalls;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public long getLeads() {
        return leads;
    }

    public void setLeads(long leads) {
        this.leads = leads;
    }

    public long getConversations() {
        return conversations;
    }

    public void setConversations(long conversations) {
        this.conversations = conversations;
    }

    public long getMessages() {
        return messages;
    }

    public void setMessages(long messages) {
        this.messages = messages;
    }

    public long getDocuments() {
        return documents;
    }

    public void setDocuments(long documents) {
        this.documents = documents;
    }

    public long getKnowledgeChunks() {
        return knowledgeChunks;
    }

    public void setKnowledgeChunks(long knowledgeChunks) {
        this.knowledgeChunks = knowledgeChunks;
    }

    public long getKnowledgeBaseJobs() {
        return knowledgeBaseJobs;
    }

    public void setKnowledgeBaseJobs(long knowledgeBaseJobs) {
        this.knowledgeBaseJobs = knowledgeBaseJobs;
    }

    public long getApprovals() {
        return approvals;
    }

    public void setApprovals(long approvals) {
        this.approvals = approvals;
    }

    public long getAgentLogs() {
        return agentLogs;
    }

    public void setAgentLogs(long agentLogs) {
        this.agentLogs = agentLogs;
    }

    public long getVoiceCalls() {
        return voiceCalls;
    }

    public void setVoiceCalls(long voiceCalls) {
        this.voiceCalls = voiceCalls;
    }
}
