package com.agentopscrm.service;

import com.agentopscrm.client.VapiClient;
import com.agentopscrm.entity.VoiceCall;
import com.agentopscrm.repository.LeadRepository;
import com.agentopscrm.repository.VoiceCallRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoiceCallServiceRecordingTest {

    @Mock private VoiceCallRepository voiceCallRepository;
    @Mock private LeadRepository leadRepository;
    @Mock private VapiClient vapiClient;
    @Mock private RestTemplate restTemplate;

    private VoiceCallService service;

    @BeforeEach
    void setUp() {
        service = new VoiceCallService(
            voiceCallRepository, leadRepository, vapiClient, restTemplate, "asst", "phone");
    }

    @Test
    void fetchRecordingPassesThroughAudioContentType() {
        UUID id = UUID.randomUUID();
        stubCall(id, "https://storage.example.com/rec.mp3");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
        byte[] body = new byte[] {1, 2, 3, 4};
        when(vapiClient.downloadMedia(any()))
            .thenReturn(new ResponseEntity<>(body, headers, HttpStatus.OK));

        VoiceCallService.RecordingPayload payload = service.fetchRecording(id);

        assertArrayEquals(body, payload.data());
        assertEquals(MediaType.parseMediaType("audio/mpeg"), payload.contentType());
    }

    @Test
    void fetchRecordingInfersWavWhenUpstreamIsOctetStream() {
        UUID id = UUID.randomUUID();
        stubCall(id, "https://storage.example.com/files/secret.wav?token=abc");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        when(vapiClient.downloadMedia(any()))
            .thenReturn(new ResponseEntity<>(new byte[] {9}, headers, HttpStatus.OK));

        VoiceCallService.RecordingPayload payload = service.fetchRecording(id);

        assertEquals(MediaType.parseMediaType("audio/wav"), payload.contentType());
    }

    @Test
    void fetchRecordingRejectsMissingRecording() {
        UUID id = UUID.randomUUID();
        stubCall(id, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.fetchRecording(id));
        assertTrue(ex.getMessage().contains("No recording"));
    }

    @Test
    void fetchRecordingRejectsNonHttpUrl() {
        UUID id = UUID.randomUUID();
        stubCall(id, "file:///tmp/secret.mp3");

        assertThrows(IllegalArgumentException.class, () -> service.fetchRecording(id));
    }

    @Test
    void inferAudioMediaTypeUsesExtensionAndIgnoresQuery() {
        assertEquals(MediaType.parseMediaType("audio/webm"), VoiceCallService.inferAudioMediaType("https://x/a.webm?sig=1"));
        assertEquals(MediaType.parseMediaType("audio/mp4"), VoiceCallService.inferAudioMediaType("https://x/a.m4a"));
        assertEquals(MediaType.parseMediaType("audio/mpeg"), VoiceCallService.inferAudioMediaType("https://x/a.bin"));
    }

    @Test
    void fetchRecordingRefreshesStaleUrlFromVapi() throws Exception {
        UUID id = UUID.randomUUID();
        VoiceCall call = new VoiceCall(id);
        call.setRecordingUrl("https://storage.vapi.ai/expired.wav");
        call.setVapiCallId("vapi-1");
        when(voiceCallRepository.findById(id)).thenReturn(Optional.of(call));
        when(voiceCallRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(vapiClient.downloadMedia(any()))
            .thenThrow(new RestClientException("expired"))
            .thenReturn(new ResponseEntity<>(new byte[] {7, 8}, new HttpHeaders(), HttpStatus.OK));

        VapiClient.VapiCallResponse remote = new VapiClient.VapiCallResponse();
        remote.recordingUrl = "https://cdn.example.com/fresh.wav";
        when(vapiClient.getCall("vapi-1")).thenReturn(remote);

        VoiceCallService.RecordingPayload payload = service.fetchRecording(id);

        assertArrayEquals(new byte[] {7, 8}, payload.data());
        assertEquals("https://cdn.example.com/fresh.wav", call.getRecordingUrl());
        verify(voiceCallRepository).save(call);
    }

    @Test
    void extractRecordingUrlPrefersStereoArtifactWhenMonoMissing() {
        VapiClient.VapiCallResponse remote = new VapiClient.VapiCallResponse();
        remote.artifact = new VapiClient.Artifact();
        remote.artifact.stereoRecordingUrl = "https://cdn.example.com/stereo.wav";

        assertEquals("https://cdn.example.com/stereo.wav", VoiceCallService.extractRecordingUrl(remote));
    }

    @Test
    void fetchRecordingWrapsUpstreamFailure() {
        UUID id = UUID.randomUUID();
        stubCall(id, "https://storage.example.com/rec.mp3");
        when(vapiClient.downloadMedia(any()))
            .thenThrow(new RestClientException("upstream down"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.fetchRecording(id));
        assertEquals("Recording could not be retrieved", ex.getMessage());
    }

    private void stubCall(UUID id, String recordingUrl) {
        VoiceCall call = new VoiceCall(id);
        call.setRecordingUrl(recordingUrl);
        when(voiceCallRepository.findById(id)).thenReturn(Optional.of(call));
    }
}
