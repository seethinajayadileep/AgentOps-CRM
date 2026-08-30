package com.agentopscrm.config;

import com.agentopscrm.entity.enums.ApprovalType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Maps frontend and backend approval-type names onto {@link ApprovalType}.
 * Unknown values fail conversion so Spring returns HTTP 400 instead of 500.
 */
@Component
public class StringToApprovalTypeConverter implements Converter<String, ApprovalType> {

    @Override
    public ApprovalType convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        String normalized = source.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "VOICE_CALL", "OUTBOUND_CALL" -> ApprovalType.VOICE_CALL;
            case "FOLLOW_UP_MESSAGE", "FOLLOWUP_MESSAGE" -> ApprovalType.FOLLOW_UP_MESSAGE;
            case "REPORT_GENERATION", "OUTREACH_MESSAGE" -> ApprovalType.REPORT_GENERATION;
            default -> throw new IllegalArgumentException(
                    "Invalid approval type. Supported values: FOLLOW_UP_MESSAGE, VOICE_CALL, REPORT_GENERATION");
        };
    }
}
