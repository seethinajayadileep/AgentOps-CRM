package com.agentopscrm.config;

import com.agentopscrm.entity.enums.ConversationStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Accepts canonical and common display-case conversation status values.
 * Unknown values fail conversion so Spring returns HTTP 400 instead of 500.
 */
@Component
public class StringToConversationStatusConverter implements Converter<String, ConversationStatus> {

    @Override
    public ConversationStatus convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        String normalized = source.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "ACTIVE" -> ConversationStatus.ACTIVE;
            case "PAUSED", "PAUSE" -> ConversationStatus.PAUSED;
            case "CLOSED", "CLOSE" -> ConversationStatus.CLOSED;
            case "ARCHIVED", "ARCHIVE" -> ConversationStatus.ARCHIVED;
            default -> throw new IllegalArgumentException(
                    "Invalid conversation status. Supported values: ACTIVE, PAUSED, CLOSED, ARCHIVED");
        };
    }
}
