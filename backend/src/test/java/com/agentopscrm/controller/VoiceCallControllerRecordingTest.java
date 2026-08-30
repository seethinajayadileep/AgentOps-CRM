package com.agentopscrm.controller;

import com.agentopscrm.exception.GlobalExceptionHandler;
import com.agentopscrm.service.VoiceCallService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class VoiceCallControllerRecordingTest {

    @Mock private VoiceCallService voiceCallService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new VoiceCallController(voiceCallService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void recordingUsesUpstreamMediaTypeAndLength() throws Exception {
        UUID id = UUID.randomUUID();
        byte[] audio = new byte[] {10, 20, 30};
        when(voiceCallService.fetchRecording(id))
            .thenReturn(new VoiceCallService.RecordingPayload(audio, MediaType.parseMediaType("audio/wav")));

        mockMvc.perform(get("/api/voice-calls/{callId}/recording", id))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("audio/wav")))
            .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, "3"))
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline"))
            .andExpect(content().bytes(audio));
    }

    @Test
    void missingRecordingIsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(voiceCallService.fetchRecording(id))
            .thenThrow(new IllegalArgumentException("No recording is available for this call"));

        mockMvc.perform(get("/api/voice-calls/{callId}/recording", id))
            .andExpect(status().isNotFound());
    }

    @Test
    void upstreamFailureIsBadGateway() throws Exception {
        UUID id = UUID.randomUUID();
        when(voiceCallService.fetchRecording(id))
            .thenThrow(new IllegalStateException("Recording could not be retrieved"));

        mockMvc.perform(get("/api/voice-calls/{callId}/recording", id))
            .andExpect(status().isBadGateway());
    }
}
