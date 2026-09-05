package com.agentopscrm.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VapiClientDownloadTest {

    @Mock private RestTemplate restTemplate;

    @Test
    void shouldSendVapiAuthOnlyToVapiHosts() {
        assertTrue(VapiClient.shouldSendVapiAuth("https://storage.vapi.ai/file.wav"));
        assertTrue(VapiClient.shouldSendVapiAuth("https://api.vapi.ai/call/1/mono-recording"));
        assertFalse(VapiClient.shouldSendVapiAuth(
                "https://abc.r2.cloudflarestorage.com/hipaa-recordings/file.wav"));
        assertFalse(VapiClient.shouldSendVapiAuth("https://cdn.example.com/file.wav"));
    }

    @Test
    void downloadMediaOmitsBearerForR2() {
        VapiClient client = new VapiClient(restTemplate, "vapi-secret", true);
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class)))
                .thenReturn(new ResponseEntity<>(new byte[] {1}, HttpStatus.OK));

        client.downloadMedia("https://abc.r2.cloudflarestorage.com/hipaa-recordings/file.wav");

        ArgumentCaptor<HttpEntity<?>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(any(URI.class), eq(HttpMethod.GET), captor.capture(), eq(byte[].class));
        HttpHeaders headers = captor.getValue().getHeaders();
        assertFalse(headers.containsKey(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void downloadCallRecordingHitsMonoArtifactWithBearer() throws Exception {
        VapiClient client = new VapiClient(restTemplate, "vapi-secret", true);
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class)))
                .thenReturn(new ResponseEntity<>(new byte[] {9, 8}, HttpStatus.OK));

        ResponseEntity<byte[]> response = client.downloadCallRecording("call-123");

        assertEquals(2, response.getBody().length);
        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        ArgumentCaptor<HttpEntity<?>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(uriCaptor.capture(), eq(HttpMethod.GET), entityCaptor.capture(), eq(byte[].class));
        assertEquals(URI.create("https://api.vapi.ai/call/call-123/mono-recording"), uriCaptor.getValue());
        assertTrue(entityCaptor.getValue().getHeaders().getFirst(HttpHeaders.AUTHORIZATION).startsWith("Bearer "));
    }
}
