package com.agentopscrm.util;

import com.agentopscrm.config.JacksonLocalDateTimeConfig;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimestampJsonTest {

    @Test
    void toIsoInstantUsesJvmZoneAndUtcSuffix() {
        LocalDateTime local = LocalDateTime.of(2026, 8, 31, 13, 0, 0);
        String iso = TimestampJson.toIsoInstant(local);
        assertEquals(local.atZone(ZoneId.systemDefault()).toInstant().toString(), iso);
        assertTrue(iso.endsWith("Z"));
    }

    @Test
    void roundTripsOffsetAndNaiveIso() {
        LocalDateTime local = LocalDateTime.of(2026, 8, 31, 13, 0, 0);
        assertEquals(local, TimestampJson.fromIso(TimestampJson.toIsoInstant(local)));
        assertEquals(local, TimestampJson.fromIso("2026-08-31T13:00:00"));
    }

    @Test
    void jacksonWritesLocalDateTimeWithUtcOffset() throws Exception {
        Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.json()
                .modules(new JavaTimeModule())
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        new JacksonLocalDateTimeConfig().localDateTimeAsInstant().customize(builder);
        ObjectMapper mapper = builder.build();

        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 31, 13, 0, 0);
        String json = mapper.writeValueAsString(new Payload(createdAt));
        assertTrue(json.contains("Z"), json);
        assertEquals(createdAt, mapper.readValue(json, Payload.class).createdAt);
    }

    static final class Payload {
        public final LocalDateTime createdAt;

        @JsonCreator
        Payload(@JsonProperty("createdAt") LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }
}
