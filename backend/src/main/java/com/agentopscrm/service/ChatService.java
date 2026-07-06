package com.agentopscrm.service;

import com.agentopscrm.agent.EvaluationAgent;
import com.agentopscrm.agent.LeadQualificationAgent;
import com.agentopscrm.dto.EvaluationRequest;
import com.agentopscrm.dto.EvaluationResponse;
import com.agentopscrm.dto.LeadQualificationRequest;
import com.agentopscrm.dto.LeadQualificationResponse;
import com.agentopscrm.entity.*;
import com.agentopscrm.entity.enums.AgentActionStatus;
import com.agentopscrm.entity.enums.Channel;
import com.agentopscrm.entity.enums.ConversationStatus;
import com.agentopscrm.entity.enums.MessageRole;
import com.agentopscrm.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    @Autowired
    private BusinessRepository businessRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private AgentLogRepository agentLogRepository;

    @Autowired
    private RagService ragService;

    @Autowired
    private AnswerService answerService;

    @Autowired
    private LeadQualificationAgent leadQualificationAgent;

    @Autowired
    private LeadQualificationService leadQualificationService;

    @Autowired
    private EvaluationService evaluationService;

    @Value("classpath:prompts/support-agent.md")
    private Resource supportAgentPrompt;

    private static final String NO_INFO_MESSAGE = 
        "I do not have confirmed information about that. Please share your contact details and our team will help you.";

    /**
     * Process a chat request and generate response
     */
    @Transactional
    public ChatResult processChat(UUID businessId, UUID conversationId, String question) {
        logger.info("Processing chat - businessId: {}, conversationId: {}", businessId, conversationId);

        // 1. Validate business exists
        Business business = businessRepository.findById(businessId)
            .orElseThrow(() -> new IllegalArgumentException("Business not found: " + businessId));

        // 2. Create or retrieve conversation
        Conversation conversation;
        if (conversationId == null) {
            conversation = createConversation(business);
            logger.info("Created new conversation: {}", conversation.getId());
        } else {
            conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));
            
            if (!conversation.getBusiness().getId().equals(businessId)) {
                throw new IllegalArgumentException("Conversation does not belong to business");
            }
        }

        // 3. Save user message
        Message userMessage = saveUserMessage(conversation, question);
        logger.info("Saved user message: {}", userMessage.getId());

        // 4. Check if it's a greeting - handle without RAG
        if (isGreeting(question)) {
            String greetingResponse = "Hi! I'm here to help answer questions about " + business.getName() + 
                                     ". Feel free to ask me anything about our services, products, or business!";
            Message assistantMessage = saveAssistantMessage(conversation, greetingResponse);
            logger.info("Handled greeting, saved assistant message: {}", assistantMessage.getId());
            
            saveAgentLog(conversation, business, question, greetingResponse, 100.0);
            conversationRepository.save(conversation);
            
            return new ChatResult(conversation.getId(), greetingResponse, List.of(), 100);
        }

        // 4b. Check if it's a simple acknowledgement / thanks (e.g. "ok thanks",
        //     "thank you", "cool"). Handle it directly WITHOUT running RAG or the
        //     lead-capture flow. Otherwise these short messages return the
        //     NO_INFO fallback, which was being (mis)interpreted as the agent
        //     asking for contact details and re-triggered lead capture - causing
        //     the agent to ask for the customer's details all over again after
        //     they had just been saved.
        if (isAcknowledgement(question)) {
            String ackResponse = "You're welcome! If there's anything else I can help you with, just let me know.";
            Message assistantMessage = saveAssistantMessage(conversation, ackResponse);
            logger.info("Handled acknowledgement, saved assistant message: {}", assistantMessage.getId());

            saveAgentLog(conversation, business, question, ackResponse, 100.0);
            conversationRepository.save(conversation);

            return new ChatResult(conversation.getId(), ackResponse, List.of(), 100);
        }

        // 5. Search RAG chunks for business questions
        RagService.SearchResult searchResult;
        try {
            searchResult = ragService.search(businessId, question, 5);
            logger.info("Found {} relevant chunks", searchResult.getResults().size());
        } catch (Exception e) {
            logger.error("RAG search failed", e);
            throw new ChatException("Failed to search knowledge base: " + e.getMessage());
        }

        // 6. Generate DRAFT answer using LLM
        String answer;
        double confidenceScore;
        List<String> sources;
        List<String> retrievedChunks = new ArrayList<>();

        // Use lower threshold (0.2 instead of 0.5) and always attempt if chunks exist
        if (searchResult.getResults().isEmpty()) {
            // No chunks found at all
            answer = NO_INFO_MESSAGE;
            confidenceScore = 0.0;
            sources = List.of();
            logger.info("No chunks found, using fallback message");
        } else {
            // Build context from chunks and let the LLM decide
            List<String> contextExcerpts = buildContextExcerpts(searchResult.getResults());
            retrievedChunks = extractChunkContents(searchResult.getResults());
            sources = extractSources(searchResult.getResults());
            
            try {
                // Generate answer - let LLM determine if context is sufficient
                String generatedAnswer = answerService.generateAnswer(question, contextExcerpts);
                
                answer = generatedAnswer;
                confidenceScore = getAverageScore(searchResult.getResults()) * 100; // Convert to percentage
                
                logger.info("Generated draft answer with confidence: {}", confidenceScore);
            } catch (Exception e) {
                logger.error("Answer generation failed", e);
                answer = NO_INFO_MESSAGE;
                confidenceScore = 0.0;
                sources = List.of();
                retrievedChunks = new ArrayList<>();
            }
        }

        // 6b. F-008 Evaluation Agent: verify the DRAFT answer is grounded and safe
        //     before it is ever saved/sent. If unsafe, replace with safe fallback.
        //     The plain NO_INFO_MESSAGE fallback is already safe, so we skip it.
        EvaluationResponse evaluation = null;
        if (!answer.equals(NO_INFO_MESSAGE)) {
            try {
                EvaluationRequest evalRequest = new EvaluationRequest(
                    businessId,
                    conversation.getId(),
                    question,
                    answer,
                    retrievedChunks,
                    sources
                );
                evaluation = evaluationService.evaluate(evalRequest);

                if (evaluation != null && !evaluation.isSafeToSend()) {
                    String fallback = evaluation.getFinalAnswer() != null
                        ? evaluation.getFinalAnswer()
                        : NO_INFO_MESSAGE;
                    logger.info("Evaluation blocked unsafe answer (risk={}): {}",
                        evaluation.getHallucinationRisk(), evaluation.getReason());
                    answer = fallback;
                }
            } catch (Exception e) {
                // Never break chat on evaluation failure; keep the draft as-is.
                logger.error("Evaluation step failed, sending draft answer", e);
            }
        }

        // 7. Multi-step lead capture flow. This may override the support answer
        //    with a capture prompt (asking for the missing name/contact) or a
        //    confirmation once a lead is created.
        //
        //    Detecting that the support answer itself asked for contact details
        //    (the NO_INFO fallback) lets us enter capture mode so the customer's
        //    NEXT reply (name/phone/email) is actually stored, instead of being
        //    treated as a brand-new knowledge-base question - which is what made
        //    the agent repeatedly ask for the same information.
        boolean assistantAskedForContact = answer.equals(NO_INFO_MESSAGE);
        UUID leadId = null;
        LeadCaptureOutcome captureOutcome = LeadCaptureOutcome.none();
        try {
            captureOutcome = handleLeadCapture(conversation, question, businessId, assistantAskedForContact);
            leadId = captureOutcome.getLeadId();
        } catch (Exception e) {
            // Don't fail the chat if lead qualification fails
            logger.error("Lead capture failed, continuing chat", e);
        }

        boolean leadCaptureInteraction = captureOutcome.getResponseMessage() != null || leadId != null;
        String finalAnswer = captureOutcome.getResponseMessage() != null
            ? captureOutcome.getResponseMessage()
            : answer;

        // During lead capture the message is a data-collection prompt, not a
        // knowledge-base answer, so we don't surface RAG confidence/sources/eval.
        List<String> finalSources = leadCaptureInteraction ? List.of() : sources;
        double finalConfidence = leadCaptureInteraction ? 0.0 : confidenceScore;
        EvaluationResponse finalEvaluation = leadCaptureInteraction ? null : evaluation;

        // 8. Save the single assistant message the customer will actually see.
        Message assistantMessage = saveAssistantMessage(conversation, finalAnswer);
        logger.info("Saved assistant message: {}", assistantMessage.getId());

        // 9. Save agent log + persist conversation state.
        saveAgentLog(conversation, business, question, finalAnswer, finalConfidence);
        conversationRepository.save(conversation);

        // 10. Build result.
        ChatResult result = new ChatResult(
            conversation.getId(),
            finalAnswer,
            finalSources,
            (int) Math.round(finalConfidence)
        );
        result.setLeadId(leadId);
        result.setEvaluation(finalEvaluation);
        result.setLeadCaptureInProgress(leadCaptureInteraction);

        return result;
    }

    /**
     * Check if the message is a greeting
     */
    private boolean isGreeting(String message) {
        String lower = message.toLowerCase().trim();
        return lower.matches("^(hi|hello|hey|greetings|good morning|good afternoon|good evening|howdy|sup|yo|hola)([\\s\\p{Punct}]*)$");
    }

    /**
     * Check if the message is a simple acknowledgement / thanks that does not
     * require a knowledge-base lookup or lead capture (e.g. "ok thanks",
     * "thank you", "great, thanks", "cool", "got it").
     */
    private boolean isAcknowledgement(String message) {
        String lower = message.toLowerCase().trim();
        // Optional leading filler ("ok", "okay", "great", "cool", "alright",
        // "perfect", "awesome", "sounds good", "got it") followed by a thanks
        // phrase, OR a standalone thanks/closing phrase.
        return lower.matches(
            "^(ok|okay|k|kk|great|cool|alright|nice|perfect|awesome|got it|sounds good|noted)?[\\s,\\.!]*"
            + "(thanks|thank you|thank u|thankyou|thx|ty|cheers|appreciate it|much appreciated|"
            + "bye|goodbye|see you|no thanks|no thank you|that's all|thats all|that is all|"
            + "great|cool|perfect|awesome|got it|sounds good|noted|okay|ok)"
            + "[\\s,\\.!]*$");
    }

    /**
     * Calculate average similarity score
     */
    private double getAverageScore(List<RagService.RagResult> results) {
        if (results.isEmpty()) {
            return 0.0;
        }
        double sum = results.stream()
            .mapToDouble(r -> r.getSimilarity() != null ? r.getSimilarity() : 0.0)
            .sum();
        return sum / results.size();
    }

    /**
     * Create a new conversation
     */
    private Conversation createConversation(Business business) {
        Conversation conversation = new Conversation();
        conversation.setBusiness(business);
        conversation.setChannel(Channel.WEB_WIDGET);
        conversation.setStatus(ConversationStatus.ACTIVE);
        return conversationRepository.save(conversation);
    }

    /**
     * Save user message
     */
    private Message saveUserMessage(Conversation conversation, String content) {
        Message message = new Message();
        message.setConversation(conversation);
        message.setRole(MessageRole.USER);
        message.setContent(content);
        return messageRepository.save(message);
    }

    /**
     * Save assistant message
     */
    private Message saveAssistantMessage(Conversation conversation, String content) {
        Message message = new Message();
        message.setConversation(conversation);
        message.setRole(MessageRole.ASSISTANT);
        message.setContent(content);
        return messageRepository.save(message);
    }

    /**
     * Save agent log
     */
    private void saveAgentLog(Conversation conversation, Business business, 
                             String question, String answer, double confidenceScore) {
        AgentLog log = new AgentLog();
        log.setConversation(conversation);
        log.setBusiness(business);
        log.setAgentName("SupportChatAgent");
        log.setAction("SUPPORT_CHAT");
        log.setInputJson(question);
        log.setOutputJson(answer);
        log.setStatus(AgentActionStatus.SUCCESS);
        agentLogRepository.save(log);
    }

    /**
     * Build context excerpts from RAG results
     */
    private List<String> buildContextExcerpts(List<RagService.RagResult> results) {
        return results.stream()
            .map(result -> String.format("[From %s]\n%s", result.getSourceUrl(), result.getContent()))
            .collect(Collectors.toList());
    }

    /**
     * Extract unique sources from results
     */
    private List<String> extractSources(List<RagService.RagResult> results) {
        return results.stream()
            .map(RagService.RagResult::getSourceUrl)
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * Extract the raw chunk contents used as grounding context. These are passed
     * to the Evaluation Agent (F-008) so it can verify the draft answer is
     * supported by the retrieved business knowledge.
     */
    private List<String> extractChunkContents(List<RagService.RagResult> results) {
        return results.stream()
            .map(RagService.RagResult::getContent)
            .filter(c -> c != null && !c.isBlank())
            .collect(Collectors.toList());
    }

    /**
     * Get conversation history
     */
    public List<Message> getConversationHistory(UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    /**
     * Handle the multi-step lead capture flow for a single turn.
     *
     * @param assistantAskedForContact true when the support answer for this turn
     *        was the "please share your contact details" fallback. In that case
     *        we proactively enter capture mode so the customer's next reply is
     *        stored instead of being re-processed as a new question.
     * @return outcome describing whether a lead was created and/or a message the
     *         customer should be shown instead of the RAG answer.
     */
    private LeadCaptureOutcome handleLeadCapture(Conversation conversation, String message,
                                                 UUID businessId, boolean assistantAskedForContact) {
        String status = conversation.getLeadCaptureStatus();

        // Step 0: A lead was already captured in this conversation. Do NOT ask
        // for the customer's details again just because a later message hit the
        // NO_INFO fallback. Only re-open capture if the customer expresses a
        // fresh, explicit buying intent (e.g. asks about another product).
        if ("CAPTURED".equals(status)) {
            boolean buyingIntent = leadQualificationAgent.detectBuyingIntent(message);
            if (buyingIntent) {
                logger.info("Re-opening lead capture after prior capture due to new buying intent");
                conversation.setPendingLeadRequirement(message);
                conversation.setLeadCaptureStatus("AWAITING_DETAILS");
                conversationRepository.save(conversation);
                return LeadCaptureOutcome.message(
                    "Sure — I can have our team follow up on that as well. Could you confirm the best name and phone number or email to reach you?");
            }
            return LeadCaptureOutcome.none();
        }

        // Step 1: Not capturing yet. Enter capture mode when the customer shows
        // buying intent OR when the support agent just asked for contact details.
        if (status == null) {
            boolean buyingIntent = leadQualificationAgent.detectBuyingIntent(message);
            if (buyingIntent || assistantAskedForContact) {
                logger.info("Initiating lead capture (buyingIntent={}, askedForContact={})",
                    buyingIntent, assistantAskedForContact);
                if (conversation.getPendingLeadRequirement() == null) {
                    conversation.setPendingLeadRequirement(message);
                }
                
                // Try to extract lead information from the FIRST message before asking
                LeadQualificationAgent.LeadExtractionResult extraction;
                try {
                    extraction = leadQualificationAgent.extractLeadInfo(message);
                } catch (Exception e) {
                    logger.warn("AI extraction failed on first message", e);
                    conversation.setLeadCaptureStatus("AWAITING_DETAILS");
                    conversationRepository.save(conversation);
                    return LeadCaptureOutcome.message(
                        "Sure — I can have our team follow up. Could you share your name and a phone number or email so we can reach you?");
                }
                
                // Store any details found in the first message
                if (extraction.getName() != null && !extraction.getName().isBlank()
                        && !extraction.getName().equals("Unknown")) {
                    conversation.setPendingLeadName(extraction.getName());
                }
                if (extraction.getEmail() != null && !extraction.getEmail().isBlank()) {
                    conversation.setPendingLeadEmail(extraction.getEmail());
                }
                if (extraction.getPhone() != null && !extraction.getPhone().isBlank()) {
                    conversation.setPendingLeadPhone(extraction.getPhone());
                }
                
                boolean hasName = conversation.getPendingLeadName() != null
                    && !conversation.getPendingLeadName().isBlank()
                    && !conversation.getPendingLeadName().equals("Unknown");
                boolean hasContact = (conversation.getPendingLeadEmail() != null && !conversation.getPendingLeadEmail().isBlank())
                    || (conversation.getPendingLeadPhone() != null && !conversation.getPendingLeadPhone().isBlank());
                
                // If we already have complete information, create the lead immediately
                if (hasName && hasContact) {
                    logger.info("Complete lead info found in first message, creating lead immediately");
                    LeadQualificationRequest leadRequest = new LeadQualificationRequest(
                        businessId,
                        conversation.getId(),
                        buildLeadMessage(conversation)
                    );
                    
                    try {
                        LeadQualificationResponse leadResponse = leadQualificationService.qualifyLead(leadRequest);
                        UUID leadId = leadResponse.getLeadId();
                        String confirmation = getLeadConfirmationMessage(conversation);
                        
                        clearPendingLeadData(conversation);
                        conversation.setLeadCaptureStatus("CAPTURED");
                        conversationRepository.save(conversation);
                        
                        logger.info("Lead created from first message: {}", leadId);
                        return LeadCaptureOutcome.lead(leadId, confirmation);
                    } catch (Exception e) {
                        logger.error("Failed to create lead from first message", e);
                        // Fall through to ask for details
                    }
                }
                
                // Information is incomplete, ask only for what's missing
                conversation.setLeadCaptureStatus("COLLECTING_DETAILS");
                conversationRepository.save(conversation);
                return LeadCaptureOutcome.message(buildMissingDetailsPrompt(conversation, hasName, hasContact));
            }
            return LeadCaptureOutcome.none();
        }

        // Step 2 & 3: In capture mode - extract whatever details were provided.
        if ("AWAITING_DETAILS".equals(status) || "COLLECTING_DETAILS".equals(status)) {
            LeadQualificationAgent.LeadExtractionResult extraction;
            try {
                extraction = leadQualificationAgent.extractLeadInfo(message);
            } catch (Exception e) {
                logger.warn("AI extraction failed in capture mode", e);
                return LeadCaptureOutcome.message(
                    "Could you please share your name and a phone number or email so our team can reach you?");
            }

            // Store any newly-provided details into the conversation's pending fields
            // (preserving details captured on earlier turns).
            if (extraction.getName() != null && !extraction.getName().isBlank()
                    && !extraction.getName().equals("Unknown")) {
                conversation.setPendingLeadName(extraction.getName());
            }
            if (extraction.getEmail() != null && !extraction.getEmail().isBlank()) {
                conversation.setPendingLeadEmail(extraction.getEmail());
            }
            if (extraction.getPhone() != null && !extraction.getPhone().isBlank()) {
                conversation.setPendingLeadPhone(extraction.getPhone());
            }
            if (extraction.getRequirementText() != null && !extraction.getRequirementText().isBlank()
                    && conversation.getPendingLeadRequirement() == null) {
                conversation.setPendingLeadRequirement(extraction.getRequirementText());
            }

            boolean hasName = conversation.getPendingLeadName() != null
                && !conversation.getPendingLeadName().isBlank()
                && !conversation.getPendingLeadName().equals("Unknown");
            boolean hasContact = (conversation.getPendingLeadEmail() != null && !conversation.getPendingLeadEmail().isBlank())
                || (conversation.getPendingLeadPhone() != null && !conversation.getPendingLeadPhone().isBlank());

            if (hasName && hasContact) {
                logger.info("All required lead fields present, creating lead");
                LeadQualificationRequest leadRequest = new LeadQualificationRequest(
                    businessId,
                    conversation.getId(),
                    buildLeadMessage(conversation)
                );

                try {
                    LeadQualificationResponse leadResponse = leadQualificationService.qualifyLead(leadRequest);
                    UUID leadId = leadResponse.getLeadId();
                    String confirmation = getLeadConfirmationMessage(conversation);

                    // Clear pending data and mark the conversation as CAPTURED
                    // now that the lead exists. Keeping a CAPTURED marker (rather
                    // than null) stops a later NO_INFO fallback from silently
                    // re-triggering the whole "share your details" flow.
                    clearPendingLeadData(conversation);
                    conversation.setLeadCaptureStatus("CAPTURED");
                    conversationRepository.save(conversation);

                    logger.info("Lead created: {}", leadId);
                    return LeadCaptureOutcome.lead(leadId, confirmation);
                } catch (Exception e) {
                    logger.error("Failed to create lead", e);
                    conversation.setLeadCaptureStatus("COLLECTING_DETAILS");
                    conversationRepository.save(conversation);
                    return LeadCaptureOutcome.message(
                        "Thanks! I had trouble saving your details just now. Could you re-share your name along with a phone number or email?");
                }
            }

            // Still missing something - acknowledge what we have and ask only for
            // the remaining field(s), so we never repeat the same request.
            conversation.setLeadCaptureStatus("COLLECTING_DETAILS");
            conversationRepository.save(conversation);
            logger.info("Still collecting details - hasName: {}, hasContact: {}", hasName, hasContact);
            return LeadCaptureOutcome.message(buildMissingDetailsPrompt(conversation, hasName, hasContact));
        }

        return LeadCaptureOutcome.none();
    }

    /**
     * Builds a progressive, non-repetitive prompt that asks only for the fields
     * still missing and acknowledges anything the customer already provided.
     */
    private String buildMissingDetailsPrompt(Conversation conversation, boolean hasName, boolean hasContact) {
        if (hasName && !hasContact) {
            String name = conversation.getPendingLeadName();
            return String.format("Thanks %s! Could you also share a phone number or email so our team can reach you?", name);
        }
        if (!hasName && hasContact) {
            return "Thanks, I've noted your contact details. And what name should we use when our team reaches out to you?";
        }
        return "Sure — please share your name along with a phone number or email so our team can contact you.";
    }

    /**
     * Outcome of the lead capture flow for a single turn.
     */
    private static class LeadCaptureOutcome {
        private final UUID leadId;
        private final String responseMessage;

        private LeadCaptureOutcome(UUID leadId, String responseMessage) {
            this.leadId = leadId;
            this.responseMessage = responseMessage;
        }

        static LeadCaptureOutcome none() {
            return new LeadCaptureOutcome(null, null);
        }

        static LeadCaptureOutcome message(String responseMessage) {
            return new LeadCaptureOutcome(null, responseMessage);
        }

        static LeadCaptureOutcome lead(UUID leadId, String responseMessage) {
            return new LeadCaptureOutcome(leadId, responseMessage);
        }

        UUID getLeadId() {
            return leadId;
        }

        String getResponseMessage() {
            return responseMessage;
        }
    }

    /**
     * Build lead message from pending data
     */
    private String buildLeadMessage(Conversation conversation) {
        StringBuilder msg = new StringBuilder();
        
        if (conversation.getPendingLeadRequirement() != null) {
            msg.append("Requirement: ").append(conversation.getPendingLeadRequirement()).append("\n");
        }
        if (conversation.getPendingLeadName() != null) {
            msg.append("Name: ").append(conversation.getPendingLeadName()).append("\n");
        }
        if (conversation.getPendingLeadEmail() != null) {
            msg.append("Email: ").append(conversation.getPendingLeadEmail()).append("\n");
        }
        if (conversation.getPendingLeadPhone() != null) {
            msg.append("Phone: ").append(conversation.getPendingLeadPhone());
        }
        
        return msg.toString();
    }

    /**
     * Get lead confirmation message
     */
    private String getLeadConfirmationMessage(Conversation conversation) {
        String name = conversation.getPendingLeadName();
        if (name == null || name.isBlank()) {
            name = "there";
        }
        return String.format("Thanks %s, your details have been saved. Our team will contact you soon.", name);
    }

    /**
     * Clear pending lead data
     */
    private void clearPendingLeadData(Conversation conversation) {
        conversation.setLeadCaptureStatus(null);
        conversation.setPendingLeadName(null);
        conversation.setPendingLeadEmail(null);
        conversation.setPendingLeadPhone(null);
        conversation.setPendingLeadRequirement(null);
    }

    /**
     * Result class
     */
    public static class ChatResult {
        private final UUID conversationId;
        private final String answer;
        private final List<String> sources;
        private final int confidenceScore;
        private UUID leadId;
        private EvaluationResponse evaluation;
        private boolean leadCaptureInProgress;

        public ChatResult(UUID conversationId, String answer, List<String> sources, int confidenceScore) {
            this.conversationId = conversationId;
            this.answer = answer;
            this.sources = sources;
            this.confidenceScore = confidenceScore;
            this.leadId = null;
            this.evaluation = null;
            this.leadCaptureInProgress = false;
        }

        public boolean isLeadCaptureInProgress() {
            return leadCaptureInProgress;
        }

        public void setLeadCaptureInProgress(boolean leadCaptureInProgress) {
            this.leadCaptureInProgress = leadCaptureInProgress;
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

        public UUID getLeadId() {
            return leadId;
        }

        public void setLeadId(UUID leadId) {
            this.leadId = leadId;
        }

        public EvaluationResponse getEvaluation() {
            return evaluation;
        }

        public void setEvaluation(EvaluationResponse evaluation) {
            this.evaluation = evaluation;
        }
    }

    /**
     * Exception class
     */
    public static class ChatException extends RuntimeException {
        public ChatException(String message) {
            super(message);
        }
    }
}
