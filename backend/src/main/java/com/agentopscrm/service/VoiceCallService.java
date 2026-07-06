package com.agentopscrm.service;

import com.agentopscrm.client.VapiClient;
import com.agentopscrm.dto.VapiWebhookEvent;
import com.agentopscrm.dto.VoiceCallResponse;
import com.agentopscrm.dto.VoiceCallStartRequest;
import com.agentopscrm.entity.Business;
import com.agentopscrm.entity.Lead;
import com.agentopscrm.entity.VoiceCall;
import com.agentopscrm.entity.enums.CallOutcome;
import com.agentopscrm.entity.enums.VoiceCallStatus;
import com.agentopscrm.repository.LeadRepository;
import com.agentopscrm.repository.VoiceCallRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/**
 * Service for managing voice calls via Vapi integration.
 * 
 * Why exists: Handles voice call lifecycle including starting calls, processing webhooks,
 * and integrating with AgentLog for tracking.
 * 
 * @author AgentOps Team
 * @version 0.2.0
 */
@Service
public class VoiceCallService {

    private static final Logger logger = LoggerFactory.getLogger(VoiceCallService.class);

    private final VoiceCallRepository voiceCallRepository;
    private final LeadRepository leadRepository;
    private final VapiClient vapiClient;
    private final String assistantId;
    private final String phoneNumberId;

    public VoiceCallService(
        VoiceCallRepository voiceCallRepository,
        LeadRepository leadRepository,
        VapiClient vapiClient,
        @Value("${vapi.assistant-id}") String assistantId,
        @Value("${vapi.phone-number-id}") String phoneNumberId
    ) {
        this.voiceCallRepository = voiceCallRepository;
        this.leadRepository = leadRepository;
        this.vapiClient = vapiClient;
        this.assistantId = assistantId;
        this.phoneNumberId = phoneNumberId;
    }

