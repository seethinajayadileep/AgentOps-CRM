package com.agentopscrm.service;

import com.agentopscrm.client.CalComClient;
import com.agentopscrm.client.SlackWebhookClient;
import com.agentopscrm.client.TelegramClient;
import com.agentopscrm.entity.Business;
import com.agentopscrm.entity.Lead;
import com.agentopscrm.entity.enums.LeadStatus;
import com.agentopscrm.repository.AgentLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeadNotificationServiceTest {

    @Mock private TelegramClient telegramClient;
    @Mock private SlackWebhookClient slackWebhookClient;
    @Mock private CalComClient calComClient;
    @Mock private AgentLogRepository agentLogRepository;

    private LeadNotificationService service;

    @BeforeEach
    void setUp() {
        service = new LeadNotificationService(
                telegramClient, slackWebhookClient, calComClient, agentLogRepository,
                "http://localhost:5173");
    }

    @Test
    void skipsWhenNoChatChannelConfigured() throws Exception {
        when(telegramClient.isConfigured()).thenReturn(false);
        when(slackWebhookClient.isConfigured()).thenReturn(false);

        service.scheduleFreshLeadNotice(sampleLead(), "INBOUND");

        verify(telegramClient, never()).sendMessage(any());
        verify(slackWebhookClient, never()).sendMessage(any());
        verify(agentLogRepository, never()).save(any());
    }

    @Test
    void sendsTelegramAndSlackWithBookingLink() throws Exception {
        when(telegramClient.isConfigured()).thenReturn(true);
        when(slackWebhookClient.isConfigured()).thenReturn(true);
        when(calComClient.getBookingUrl()).thenReturn("https://cal.com/ada/15min");

        service.scheduleFreshLeadNotice(sampleLead(), "INBOUND");

        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(telegramClient).sendMessage(text.capture());
        verify(slackWebhookClient).sendMessage(text.getValue());
        assertTrue(text.getValue().contains("Jayadileep"));
        assertTrue(text.getValue().contains("jayadileepb@gmail.com"));
        assertTrue(text.getValue().contains("INBOUND"));
        assertTrue(text.getValue().contains("https://cal.com/ada/15min"));
        assertTrue(text.getValue().contains("/leads/"));
        verify(agentLogRepository).save(any());
    }

    private static Lead sampleLead() {
        Business business = new Business(UUID.randomUUID());
        business.setName("startupgenome");
        Lead lead = new Lead(UUID.randomUUID());
        lead.setBusiness(business);
        lead.setName("Jayadileep");
        lead.setEmail("jayadileepb@gmail.com");
        lead.setPhone("+910000000000");
        lead.setStatus(LeadStatus.QUALIFIED);
        lead.setLeadScore(BigDecimal.valueOf(72));
        return lead;
    }
}
