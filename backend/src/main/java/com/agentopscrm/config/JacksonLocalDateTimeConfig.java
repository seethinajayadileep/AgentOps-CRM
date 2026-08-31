package com.agentopscrm.config;

import com.agentopscrm.util.TimestampJson;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Serializes {@link LocalDateTime} as an ISO-8601 instant with a {@code Z} offset
 * so browsers do not interpret timezone-less values as local wall clock.
 */
@Configuration
public class JacksonLocalDateTimeConfig {

    @Bean
    public SimpleModule localDateTimeInstantModule() {
        SimpleModule module = new SimpleModule("localDateTimeInstant");
        module.addSerializer(LocalDateTime.class, new LocalDateTimeInstantSerializer());
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeInstantDeserializer());
        return module;
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer localDateTimeAsInstant() {
        return builder -> builder
                .serializerByType(LocalDateTime.class, new LocalDateTimeInstantSerializer())
                .deserializerByType(LocalDateTime.class, new LocalDateTimeInstantDeserializer());
    }

    static final class LocalDateTimeInstantSerializer extends JsonSerializer<LocalDateTime> {
        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            if (value == null) {
                gen.writeNull();
                return;
            }
            gen.writeString(TimestampJson.toIsoInstant(value));
        }
    }

    static final class LocalDateTimeInstantDeserializer extends JsonDeserializer<LocalDateTime> {
        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return TimestampJson.fromIso(p.getValueAsString());
        }
    }
}
