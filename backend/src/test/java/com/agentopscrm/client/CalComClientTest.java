package com.agentopscrm.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalComClientTest {

    @Mock private RestTemplate restTemplate;

    @Test
    void isConfiguredFromBookingUrlOnly() {
        assertFalse(new CalComClient(restTemplate, "cal_live_abc", "").isConfigured());
        assertNull(new CalComClient(restTemplate, "", "").getBookingUrl());
        CalComClient client = new CalComClient(restTemplate, "", "https://cal.com/ada/15min");
        assertTrue(client.isConfigured());
        assertFalse(client.hasApiKey());
        assertEquals("https://cal.com/ada/15min", client.getBookingUrl());
    }

    @Test
    void pingCallsMeWhenApiKeyPresent() throws Exception {
        CalComClient client = new CalComClient(restTemplate, "cal_live_abc", "https://cal.com/ada/15min");
        when(restTemplate.exchange(
                eq(CalComClient.ME_URL),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"status\":\"success\"}", HttpStatus.OK));

        client.ping();
        assertTrue(client.hasApiKey());
    }
}
