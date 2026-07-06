package com.agentopscrm.controller;

import com.agentopscrm.dto.EvaluationResponse;
import com.agentopscrm.entity.Message;
import com.agentopscrm.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private ChatService chatService;

    /**
     * POST /api/chat/ask
     * Process a customer question and generate AI response
     */
    @PostMapping("/ask")
    public ResponseEntity<AskResponse> ask(@RequestBody AskRequest request) {
        logger.info("POST /api/chat/ask - businessId: {}, conversationId: {}", 
                   request.businessId, request.conversationId);

        // Validate request
        if (request.businessId == null) {
            return ResponseEntity.badRequest().build();
        }
        if (request.question == null || request.question.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            // Process chat
            ChatService.ChatResult result = chatService.processChat(
                request.businessId,
                request.conversationId,
                request.question
            );

            // Build response
            AskResponse response = new AskResponse(
                result.getConversationId(),
                result.getAnswer(),
                result.getSources(),
                result.getConfidenceScore()
            );

            // Flag lead-capture interactions so the client can hide RAG
            // confidence/sources on data-collection prompts. leadId is only
            // populated once a lead has actually been created.
            if (result.getLeadId() != null || result.isLeadCaptureInProgress()) {
                response.setLeadDetected(true);
                response.setLeadId(result.getLeadId());
            }

            // Add F-008 evaluation summary if the answer was evaluated
            EvaluationResponse evaluation = result.getEvaluation();
            if (evaluation != null) {
                response.setEvaluation(new EvaluationSummary(
                    evaluation.getHallucinationRisk(),
                    evaluation.isSafeToSend(),
                    evaluation.getReason()
                ));
            }

            logger.info("Chat processed successfully - conversationId: {}, confidence: {}, leadDetected: {}, safeToSend: {}", 
                       result.getConversationId(), result.getConfidenceScore(), response.isLeadDetected(),
                       evaluation != null ? evaluation.isSafeToSend() : "n/a");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.error("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (ChatService.ChatException e) {
            logger.error("Chat service error: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            logger.error("Unexpected error processing chat", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/chat/conversations/{conversationId}/messages
     * Get conversation history
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ConversationHistoryResponse> getConversationHistory(
            @PathVariable UUID conversationId) {
        logger.info("GET /api/chat/conversations/{}/messages", conversationId);

        try {
            List<Message> messages = chatService.getConversationHistory(conversationId);
            
            List<MessageResponse> messageResponses = messages.stream()
                .map(m -> new MessageResponse(
                    m.getId(),
                    m.getRole().toString(),
                    m.getContent(),
                    m.getCreatedAt()
                ))
                .collect(Collectors.toList());

            ConversationHistoryResponse response = new ConversationHistoryResponse(
                conversationId,
                messageResponses
            );

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.error("Conversation not found: {}", conversationId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error retrieving conversation history", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Request DTO for POST /api/chat/ask
     */
    public static class AskRequest {
        private UUID businessId;
        private UUID conversationId;
        private String question;

        // Getters and setters
        public UUID getBusinessId() {
            return businessId;
        }

        public void setBusinessId(UUID businessId) {
            this.businessId = businessId;
        }

        public UUID getConversationId() {
            return conversationId;
        }

        public void setConversationId(UUID conversationId) {
            this.conversationId = conversationId;
        }

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }
    }

    /**
     * Response DTO for POST /api/chat/ask
     */
    public static class AskResponse {
        private final UUID conversationId;
        private final String answer;
        private final List<String> sources;
        private final int confidenceScore;
        private boolean leadDetected;
        private UUID leadId;
        private EvaluationSummary evaluation;

        public AskResponse(UUID conversationId, String answer, List<String> sources, int confidenceScore) {
            this.conversationId = conversationId;
            this.answer = answer;
            this.sources = sources;
            this.confidenceScore = confidenceScore;
            this.leadDetected = false;
            this.leadId = null;
            this.evaluation = null;
        }

        public UUID getConversationId() {
            return conversationId;
        }

        public String getAnswer() {
            return answer;
        }

        public List<String> getSources() {
            return sources;
        }

        public int getConfidenceScore() {
            return confidenceScore;
        }

        public boolean isLeadDetected() {
            return leadDetected;
        }

        public void setLeadDetected(boolean leadDetected) {
            this.leadDetected = leadDetected;
        }

        public UUID getLeadId() {
            return leadId;
        }

        public void setLeadId(UUID leadId) {
            this.leadId = leadId;
        }

        public EvaluationSummary getEvaluation() {
            return evaluation;
        }

        public void setEvaluation(EvaluationSummary evaluation) {
            this.evaluation = evaluation;
        }
    }

    /**
     * Nested evaluation summary exposed to the client (F-008).
     * Only customer-safe fields are exposed (never the internal prompt/system details).
     */
    public static class EvaluationSummary {
        private final String hallucinationRisk;
        private final boolean safeToSend;
        private final String reason;

        public EvaluationSummary(String hallucinationRisk, boolean safeToSend, String reason) {
            this.hallucinationRisk = hallucinationRisk;
            this.safeToSend = safeToSend;
            this.reason = reason;
        }

        public String getHallucinationRisk() {
            return hallucinationRisk;
        }

        public boolean isSafeToSend() {
            return safeToSend;
        }

        public String getReason() {
            return reason;
        }
    }

    /**
     * Response DTO for conversation history
     */
    public static class ConversationHistoryResponse {
        private final UUID conversationId;
        private final List<MessageResponse> messages;

        public ConversationHistoryResponse(UUID conversationId, List<MessageResponse> messages) {
            this.conversationId = conversationId;
            this.messages = messages;
        }

        public UUID getConversationId() {
            return conversationId;
        }

        public List<MessageResponse> getMessages() {
            return messages;
        }
    }

    /**
     * DTO for message in conversation history
     */
    public static class MessageResponse {
        private final UUID id;
        private final String role;
        private final String content;
        private final LocalDateTime createdAt;

        public MessageResponse(UUID id, String role, String content, LocalDateTime createdAt) {
            this.id = id;
            this.role = role;
            this.content = content;
            this.createdAt = createdAt;
        }

        public UUID getId() {
            return id;
        }

        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
    }
}
