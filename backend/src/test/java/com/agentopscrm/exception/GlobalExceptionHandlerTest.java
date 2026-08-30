package com.agentopscrm.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void typeMismatchForEnumReturns400WithSupportedValues() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "Closed-ish",
                com.agentopscrm.entity.enums.ConversationStatus.class,
                "status",
                null,
                new IllegalArgumentException("No enum constant"));

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleTypeMismatch(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_PARAMETER", response.getBody().getError());
        assertTrue(response.getBody().getMessage().contains("ACTIVE"));
        assertFalse(response.getBody().getMessage().toLowerCase().contains("exception"));
    }

    @Test
    void missingRequiredParameterReturns400WithoutInternalDetails() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("q", "String");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleMissingParameter(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("MISSING_PARAMETER", response.getBody().getError());
        assertTrue(response.getBody().getMessage().contains("'q'"));
        assertFalse(response.getBody().getMessage().toLowerCase().contains("exception"));
        assertFalse(response.getBody().getMessage().contains("Servlet"));
    }

    @Test
    void genericExceptionHidesInternalDetailsAndIncludesReference() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleGenericException(new RuntimeException("PKIX path building failed: https://api.apify.com"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("INTERNAL_ERROR", response.getBody().getError());
        assertTrue(response.getBody().getMessage().startsWith("An unexpected error occurred. Reference ERR-"));
        assertFalse(response.getBody().getMessage().contains("PKIX"));
        assertFalse(response.getBody().getMessage().contains("api.apify.com"));
        assertFalse(response.getBody().getMessage().contains("javax"));
    }
}
