package com.agentopscrm.service;

import com.agentopscrm.entity.Business;
import com.agentopscrm.entity.Conversation;
import com.agentopscrm.entity.Lead;
import com.agentopscrm.entity.enums.Channel;
import com.agentopscrm.entity.enums.ConversationStatus;
import com.agentopscrm.entity.enums.CrawlStatus;
import com.agentopscrm.entity.enums.LeadStatus;
import com.agentopscrm.exception.LeadNotFoundException;
import com.agentopscrm.repository.BusinessRepository;
import com.agentopscrm.repository.ConversationRepository;
import com.agentopscrm.repository.LeadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Bug 1: PUT /api/leads/{id}/status returning 500
 * due to LazyInitializationException on Business/Conversation.
 *
 * These tests run against a real Hibernate session (H2, non open-in-view)
 * to reproduce the exact production conditions: the entity returned from
 * save() must still allow safe access to lazy associations after the
 * original transaction/session has ended, once mapped through
 * {@link LeadService#updateStatus}.
 *
 * Every valid {@link LeadStatus} value used by the frontend status
 * selector is covered: NEW, QUALIFIED, HOT, COLD, FOLLOWED_UP, CLOSED.
 *
 * Flyway is disabled and the schema is generated directly from JPA
 * entities (create-drop) because the production migration scripts use
 * PostgreSQL-specific multi-column ALTER TABLE syntax that H2 cannot
 * parse. This keeps the test focused on the lazy-loading behavior under
 * test rather than on cross-database migration compatibility.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class LeadServiceStatusUpdateTest {

    @Autowired
    private LeadService leadService;

    @Autowired
    private BusinessRepository businessRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private LeadRepository leadRepository;

    private UUID businessId;
    private UUID conversationId;
    private UUID leadId;

    @BeforeEach
    @Transactional
    void setUp() {
        Business business = new Business();
        business.setName("Acme Corp");
        business.setWebsiteUrl("https://acme-" + UUID.randomUUID() + ".test");
        business.setIndustry("Testing");
        business.setCrawlStatus(CrawlStatus.NOT_STARTED);
        business = businessRepository.save(business);
        businessId = business.getId();

        Conversation conversation = new Conversation();
        conversation.setBusiness(business);
        conversation.setChannel(Channel.WEB_WIDGET);
        conversation.setStatus(ConversationStatus.ACTIVE);
        conversation = conversationRepository.save(conversation);
        conversationId = conversation.getId();

        Lead lead = new Lead();
        lead.setBusiness(business);
        lead.setConversation(conversation);
        lead.setName("Jane Prospect");
        lead.setEmail("jane@example.com");
        lead.setStatus(LeadStatus.NEW);
        lead = leadRepository.save(lead);
        leadId = lead.getId();
    }

    /**
     * Covers every valid LeadStatus transition exercised by the UI:
     * NEW, QUALIFIED, HOT, COLD, FOLLOWED_UP, CLOSED.
     */
    @ParameterizedTest
    @EnumSource(value = LeadStatus.class, names = {
            "NEW", "QUALIFIED", "HOT", "COLD", "FOLLOWED_UP", "CLOSED"
    })
    void updateStatus_forEveryValidStatus_doesNotThrowLazyInitializationException(LeadStatus status) {
        Lead updated = assertDoesNotThrow(() -> leadService.updateStatus(leadId, status));

        assertEquals(status, updated.getStatus());

        // Access lazy associations OUTSIDE of any explicit transaction here -
        // the test method itself is not @Transactional, mirroring the
        // production controller which has no open session at this point.
        assertDoesNotThrow(() -> {
            assertNotNull(updated.getBusiness());
            assertEquals(businessId, updated.getBusiness().getId());
            assertEquals("Acme Corp", updated.getBusiness().getName());
        }, "Accessing lead.getBusiness() must not throw LazyInitializationException");

        assertDoesNotThrow(() -> {
            assertNotNull(updated.getConversation());
            assertEquals(conversationId, updated.getConversation().getId());
        }, "Accessing lead.getConversation() must not throw LazyInitializationException");
    }

    @Test
    void updateStatus_withUnknownLeadId_throwsLeadNotFoundException() {
        UUID unknownId = UUID.randomUUID();
        assertThrows(LeadNotFoundException.class, () -> leadService.updateStatus(unknownId, LeadStatus.HOT));
    }

    @Test
    void updateStatus_withNullStatus_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> leadService.updateStatus(leadId, null));
    }

    @Test
    void getAllLeads_associationsInitialized_doesNotThrow() {
        assertDoesNotThrow(() -> {
            var leads = leadService.getAllLeads();
            assertFalse(leads.isEmpty());
            for (Lead lead : leads) {
                if (lead.getBusiness() != null) {
                    lead.getBusiness().getName();
                }
            }
        });
    }
}
