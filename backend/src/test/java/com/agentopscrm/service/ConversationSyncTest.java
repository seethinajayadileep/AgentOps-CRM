package com.agentopscrm.service;

import com.agentopscrm.agent.LeadQualificationAgent;
import com.agentopscrm.dto.LeadQualificationRequest;
import com.agentopscrm.entity.Business;
import com.agentopscrm.entity.Conversation;
import com.agentopscrm.entity.Lead;
import com.agentopscrm.entity.enums.Channel;
import com.agentopscrm.entity.enums.ConversationStatus;
import com.agentopscrm.entity.enums.CrawlStatus;
import com.agentopscrm.entity.enums.Urgency;
import com.agentopscrm.entity.enums.Timeline;
import com.agentopscrm.repository.AgentLogRepository;
import com.agentopscrm.repository.BusinessRepository;
import com.agentopscrm.repository.ConversationRepository;
import com.agentopscrm.repository.LeadRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for Bug #2: Conversation Contact Synchronization
 * 
 * Bug: Lead is created with name/email but Conversations page shows "Anonymous"
 * Fix: Update conversation's customer contact information when lead is captured
 */
@ExtendWith(MockitoExtension.class)
class ConversationSyncTest {

    @Mock
    private LeadQualificationAgent agent;

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private BusinessRepository businessRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private AgentLogRepository agentLogRepository;

    @Mock
    private ObjectMapper objectMapper;

    private LeadQualificationService service;

    @BeforeEach
    void setUp() {
        service = new LeadQualificationService(
            agent, leadRepository, businessRepository, conversationRepository,
            agentLogRepository, objectMapper
        );
    }

    @Test
    void qualifyLead_updatesAnonymousConversation_withLeadContactInfo() throws Exception {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();

        Business business = new Business();
        business.setId(businessId);
        business.setName("Test Business");
        business.setWebsiteUrl("https://test.com");
        business.setCrawlStatus(CrawlStatus.COMPLETED);

        Conversation conversation = new Conversation();
        conversation.setId(conversationId);
        conversation.setBusiness(business);
        conversation.setChannel(Channel.WEB_WIDGET);
        conversation.setStatus(ConversationStatus.ACTIVE);
        conversation.setCustomerName(null);  // Anonymous
        conversation.setCustomerEmail(null);
        conversation.setCustomerPhone(null);

        LeadQualificationAgent.LeadExtractionResult extraction = new LeadQualificationAgent.LeadExtractionResult();
        extraction.setName("Retest QA");
        extraction.setEmail("retest-qa@example.com");
        extraction.setPhone(null);
        extraction.setRequirementText("Help with advertising campaign");
        extraction.setUrgency("medium");
        extraction.setTimeline("WITHIN_1_MONTH");

        LeadQualificationRequest request = new LeadQualificationRequest();
        request.setBusinessId(businessId);
        request.setConversationId(conversationId);
        request.setMessage("I want help with an advertising campaign. My name is Retest QA and my email is retest-qa@example.com.");

        when(businessRepository.findById(businessId)).thenReturn(Optional.of(business));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(leadRepository.findByConversationId(conversationId)).thenReturn(Optional.empty());
        when(agent.extractLeadInfo(anyString())).thenReturn(extraction);
        when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> {
            Lead lead = inv.getArgument(0);
            lead.setId(UUID.randomUUID());
            return lead;
        });
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act
        service.qualifyLead(request);

