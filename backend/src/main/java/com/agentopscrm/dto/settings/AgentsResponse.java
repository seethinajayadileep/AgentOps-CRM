package com.agentopscrm.dto.settings;

import java.util.List;

/**
 * Agents readiness and safety configuration response.
 *
 * @author AgentOps Team
 * @version 0.1.0
 */
public class AgentsResponse {
    private List<AgentStatus> agents;
    private SafetyConfig safetyConfig;

    public AgentsResponse() {
    }

    public AgentsResponse(List<AgentStatus> agents, SafetyConfig safetyConfig) {
        this.agents = agents;
        this.safetyConfig = safetyConfig;
    }

    public List<AgentStatus> getAgents() {
        return agents;
    }

    public void setAgents(List<AgentStatus> agents) {
        this.agents = agents;
    }

    public SafetyConfig getSafetyConfig() {
        return safetyConfig;
    }

    public void setSafetyConfig(SafetyConfig safetyConfig) {
        this.safetyConfig = safetyConfig;
    }

    public static class SafetyConfig {
        private boolean evaluationEnabled;
        private boolean unsafeAnswerBlocking;
        private boolean fallbackAnswerAvailable;
        private boolean humanApprovalEnabled;
        private boolean humanApprovalForVoice;
        private String leadCaptureBehavior;

        public SafetyConfig() {
        }

        // Getters and setters
        public boolean isEvaluationEnabled() {
            return evaluationEnabled;
        }

        public void setEvaluationEnabled(boolean evaluationEnabled) {
            this.evaluationEnabled = evaluationEnabled;
        }

        public boolean isUnsafeAnswerBlocking() {
            return unsafeAnswerBlocking;
        }

        public void setUnsafeAnswerBlocking(boolean unsafeAnswerBlocking) {
            this.unsafeAnswerBlocking = unsafeAnswerBlocking;
        }

        public boolean isFallbackAnswerAvailable() {
            return fallbackAnswerAvailable;
        }

        public void setFallbackAnswerAvailable(boolean fallbackAnswerAvailable) {
            this.fallbackAnswerAvailable = fallbackAnswerAvailable;
        }

        public boolean isHumanApprovalEnabled() {
            return humanApprovalEnabled;
        }

        public void setHumanApprovalEnabled(boolean humanApprovalEnabled) {
            this.humanApprovalEnabled = humanApprovalEnabled;
        }

        public boolean isHumanApprovalForVoice() {
            return humanApprovalForVoice;
        }

        public void setHumanApprovalForVoice(boolean humanApprovalForVoice) {
            this.humanApprovalForVoice = humanApprovalForVoice;
        }

        public String getLeadCaptureBehavior() {
            return leadCaptureBehavior;
        }

        public void setLeadCaptureBehavior(String leadCaptureBehavior) {
            this.leadCaptureBehavior = leadCaptureBehavior;
        }
    }
}
