package com.agentopscrm.service;

import com.agentopscrm.repository.AgentLogRepository;
import com.agentopscrm.repository.BusinessRepository;
import com.agentopscrm.repository.ConversationRepository;
import com.agentopscrm.repository.MessageRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ConversationServiceValidationTest {

    @Mock private ConversationRepository conversationRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private BusinessRepository businessRepository;
    @Mock private AgentLogRepository agentLogRepository;
    @Mock private EntityManager entityManager;

    private ConversationService service;

    @BeforeEach
    void setUp() {
        service = new ConversationService(
                conversationRepository, messageRepository, businessRepository, agentLogRepository, entityManager);
    }

    @Test
    void searchOverMaxLengthIsInvalid() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.getAllConversations("x".repeat(201), null, null, null, null, null, null, 0, 20, null));
        assertTrue(ex.getMessage().contains("200"));
    }

    @Test
    void invalidDateIsRejected() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.getAllConversations(null, null, null, null, null, "not-a-date", null, 0, 20, null));
        assertTrue(ex.getMessage().toLowerCase().contains("date"));
    }

    @Test
    void negativePageIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                service.getAllConversations(null, null, null, null, null, null, null, -1, 20, null));
    }
}
