package com.agentopscrm.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns Apify location objects and stored {@code {lat=…, lng=…}} dumps into a readable place.
 */
public final class LeadLocation {

    private static final Pattern COORD_DUMP = Pattern.compile(
            "^\\{\\s*lat\\s*[=:]\\s*(-?\\d+(?:\\.\\d+)?)\\s*,\\s*lng\\s*[=:]\\s*(-?\\d+(?:\\.\\d+)?)\\s*}$",
            Pattern.CASE_INSENSITIVE);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private LeadLocation() {}

    public static String fromApifyItem(Map<String, Object> raw) {
        return firstNonBlank(readableFromMap(raw), null);
    }

    public static String display(String stored, String rawDataJson) {
        String fromRaw = readableFromJson(rawDataJson);
        if (fromRaw != null) {
            return fromRaw;
        }
        String fromStored = readableFromStored(stored);
        return fromStored != null ? fromStored : stored;
    }

    private static String readableFromJson(String rawDataJson) {
        if (rawDataJson == null || rawDataJson.isBlank()) {
            return null;
        }
        try {
            return readableFromMap(MAPPER.readValue(rawDataJson, MAP_TYPE));
        } catch (Exception ignored) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static String readableFromMap(Map<String, ?> raw) {
        if (raw == null) {
            return null;
        }
        String address = firstText(raw, "address", "fullAddress", "formattedAddress", "streetAddress");
        if (address != null) {
            return address;
        }
        String composed = composePlace(raw);
        if (composed != null) {
            return composed;
        }
        Object location = raw.get("location");
        if (location instanceof Map<?, ?> nested) {
            String nestedReadable = readableFromMap((Map<String, ?>) nested);
            if (nestedReadable != null) {
                return nestedReadable;
            }
            return formatCoords(nested.get("lat"), firstNonNull(nested.get("lng"), nested.get("lon"), nested.get("longitude")));
        }
        return formatCoords(raw.get("lat"), firstNonNull(raw.get("lng"), raw.get("lon"), raw.get("longitude")));
    }

    private static String readableFromStored(String location) {
        if (location == null) {
            return null;
        }
        String trimmed = location.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        Matcher dump = COORD_DUMP.matcher(trimmed);
        if (dump.matches()) {
            return formatCoords(dump.group(1), dump.group(2));
        }
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return readableFromJson(trimmed);
        }
        return trimmed;
    }

    private static String composePlace(Map<String, ?> raw) {
        List<String> parts = new ArrayList<>();
        for (String key : List.of("street", "neighborhood", "city", "state", "postalCode", "country", "countryCode")) {
            String text = asText(raw.get(key));
            if (text != null) {
                parts.add(text);
            }
        }
        Set<String> unique = new LinkedHashSet<>(parts);
        return unique.isEmpty() ? null : String.join(", ", unique);
    }

    private static String firstText(Map<String, ?> raw, String... keys) {
        for (String key : keys) {
            String text = asText(raw.get(key));
            if (text != null) {
                return text;
            }
        }
        return null;
    }

    private static String asText(Object value) {
        if (!(value instanceof String text)) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed) || COORD_DUMP.matcher(trimmed).matches()) {
            return null;
        }
        return trimmed;
    }

    private static String formatCoords(Object lat, Object lng) {
        Double latitude = asDouble(lat);
        Double longitude = asDouble(lng);
        if (latitude == null || longitude == null) {
            return null;
        }
        String ns = latitude >= 0 ? "N" : "S";
        String ew = longitude >= 0 ? "E" : "W";
        return String.format("%.4f° %s, %.4f° %s", Math.abs(latitude), ns, Math.abs(longitude), ew);
    }

    private static Double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String firstNonBlank(String first, String fallback) {
        return first != null && !first.isBlank() ? first : fallback;
    }
}
