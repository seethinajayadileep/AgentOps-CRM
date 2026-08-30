package com.agentopscrm.config;

import com.agentopscrm.entity.enums.ConversationStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StringToConversationStatusConverterTest {

    private final StringToConversationStatusConverter converter = new StringToConversationStatusConverter();

    @Test
    void mapsCanonicalAndDisplayCaseValues() {
        assertEquals(ConversationStatus.ACTIVE, converter.convert("ACTIVE"));
        assertEquals(ConversationStatus.PAUSED, converter.convert("Paused"));
        assertEquals(ConversationStatus.CLOSED, converter.convert("Closed"));
        assertEquals(ConversationStatus.ARCHIVED, converter.convert("archived"));
    }

    @Test
    void rejectsUnknownStatus() {
        assertThrows(IllegalArgumentException.class, () -> converter.convert("NOT_A_STATUS"));
    }
}