        // Assert
        ArgumentCaptor<Conversation> conversationCaptor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository, atLeastOnce()).save(conversationCaptor.capture());

        Conversation savedConversation = conversationCaptor.getValue();
        assertEquals("Retest QA", savedConversation.getCustomerName(), 
            "Conversation should be updated with lead name");
        assertEquals("retest-qa@example.com", savedConversation.getCustomerEmail(),
            "Conversation should be updated with lead email");
    }

    @Test
    void qualifyLead_preservesExistingConversationValues_whenLeadFieldIsNull() throws Exception {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();

        Business business = new Business();
        business.setId(businessId);
        business.setName("Test Business");
        business.setWebsiteUrl("https://test.com");
        business.setCrawlStatus(CrawlStatus.COMPLETED);

        Conversation conversation = new Conversation();
        conversation.setId(conversationId);
        conversation.setBusiness(business);
        conversation.setChannel(Channel.WEB_WIDGET);
        conversation.setStatus(ConversationStatus.ACTIVE);
        conversation.setCustomerName("Existing Name");
        conversation.setCustomerEmail("existing@example.com");
        conversation.setCustomerPhone("+1234567890");

        LeadQualificationAgent.LeadExtractionResult extraction = new LeadQualificationAgent.LeadExtractionResult();
        extraction.setName("New Name");
        extraction.setEmail(null);  // Email should be preserved
        extraction.setPhone(null);  // Phone should be preserved
        extraction.setRequirementText("Additional requirement");
        extraction.setUrgency("high");
        extraction.setTimeline("ASAP");

        LeadQualificationRequest request = new LeadQualificationRequest();
        request.setBusinessId(businessId);
        request.setConversationId(conversationId);
        request.setMessage("I need help urgently");

        when(businessRepository.findById(businessId)).thenReturn(Optional.of(business));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(leadRepository.findByConversationId(conversationId)).thenReturn(Optional.empty());
        when(agent.extractLeadInfo(anyString())).thenReturn(extraction);
        when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> {
            Lead lead = inv.getArgument(0);
            lead.setId(UUID.randomUUID());
            return lead;
        });
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act
        service.qualifyLead(request);

        // Assert
        // Conversation should still have original email and phone
        assertEquals("Existing Name", conversation.getCustomerName());
        assertEquals("existing@example.com", conversation.getCustomerEmail());
        assertEquals("+1234567890", conversation.getCustomerPhone());
    }

    @Test
    void qualifyLead_replacesAnonymousName_withActualName() throws Exception {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();

        Business business = new Business();
        business.setId(businessId);
        business.setName("Test Business");
        business.setWebsiteUrl("https://test.com");
        business.setCrawlStatus(CrawlStatus.COMPLETED);

        Conversation conversation = new Conversation();
        conversation.setId(conversationId);
        conversation.setBusiness(business);
        conversation.setChannel(Channel.WEB_WIDGET);
        conversation.setStatus(ConversationStatus.ACTIVE);
        conversation.setCustomerName("Anonymous");
        conversation.setCustomerEmail(null);

        LeadQualificationAgent.LeadExtractionResult extraction = new LeadQualificationAgent.LeadExtractionResult();
        extraction.setName("John Doe");
        extraction.setEmail("john@example.com");
        extraction.setRequirementText("Test requirement");

        LeadQualificationRequest request = new LeadQualificationRequest();
        request.setBusinessId(businessId);
        request.setConversationId(conversationId);
        request.setMessage("My name is John Doe, email john@example.com");

        when(businessRepository.findById(businessId)).thenReturn(Optional.of(business));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(leadRepository.findByConversationId(conversationId)).thenReturn(Optional.empty());
        when(agent.extractLeadInfo(anyString())).thenReturn(extraction);
        when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> {
            Lead lead = inv.getArgument(0);
            lead.setId(UUID.randomUUID());
            return lead;
        });
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act
        service.qualifyLead(request);

        // Assert
        ArgumentCaptor<Conversation> conversationCaptor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository, atLeastOnce()).save(conversationCaptor.capture());

        Conversation savedConversation = conversationCaptor.getValue();
        assertEquals("John Doe", savedConversation.getCustomerName(),
            "Anonymous should be replaced with actual name");
    }

    @Test
    void qualifyLead_usesConversationContactAsFallback_whenExtractionOmitsIt() throws Exception {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();

        Business business = new Business();
        business.setId(businessId);
        business.setName("Test Business");
        business.setWebsiteUrl("https://test.com");
        business.setCrawlStatus(CrawlStatus.COMPLETED);

        // Conversation has contact info from previous messages
        Conversation conversation = new Conversation();
        conversation.setId(conversationId);
        conversation.setBusiness(business);
        conversation.setChannel(Channel.WEB_WIDGET);
        conversation.setStatus(ConversationStatus.ACTIVE);
        conversation.setCustomerName("John Doe");
        conversation.setCustomerEmail("john@example.com");
        conversation.setCustomerPhone("+1234567890");

        // But extraction doesn't capture them in this message
        LeadQualificationAgent.LeadExtractionResult extraction = new LeadQualificationAgent.LeadExtractionResult();
        extraction.setName(null);  // Not extracted
        extraction.setEmail(null); // Not extracted
        extraction.setPhone(null); // Not extracted
        extraction.setRequirementText("I need help with my project");
        extraction.setUrgency("medium");
        extraction.setTimeline("WITHIN_1_MONTH");

        LeadQualificationRequest request = new LeadQualificationRequest();
        request.setBusinessId(businessId);
        request.setConversationId(conversationId);
        request.setMessage("I need help with my project");

        when(businessRepository.findById(businessId)).thenReturn(Optional.of(business));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(leadRepository.findByConversationId(conversationId)).thenReturn(Optional.empty());
        when(agent.extractLeadInfo(anyString())).thenReturn(extraction);
        when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> {
            Lead lead = inv.getArgument(0);
            lead.setId(UUID.randomUUID());
            return lead;
        });
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act
        service.qualifyLead(request);

        // Assert - Lead should use conversation contact info as fallback
        ArgumentCaptor<Lead> leadCaptor = ArgumentCaptor.forClass(Lead.class);
        verify(leadRepository, atLeastOnce()).save(leadCaptor.capture());

        Lead savedLead = leadCaptor.getValue();
        assertEquals("John Doe", savedLead.getName(),
            "Should use conversation name when extraction is null");
        assertEquals("john@example.com", savedLead.getEmail(),
            "Should use conversation email when extraction is null");
        assertEquals("+1234567890", savedLead.getPhone(),
            "Should use conversation phone when extraction is null");
    }
}
