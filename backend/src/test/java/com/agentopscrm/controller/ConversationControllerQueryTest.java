package com.agentopscrm.controller;

import com.agentopscrm.dto.PaginatedResponse;
import com.agentopscrm.dto.PaginationMeta;
import com.agentopscrm.entity.enums.ConversationStatus;
import com.agentopscrm.exception.GlobalExceptionHandler;
import com.agentopscrm.service.ConversationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ConversationControllerQueryTest {

    @Mock
    private ConversationService conversationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        FormattingConversionService conversionService = new FormattingConversionService();
        conversionService.addConverter(new com.agentopscrm.config.StringToConversationStatusConverter());
        mockMvc = MockMvcBuilders.standaloneSetup(new ConversationController(conversationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setConversionService(conversionService)
                .build();
    }

    private void stubEmptyPage() {
        when(conversationService.getAllConversations(
                any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(new PaginatedResponse<>(List.of(), new PaginationMeta(0, 20, 0, 0)));
    }

    @Test
    void searchWithCustomerNameReturns200() throws Exception {
        stubEmptyPage();
        mockMvc.perform(get("/api/conversations")
                        .param("search", "Final Retest QA")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
        verify(conversationService).getAllConversations(
                eq("Final Retest QA"), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(0), eq(20), isNull());
    }

    @Test
    void emptySearchReturns200() throws Exception {
        stubEmptyPage();
        mockMvc.perform(get("/api/conversations").param("search", ""))
                .andExpect(status().isOk());
    }

    @Test
    void specialCharacterSearchReturns200() throws Exception {
        stubEmptyPage();
        mockMvc.perform(get("/api/conversations").param("search", "100%_win\\path"))
                .andExpect(status().isOk());
        verify(conversationService).getAllConversations(
                eq("100%_win\\path"), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(0), eq(20), isNull());
    }

    @Test
    void oversizedSearchIsRejectedByServiceAs400() throws Exception {
        when(conversationService.getAllConversations(
                any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
                .thenThrow(new IllegalArgumentException("search must be 200 characters or fewer"));

        mockMvc.perform(get("/api/conversations").param("search", "x".repeat(201)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_ARGUMENT"));
    }

    @Test
    void everyCanonicalStatusReturns200() throws Exception {
        stubEmptyPage();
        for (ConversationStatus status : ConversationStatus.values()) {
            mockMvc.perform(get("/api/conversations").param("status", status.name()))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void closedDisplayCaseMapsToEnum() throws Exception {
        stubEmptyPage();
        mockMvc.perform(get("/api/conversations").param("status", "Closed"))
                .andExpect(status().isOk());
        verify(conversationService).getAllConversations(
                isNull(), isNull(), eq(ConversationStatus.CLOSED), isNull(), isNull(), isNull(), isNull(),
                eq(0), eq(20), isNull());
    }

    @Test
    void invalidStatusReturns400Not500() throws Exception {
        mockMvc.perform(get("/api/conversations").param("status", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("ACTIVE")));
    }
}
