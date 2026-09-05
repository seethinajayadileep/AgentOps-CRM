package com.agentopscrm.service;

import com.agentopscrm.entity.Approval;
import com.agentopscrm.entity.enums.ApprovalStatus;
import com.agentopscrm.entity.enums.ApprovalType;
import com.agentopscrm.repository.AgentLogRepository;
import com.agentopscrm.repository.ApprovalRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalServiceFilterTest {

    @Mock private ApprovalRepository approvalRepository;
    @Mock private AgentLogRepository agentLogRepository;

    private ApprovalService approvalService;

    @BeforeEach
    void setUp() {
        approvalService = new ApprovalService(
                approvalRepository,
                agentLogRepository,
                new ObjectMapper(),
                mock(com.agentopscrm.client.ResendClient.class),
                mock(org.springframework.transaction.PlatformTransactionManager.class));
    }

    @ParameterizedTest
    @EnumSource(ApprovalStatus.class)
    void everyStatusWithNullTypeReturnsEmptyNotError(ApprovalStatus status) {
        when(approvalRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        var result = approvalService.getAllApprovals(status, null, null, null);

        assertTrue(result.isEmpty());
    }

    @ParameterizedTest
    @EnumSource(ApprovalType.class)
    void everyTypeWithApprovedStatusReturnsEmptyNotError(ApprovalType type) {
        when(approvalRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        var result = approvalService.getAllApprovals(ApprovalStatus.APPROVED, type, null, null);

        assertTrue(result.isEmpty());
    }
}
