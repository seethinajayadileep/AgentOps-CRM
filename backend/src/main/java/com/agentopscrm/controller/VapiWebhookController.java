package com.agentopscrm.controller;

import com.agentopscrm.dto.VapiWebhookEvent;
import com.agentopscrm.service.VoiceCallService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * REST controller for handling Vapi webhooks.
 * 
 * Why exists: Receives webhook events from Vapi to track call status and updates.
 * Includes signature verification for security.
 * 
 * @author AgentOps Team
 * @version 0.2.0
 */
@RestController
@RequestMapping("/api/webhooks")
public class VapiWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(VapiWebhookController.class);

    private final VoiceCallService voiceCallService;
    private final String webhookSecret;
    private final ObjectMapper objectMapper;

    public VapiWebhookController(
        VoiceCallService voiceCallService,
        @Value("${vapi.webhook-secret:}") String webhookSecret,
        ObjectMapper objectMapper
    ) {
        this.voiceCallService = voiceCallService;
        this.webhookSecret = webhookSecret;
        this.objectMapper = objectMapper;
    }

    /**
     * Handle Vapi webhook events.
     * 
     * POST /api/webhooks/vapi
     *
     * @param signature the webhook signature header
     * @param payload the raw webhook payload as string
     * @return 200 OK if processed successfully
     */
    @PostMapping("/vapi")
    public ResponseEntity<String> handleVapiWebhook(
        @RequestHeader(value = "X-Vapi-Signature", required = false) String signature,
        @RequestBody String payload
    ) {
        logger.info("Received Vapi webhook");

        try {
            // Verify webhook signature if secret is configured
            if (webhookSecret != null && !webhookSecret.trim().isEmpty()) {
                if (signature == null || !verifySignature(payload, signature)) {
                    logger.warn("Invalid webhook signature");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
                }
            }

            // Parse the webhook event from the payload string
            VapiWebhookEvent event = objectMapper.readValue(payload, VapiWebhookEvent.class);
            logger.info("Parsed webhook event type: {}", event.getType());

            // Process the webhook event
            voiceCallService.processWebhookEvent(event);

            logger.info("Successfully processed Vapi webhook event: {}", event.getType());
            return ResponseEntity.ok("Webhook processed");

        } catch (Exception e) {
            logger.error("Failed to process Vapi webhook", e);
            // Return 200 anyway to avoid webhook retry storms
            return ResponseEntity.ok("Webhook received");
        }
    }

    /**
     * Verify webhook signature using HMAC SHA-256.
     *
     * @param payload the raw webhook payload
     * @param signature the signature to verify
     * @return true if signature is valid
     */
    private boolean verifySignature(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                webhookSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            );
            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = HexFormat.of().formatHex(hash);

            // Compare signatures in constant time to prevent timing attacks
            return constantTimeEquals(signature, expectedSignature);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Failed to verify webhook signature", e);
            return false;
        }
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
     *
     * @param a first string
     * @param b second string
     * @return true if strings are equal
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }

        if (a.length() != b.length()) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }

        return result == 0;
    }
}
