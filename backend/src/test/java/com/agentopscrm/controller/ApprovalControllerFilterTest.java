package com.agentopscrm.controller;

import com.agentopscrm.config.StringToApprovalTypeConverter;
import com.agentopscrm.dto.ApprovalResponse;
import com.agentopscrm.entity.enums.ApprovalStatus;
import com.agentopscrm.entity.enums.ApprovalType;
import com.agentopscrm.exception.GlobalExceptionHandler;
import com.agentopscrm.service.ApprovalService;
import com.agentopscrm.service.FollowUpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ApprovalControllerFilterTest {

    @Mock private ApprovalService approvalService;
    @Mock private FollowUpService followUpService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        FormattingConversionService conversionService = new FormattingConversionService();
        conversionService.addConverter(new StringToApprovalTypeConverter());
        mockMvc = MockMvcBuilders.standaloneSetup(new ApprovalController(followUpService, approvalService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setConversionService(conversionService)
                .build();
    }

    @ParameterizedTest
    @CsvSource({
            "APPROVED, OUTBOUND_CALL",
            "APPROVED, VOICE_CALL",
            "PENDING, FOLLOW_UP_MESSAGE",
            "REJECTED, OUTREACH_MESSAGE",
            "APPROVED, REPORT_GENERATION"
    })
    void filterCombinationsReturn200EvenWhenEmpty(String status, String type) throws Exception {
        when(approvalService.getAllApprovals(any(), any(), isNull(), isNull()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/approvals")
                        .param("status", status)
                        .param("type", type))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void approvedPlusOutboundCallUsesVoiceCallEnum() throws Exception {
        when(approvalService.getAllApprovals(eq(ApprovalStatus.APPROVED), eq(ApprovalType.VOICE_CALL), isNull(), isNull()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/approvals")
                        .param("status", "APPROVED")
                        .param("type", "OUTBOUND_CALL"))
                .andExpect(status().isOk());
    }

    @Test
    void unknownTypeReturns400() throws Exception {
        mockMvc.perform(get("/api/approvals").param("type", "NOT_A_TYPE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validFiltersWithARowStillReturn200() throws Exception {
        ApprovalResponse row = new ApprovalResponse();
        when(approvalService.getAllApprovals(eq(ApprovalStatus.PENDING), isNull(), isNull(), isNull()))
                .thenReturn(List.of(row));

        mockMvc.perform(get("/api/approvals").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
