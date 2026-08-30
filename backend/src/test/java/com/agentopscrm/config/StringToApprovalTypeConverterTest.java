package com.agentopscrm.config;

import com.agentopscrm.entity.enums.ApprovalType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StringToApprovalTypeConverterTest {

    private final StringToApprovalTypeConverter converter = new StringToApprovalTypeConverter();

    @Test
    void mapsFrontendOutboundCallAlias() {
        assertEquals(ApprovalType.VOICE_CALL, converter.convert("OUTBOUND_CALL"));
        assertEquals(ApprovalType.VOICE_CALL, converter.convert("VOICE_CALL"));
    }

    @Test
    void mapsOutreachAlias() {
        assertEquals(ApprovalType.REPORT_GENERATION, converter.convert("OUTREACH_MESSAGE"));
        assertEquals(ApprovalType.REPORT_GENERATION, converter.convert("REPORT_GENERATION"));
    }

    @Test
    void mapsFollowUp() {
        assertEquals(ApprovalType.FOLLOW_UP_MESSAGE, converter.convert("FOLLOW_UP_MESSAGE"));
    }

    @Test
    void rejectsUnknownTypes() {
        assertThrows(IllegalArgumentException.class, () -> converter.convert("NOT_A_TYPE"));
    }
}
