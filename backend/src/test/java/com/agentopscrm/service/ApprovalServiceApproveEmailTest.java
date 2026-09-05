package com.agentopscrm.service;

import com.agentopscrm.client.ResendClient;
import com.agentopscrm.dto.ApprovalResponse;
import com.agentopscrm.entity.Approval;
import com.agentopscrm.entity.Business;
import com.agentopscrm.entity.Lead;
import com.agentopscrm.entity.enums.ApprovalStatus;
import com.agentopscrm.entity.enums.ApprovalType;
import com.agentopscrm.repository.AgentLogRepository;
import com.agentopscrm.repository.ApprovalRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalServiceApproveEmailTest {

    @Mock private ApprovalRepository approvalRepository;
    @Mock private AgentLogRepository agentLogRepository;
    @Mock private ResendClient resendClient;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private TransactionStatus transactionStatus;

    private ApprovalService approvalService;

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(transactionStatus);
        approvalService = new ApprovalService(
                approvalRepository, agentLogRepository, new ObjectMapper(), resendClient, transactionManager);
    }

    @Test
    void sendsProfessionalFollowUpWhenResendIsConfigured() throws Exception {
        Approval approval = emailApproval("PROFESSIONAL", "ada@example.com");
        when(approvalRepository.findById(approval.getId())).thenReturn(Optional.of(approval));
        when(approvalRepository.save(any(Approval.class))).thenAnswer(inv -> inv.getArgument(0));
        when(resendClient.isConfigured()).thenReturn(true);
        when(resendClient.sendEmail(any(), any(), any())).thenReturn(new ResendClient.SendResult("msg_1"));

        ApprovalResponse response = approvalService.approveApproval(approval.getId());

        verify(resendClient).sendEmail("ada@example.com", "Follow-up from Stripe", "Hello Ada");
        assertEquals(ApprovalStatus.APPROVED, response.getStatus());
        assertEquals("ada@example.com", response.getSentTo());
        assertEquals("msg_1", response.getResendMessageId());
        assertEquals("Follow-up from Stripe", response.getSentSubject());
    }

    @Test
    void doesNotSendWhatsAppStyleEvenWhenResendIsConfigured() throws Exception {
        Approval approval = emailApproval("SHORT_WHATSAPP", "ada@example.com");
        when(approvalRepository.findById(approval.getId())).thenReturn(Optional.of(approval));
        when(approvalRepository.save(any(Approval.class))).thenAnswer(inv -> inv.getArgument(0));
        org.mockito.Mockito.lenient().when(resendClient.isConfigured()).thenReturn(true);

        ApprovalResponse response = approvalService.approveApproval(approval.getId());

        verify(resendClient, never()).sendEmail(any(), any(), any());
        assertEquals(ApprovalStatus.APPROVED, response.getStatus());
        assertNull(response.getResendMessageId());
    }

    @Test
    void approvesWithoutSendingWhenResendIsNotConfigured() throws Exception {
        Approval approval = emailApproval("FRIENDLY", "ada@example.com");
        when(approvalRepository.findById(approval.getId())).thenReturn(Optional.of(approval));
        when(approvalRepository.save(any(Approval.class))).thenAnswer(inv -> inv.getArgument(0));
        when(resendClient.isConfigured()).thenReturn(false);

        ApprovalResponse response = approvalService.approveApproval(approval.getId());

        verify(resendClient, never()).sendEmail(any(), any(), any());
        assertEquals(ApprovalStatus.APPROVED, response.getStatus());
    }

    @Test
    void staysPendingWhenLeadHasNoEmail() throws Exception {
        Approval approval = emailApproval("PROFESSIONAL", null);
        when(approvalRepository.findById(approval.getId())).thenReturn(Optional.of(approval));
        when(approvalRepository.save(any(Approval.class))).thenAnswer(inv -> inv.getArgument(0));
        when(resendClient.isConfigured()).thenReturn(true);

        ApprovalResponse response = approvalService.approveApproval(approval.getId());

        verify(resendClient, never()).sendEmail(any(), any(), any());
        assertEquals(ApprovalStatus.PENDING, response.getStatus());
        assertTrue(response.getSendError().toLowerCase().contains("email"));
    }

    @Test
    void sendFailureKeepsRowRetryable() throws Exception {
        Approval approval = emailApproval("FRIENDLY", "ada@example.com");
        when(approvalRepository.findById(approval.getId())).thenReturn(Optional.of(approval));
        when(approvalRepository.save(any(Approval.class))).thenAnswer(inv -> inv.getArgument(0));
        when(resendClient.isConfigured()).thenReturn(true);
        when(resendClient.sendEmail(any(), any(), any()))
                .thenThrow(new ResendClient.ResendException("Failed to send email: invalid from"));

        ApprovalResponse response = approvalService.approveApproval(approval.getId());

        assertEquals(ApprovalStatus.SEND_FAILED, response.getStatus());
        assertTrue(response.getSendError().contains("invalid from"));
    }

    @Test
    void doesNotSendTwiceWhenAlreadyDelivered() throws Exception {
        Approval approval = emailApproval("PROFESSIONAL", "ada@example.com");
        approval.setStatus(ApprovalStatus.APPROVED);
        approval.setResendMessageId("msg_existing");
        approval.setSentTo("ada@example.com");
        when(approvalRepository.findById(approval.getId())).thenReturn(Optional.of(approval));
        org.mockito.Mockito.lenient().when(resendClient.isConfigured()).thenReturn(true);

        ApprovalResponse response = approvalService.approveApproval(approval.getId());

        verify(resendClient, never()).sendEmail(any(), any(), any());
        assertEquals("msg_existing", response.getResendMessageId());
        assertEquals(ApprovalStatus.APPROVED, response.getStatus());
    }

    @Test
    void retriesAfterSendFailed() throws Exception {
        Approval approval = emailApproval("PROFESSIONAL", "ada@example.com");
        approval.setStatus(ApprovalStatus.SEND_FAILED);
        approval.setSendError("temporary");
        when(approvalRepository.findById(approval.getId())).thenReturn(Optional.of(approval));
        when(approvalRepository.save(any(Approval.class))).thenAnswer(inv -> inv.getArgument(0));
        when(resendClient.isConfigured()).thenReturn(true);
        when(resendClient.sendEmail(any(), any(), any())).thenReturn(new ResendClient.SendResult("msg_2"));

        ApprovalResponse response = approvalService.approveApproval(approval.getId());

        ArgumentCaptor<String> to = ArgumentCaptor.forClass(String.class);
        verify(resendClient).sendEmail(to.capture(), any(), any());
        assertEquals("ada@example.com", to.getValue());
        assertEquals(ApprovalStatus.APPROVED, response.getStatus());
        assertEquals("msg_2", response.getResendMessageId());
    }

    private static Approval emailApproval(String style, String email) {
        Business business = new Business();
        business.setId(UUID.randomUUID());
        business.setName("Stripe");

        Lead lead = new Lead();
        lead.setId(UUID.randomUUID());
        lead.setName("Ada");
        lead.setEmail(email);
        lead.setBusiness(business);

        Approval approval = new Approval(UUID.randomUUID());
        approval.setBusiness(business);
        approval.setLead(lead);
        approval.setApprovalType(ApprovalType.FOLLOW_UP_MESSAGE);
        approval.setStyle(style);
        approval.setContent("Hello Ada");
        approval.setStatus(ApprovalStatus.PENDING);
        return approval;
    }
}
