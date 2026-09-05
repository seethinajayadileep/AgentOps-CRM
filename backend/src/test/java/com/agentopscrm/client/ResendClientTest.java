package com.agentopscrm.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResendClientTest {

    @Mock private RestTemplate restTemplate;

    @Test
    void isConfiguredRequiresKeyAndFrom() {
        assertFalse(new ResendClient(restTemplate, "", "Ada <ada@example.com>").isConfigured());
        assertFalse(new ResendClient(restTemplate, "re_live", "not-an-email").isConfigured());
        assertFalse(new ResendClient(restTemplate, "re_...", "Ada <ada@example.com>").isConfigured());
        assertTrue(new ResendClient(restTemplate, "re_live", "Ada <ada@example.com>").isConfigured());
    }

    @Test
    void pingTreatsSendOnlyKeyAsSuccess() throws Exception {
        ResendClient client = new ResendClient(restTemplate, "re_live", "Ada <ada@example.com>");
        HttpClientErrorException forbidden = HttpClientErrorException.create(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                null,
                "{\"name\":\"restricted_api_key\",\"message\":\"This API key is restricted to only send emails.\"}"
                        .getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);
        when(restTemplate.exchange(eq(ResendClient.DOMAINS_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(forbidden);

        client.ping();
    }

    @Test
    void sendEmailPostsToResendAndReturnsId() throws Exception {
        ResendClient client = new ResendClient(restTemplate, "re_live", "Ada <ada@example.com>");
        ResendClient.SendResponse body = new ResendClient.SendResponse();
        body.id = "abc-123";
        when(restTemplate.exchange(
                eq(ResendClient.EMAILS_URL),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(ResendClient.SendResponse.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        ResendClient.SendResult result = client.sendEmail(
                "lead@example.com",
                "Follow-up from Stripe",
                "Dear Ada, Thanks for writing. Please let us know a time to talk.");

        assertEquals("abc-123", result.id());
        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq(ResendClient.EMAILS_URL),
                eq(HttpMethod.POST),
                captor.capture(),
                eq(ResendClient.SendResponse.class));
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) captor.getValue().getBody();
        assertTrue(payload.get("text").toString().contains("\n\n"));
        assertTrue(payload.get("html").toString().contains("<p"));
    }

    @Test
    void sendEmailSurfacesProviderMessage() {
        ResendClient client = new ResendClient(restTemplate, "re_live", "Ada <ada@example.com>");
        HttpClientErrorException unprocessable = HttpClientErrorException.create(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Unprocessable",
                null,
                "{\"statusCode\":422,\"name\":\"validation_error\",\"message\":\"Invalid `from` field\"}"
                        .getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);
        when(restTemplate.exchange(
                eq(ResendClient.EMAILS_URL),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(ResendClient.SendResponse.class)))
                .thenThrow(unprocessable);

        ResendClient.ResendException ex = assertThrows(
                ResendClient.ResendException.class,
                () -> client.sendEmail("lead@example.com", "Hi", "Body"));
        assertTrue(ex.getMessage().contains("Invalid `from` field"));
    }
}