    /**
     * Start a new voice call for a lead.
     *
     * @param leadId the ID of the lead
     * @param request the call start request
     * @return the created voice call response
     * @throws IllegalArgumentException if lead not found
     * @throws RuntimeException if Vapi call fails
     */
    @Transactional
    public VoiceCallResponse startCall(UUID leadId, VoiceCallStartRequest request) {
        logger.info("Starting voice call for lead ID: {}", leadId);

        VoiceCall voiceCall = null;

        try {
            // Fetch lead and business
            Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found with ID: " + leadId));

            Business business = lead.getBusiness();

            // Format phone number with country code
            String formattedPhoneNumber = formatPhoneNumber(request.getPhoneNumber());

            // Create voice call entity
            voiceCall = new VoiceCall();
            voiceCall.setBusiness(business);
            voiceCall.setLead(lead);
            voiceCall.setPhoneNumber(formattedPhoneNumber);
            voiceCall.setStatus(VoiceCallStatus.PENDING);
            voiceCall.setProvider("vapi");
            
            // Save initial voice call record
            try {
                voiceCall = voiceCallRepository.save(voiceCall);
            } catch (Exception dbEx) {
                logger.error("Database error saving voice call record", dbEx);
                throw new RuntimeException("Failed to create voice call record. Please contact support if this persists.");
            }

            // Call Vapi API to initiate the phone call
            VapiClient.VapiCallRequest vapiRequest = new VapiClient.VapiCallRequest(
                formattedPhoneNumber,
                assistantId,
                phoneNumberId
            );
            
            VapiClient.VapiCallResponse vapiResponse = vapiClient.startCall(vapiRequest);

            // Update voice call with Vapi call ID
            voiceCall.setVapiCallId(vapiResponse.id);
            voiceCall.setStatus(VoiceCallStatus.STARTED);
            voiceCall.setStartedAt(LocalDateTime.now());
            voiceCall = voiceCallRepository.save(voiceCall);

            logger.info("Successfully started voice call with Vapi ID: {}", vapiResponse.id);

            return mapToResponse(voiceCall);

        } catch (VapiClient.VapiException e) {
            logger.error("Failed to start Vapi call", e);

            // Update voice call as failed if it was created
            if (voiceCall != null && voiceCall.getId() != null) {
                try {
                    voiceCall.setStatus(VoiceCallStatus.FAILED);
                    voiceCall.setOutcome(CallOutcome.FAILED);
                    voiceCall.setFailureReason(e.getMessage());
                    voiceCallRepository.save(voiceCall);
                } catch (Exception dbEx) {
                    logger.error("Failed to update voice call status after Vapi error", dbEx);
                }
            }

            throw new RuntimeException("Failed to start voice call: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request parameter", e);
            throw new RuntimeException("Invalid request: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            // Re-throw runtime exceptions with clean messages
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error starting voice call", e);
            throw new RuntimeException("An unexpected error occurred while starting the voice call. Please try again.");
        }
    }

    /**
     * Process a webhook event from Vapi.
     *
     * @param event the webhook event payload
     */
    @Transactional
    public void processWebhookEvent(VapiWebhookEvent event) {
        logger.info("Processing Vapi webhook event type: {}", event.getType());

        if (event.getCall() == null || event.getCall().getId() == null) {
            logger.warn("Webhook event missing call data");
            return;
        }

        String vapiCallId = event.getCall().getId();
        VoiceCall voiceCall = voiceCallRepository.findByVapiCallId(vapiCallId).orElse(null);

        if (voiceCall == null) {
            logger.warn("No voice call found for Vapi call ID: {}", vapiCallId);
            return;
        }

        // Update call status based on webhook type and data
        updateCallFromWebhook(voiceCall, event);
        voiceCallRepository.save(voiceCall);

        logger.info("Updated voice call {} from webhook", voiceCall.getId());
    }

    /**
     * Get all voice calls for a business.
     *
     * Any call that is still in a non-terminal state is refreshed directly from
     * the Vapi API first. This keeps the UI up to date even when Vapi webhooks
     * are not reaching this server (e.g. the public webhook URL is not
     * configured, or an ngrok tunnel URL changed after restart).
     *
     * @param businessId the ID of the business
     * @param pageable pagination parameters
     * @return page of voice call responses
     */
    @Transactional
    public Page<VoiceCallResponse> getCallsByBusiness(UUID businessId, Pageable pageable) {
        Page<VoiceCall> calls = voiceCallRepository.findByBusinessIdOrderByCreatedAtDesc(businessId, pageable);
        calls.getContent().forEach(this::syncFromVapiIfActive);
        return calls.map(this::mapToResponse);
    }

    /**
     * Get all voice calls across every business (used by the global Voice Calls
     * page, which is not scoped to a single business). Non-terminal calls are
     * refreshed from Vapi first, same as the per-business listing.
     *
     * @param pageable pagination parameters
     * @return page of voice call responses
     */
    @Transactional
    public Page<VoiceCallResponse> getAllCalls(Pageable pageable) {
        Page<VoiceCall> calls = voiceCallRepository.findAllByOrderByCreatedAtDesc(pageable);
        calls.getContent().forEach(this::syncFromVapiIfActive);
        return calls.map(this::mapToResponse);
    }

    /**
     * Get all voice calls for a lead.
     *
     * @param leadId the ID of the lead
     * @param pageable pagination parameters
     * @return page of voice call responses
     */
    @Transactional
    public Page<VoiceCallResponse> getCallsByLead(UUID leadId, Pageable pageable) {
        Page<VoiceCall> calls = voiceCallRepository.findByLeadIdOrderByCreatedAtDesc(leadId, pageable);
        calls.getContent().forEach(this::syncFromVapiIfActive);
        return calls.map(this::mapToResponse);
    }

    /**
     * Get a single voice call by ID.
     *
     * @param callId the ID of the call
     * @return the voice call response
     * @throws IllegalArgumentException if call not found
     */
    @Transactional
    public VoiceCallResponse getCall(UUID callId) {
        VoiceCall voiceCall = voiceCallRepository.findById(callId)
            .orElseThrow(() -> new IllegalArgumentException("Voice call not found with ID: " + callId));
        syncFromVapiIfActive(voiceCall);
        return mapToResponse(voiceCall);
    }

    /**
     * Refresh a single voice call's status/details directly from the Vapi API,
     * but only when the call is still active (non-terminal) and we have a Vapi
     * call ID to look up. Terminal calls are never re-fetched. Any failure is
     * swallowed so listing/fetching never breaks because of a Vapi hiccup.
     */
    private void syncFromVapiIfActive(VoiceCall voiceCall) {
        if (voiceCall == null
                || voiceCall.getVapiCallId() == null
                || voiceCall.getVapiCallId().isBlank()
                || isTerminalStatus(voiceCall.getStatus())) {
            return;
        }

        try {
            VapiClient.VapiCallResponse remote = vapiClient.getCall(voiceCall.getVapiCallId());
            if (remote == null) {
                return;
            }
            boolean changed = applyVapiResponse(voiceCall, remote);
            if (changed) {
                voiceCallRepository.save(voiceCall);
                logger.info("Synced voice call {} from Vapi (status={})", voiceCall.getId(), voiceCall.getStatus());
            }
        } catch (Exception e) {
            // Non-fatal: keep serving the last known state.
            logger.warn("Failed to sync voice call {} from Vapi: {}", voiceCall.getId(), e.getMessage());
        }
    }

    /**
     * Apply the fields from a Vapi GET /call response onto the stored VoiceCall.
     *
     * @return true if any field was updated
     */
    private boolean applyVapiResponse(VoiceCall voiceCall, VapiClient.VapiCallResponse remote) {
        boolean changed = false;

        if (remote.status != null) {
            VoiceCallStatus mapped = mapVapiStatus(remote.status);
            if (mapped != voiceCall.getStatus()) {
                voiceCall.setStatus(mapped);
                changed = true;
            }
        }

        // transcript (top-level or nested in artifact)
        String transcript = remote.transcript != null ? remote.transcript
            : (remote.artifact != null ? remote.artifact.transcript : null);
        if (transcript != null && !transcript.equals(voiceCall.getTranscript())) {
            voiceCall.setTranscript(transcript);
            changed = true;
        }

        // summary (top-level or nested in analysis)
        String summary = remote.summary != null ? remote.summary
            : (remote.analysis != null ? remote.analysis.summary : null);
        if (summary != null && !summary.equals(voiceCall.getSummary())) {
            voiceCall.setSummary(summary);
            changed = true;
        }

        // recording URL (top-level or nested in artifact)
        String recordingUrl = remote.recordingUrl != null ? remote.recordingUrl
            : (remote.artifact != null ? remote.artifact.recordingUrl : null);
        if (recordingUrl != null && !recordingUrl.equals(voiceCall.getRecordingUrl())) {
            voiceCall.setRecordingUrl(recordingUrl);
            changed = true;
        }

        if (remote.duration != null && !remote.duration.equals(voiceCall.getDurationSeconds())) {
            voiceCall.setDurationSeconds(remote.duration);
            changed = true;
        }

        if (remote.startedAt != null && voiceCall.getStartedAt() == null) {
            voiceCall.setStartedAt(parseDateTime(remote.startedAt));
            changed = true;
        }

        if (remote.endedAt != null && voiceCall.getEndedAt() == null) {
            voiceCall.setEndedAt(parseDateTime(remote.endedAt));
            changed = true;
        }

        // Determine outcome once the call has ended.
        if (remote.endedAt != null || isTerminalStatus(voiceCall.getStatus())) {
            if (remote.error != null) {
                voiceCall.setOutcome(CallOutcome.FAILED);
                voiceCall.setFailureReason(remote.error);
                changed = true;
            } else if (remote.endedReason != null) {
                voiceCall.setOutcome(mapEndedReasonToOutcome(remote.endedReason));
                changed = true;
            } else if (voiceCall.getStatus() == VoiceCallStatus.COMPLETED && voiceCall.getOutcome() == null) {
                voiceCall.setOutcome(CallOutcome.ANSWERED);
                changed = true;
            }
        }

        return changed;
    }

    /**
     * A terminal status will never change again, so we skip re-syncing it.
     */
    private boolean isTerminalStatus(VoiceCallStatus status) {
        if (status == null) {
            return false;
        }
        return switch (status) {
            case COMPLETED, FAILED, NO_ANSWER, BUSY, VOICEMAIL, CANCELLED -> true;
            default -> false;
        };
    }

    /**
     * Update voice call from webhook data.
     */
    private void updateCallFromWebhook(VoiceCall voiceCall, VapiWebhookEvent event) {
        VapiWebhookEvent.CallData callData = event.getCall();
        VapiWebhookEvent.MessageData messageData = event.getMessage();

        // Update status. Vapi sends the status on the call object for some
        // events, but on the message object (e.g. "ended") for status-update /
        // end-of-call-report events, so check both.
        String vapiStatus = null;
        if (callData != null && callData.getStatus() != null) {
            vapiStatus = callData.getStatus();
        } else if (messageData != null && messageData.getStatus() != null) {
            vapiStatus = messageData.getStatus();
        }
        if (vapiStatus != null) {
            voiceCall.setStatus(mapVapiStatus(vapiStatus));
        }

        // Update transcript - check call data first, then message, then artifact
        String transcript = null;
        if (callData != null && callData.getTranscript() != null) {
            transcript = callData.getTranscript();
        } else if (messageData != null) {
            if (messageData.getTranscript() != null) {
                transcript = messageData.getTranscript();
            } else if (messageData.getArtifact() != null && messageData.getArtifact().getTranscript() != null) {
                transcript = messageData.getArtifact().getTranscript();
            }
        }
        if (transcript != null) {
            voiceCall.setTranscript(transcript);
        }

        // Update summary
        String summary = null;
        if (callData != null && callData.getSummary() != null) {
            summary = callData.getSummary();
        } else if (messageData != null) {
            if (messageData.getSummary() != null) {
                summary = messageData.getSummary();
            } else if (messageData.getArtifact() != null && messageData.getArtifact().getSummary() != null) {
                summary = messageData.getArtifact().getSummary();
            }
        }
        if (summary != null) {
            voiceCall.setSummary(summary);
        }

        // Update recording URL
        String recordingUrl = null;
        if (callData != null && callData.getRecordingUrl() != null) {
            recordingUrl = callData.getRecordingUrl();
        } else if (messageData != null && messageData.getArtifact() != null && messageData.getArtifact().getRecordingUrl() != null) {
            recordingUrl = messageData.getArtifact().getRecordingUrl();
        }
        if (recordingUrl != null) {
            voiceCall.setRecordingUrl(recordingUrl);
        }

        // Update duration - check message first for durationSeconds field
        if (messageData != null && messageData.getDurationSeconds() != null) {
            voiceCall.setDurationSeconds(messageData.getDurationSeconds());
        } else if (callData != null && callData.getDuration() != null) {
            voiceCall.setDurationSeconds(callData.getDuration());
        }

        // Update started time
        String startedAt = (callData != null && callData.getStartedAt() != null) ? callData.getStartedAt() : 
                          (messageData != null ? messageData.getStartedAt() : null);
        if (startedAt != null && voiceCall.getStartedAt() == null) {
            voiceCall.setStartedAt(parseDateTime(startedAt));
        }

        // Update ended time and outcome
        String endedAt = (callData != null && callData.getEndedAt() != null) ? callData.getEndedAt() : 
                        (messageData != null ? messageData.getEndedAt() : null);
        String endedReason = (callData != null && callData.getEndedReason() != null) ? callData.getEndedReason() : 
                            (messageData != null ? messageData.getEndedReason() : null);
                            
        if (endedAt != null) {
            voiceCall.setEndedAt(parseDateTime(endedAt));
            
            // Determine outcome based on ended reason
            if (callData != null && callData.getError() != null) {
                voiceCall.setOutcome(CallOutcome.FAILED);
                voiceCall.setFailureReason(callData.getError());
            } else if (endedReason != null) {
                voiceCall.setOutcome(mapEndedReasonToOutcome(endedReason));
            } else if (voiceCall.getStatus() == VoiceCallStatus.COMPLETED) {
                voiceCall.setOutcome(CallOutcome.ANSWERED);
            }
        }
    }

    /**
     * Map Vapi status string to VoiceCallStatus enum.
     */
    private VoiceCallStatus mapVapiStatus(String vapiStatus) {
        return switch (vapiStatus.toLowerCase()) {
            case "queued", "scheduled" -> VoiceCallStatus.PENDING;
            case "ringing" -> VoiceCallStatus.STARTED;
            case "in-progress", "active", "forwarding" -> VoiceCallStatus.IN_PROGRESS;
            // Vapi reports finished calls as "ended" (with an endedReason);
            // treat both "ended" and "completed" as completed here. The precise
            // outcome (answered/no-answer/busy/etc.) is derived from endedReason.
            case "ended", "completed" -> VoiceCallStatus.COMPLETED;
            case "failed" -> VoiceCallStatus.FAILED;
            case "busy" -> VoiceCallStatus.BUSY;
            case "no-answer", "no_answer" -> VoiceCallStatus.NO_ANSWER;
            case "voicemail" -> VoiceCallStatus.VOICEMAIL;
            case "canceled", "cancelled" -> VoiceCallStatus.CANCELLED;
            default -> VoiceCallStatus.IN_PROGRESS;
        };
    }

    /**
     * Map Vapi ended reason to CallOutcome enum.
     */
    private CallOutcome mapEndedReasonToOutcome(String endedReason) {
        return switch (endedReason.toLowerCase()) {
            case "completed", "hangup" -> CallOutcome.ANSWERED;
            case "busy" -> CallOutcome.BUSY;
            case "no-answer", "no_answer" -> CallOutcome.NO_ANSWER;
            case "voicemail" -> CallOutcome.VOICEMAIL;
            case "failed", "error" -> CallOutcome.FAILED;
            default -> CallOutcome.ANSWERED;
        };
    }

    /**
     * Parse ISO 8601 datetime string.
     */
    private LocalDateTime parseDateTime(String dateTimeString) {
        try {
            return LocalDateTime.parse(dateTimeString, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            logger.warn("Failed to parse datetime: {}", dateTimeString);
            return LocalDateTime.now();
        }
    }

    /**
     * Map VoiceCall entity to VoiceCallResponse DTO.
     */
    private VoiceCallResponse mapToResponse(VoiceCall voiceCall) {
        VoiceCallResponse.Builder builder = new VoiceCallResponse.Builder()
            .id(voiceCall.getId())
            .phoneNumber(voiceCall.getPhoneNumber())
            .status(voiceCall.getStatus())
            .provider(voiceCall.getProvider())
            .outcome(voiceCall.getOutcome())
            .failureReason(voiceCall.getFailureReason())
            .vapiCallId(voiceCall.getVapiCallId())
            .transcript(voiceCall.getTranscript())
            .summary(voiceCall.getSummary())
            .recordingUrl(voiceCall.getRecordingUrl())
            .durationSeconds(voiceCall.getDurationSeconds())
            .startedAt(voiceCall.getStartedAt())
            .endedAt(voiceCall.getEndedAt())
            .createdAt(voiceCall.getCreatedAt());

        if (voiceCall.getLead() != null) {
            builder.leadId(voiceCall.getLead().getId());
            builder.leadName(voiceCall.getLead().getName());
        }

        return builder.build();
    }

    /**
     * Format phone number with +91 country code if not already present.
     *
     * @param phoneNumber the input phone number
     * @return formatted phone number with country code
     */
    private String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be empty");
        }

        String cleaned = phoneNumber.trim().replaceAll("[\\s\\-\\(\\)]", "");

        // If already starts with +, return as is
        if (cleaned.startsWith("+")) {
            return cleaned;
        }

        // If starts with 91, add + prefix
        if (cleaned.startsWith("91")) {
            return "+" + cleaned;
        }

        // If starts with 0, remove it and add +91
        if (cleaned.startsWith("0")) {
            cleaned = cleaned.substring(1);
        }

        // Add +91 prefix for Indian numbers
        return "+91" + cleaned;
    }
}
