package com.agentopscrm.util;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LeadLocationTest {

    @Test
    void fromApifyItem_prefersStreetAddressOverCoordinateObject() {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("location", Map.of("lat", 17.4938316, "lng", 78.3955591));
        raw.put("address", "Shifa Plaza, near JNTU, Kukatpally, Hyderabad, Telangana 500072, India");
        raw.put("city", "Hyderabad");

        assertEquals(
                "Shifa Plaza, near JNTU, Kukatpally, Hyderabad, Telangana 500072, India",
                LeadLocation.fromApifyItem(raw));
    }

    @Test
    void fromApifyItem_composesCityWhenAddressIsMissing() {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("location", Map.of("lat", 17.4, "lng", 78.3));
        raw.put("city", "Hyderabad");
        raw.put("state", "Telangana");

        assertEquals("Hyderabad, Telangana", LeadLocation.fromApifyItem(raw));
    }

    @Test
    void display_rewritesStoredCoordinateDumpUsingRawAddress() {
        String rawJson = """
                {"address":"Shifa Plaza, Hyderabad","location":{"lat":17.4938316,"lng":78.3955591}}
                """;
        assertEquals(
                "Shifa Plaza, Hyderabad",
                LeadLocation.display("{lat=17.4938316, lng=78.3955591}", rawJson));
    }

    @Test
    void display_formatsCoordinateDumpWhenRawDataIsMissing() {
        assertEquals("17.4938° N, 78.3956° E", LeadLocation.display("{lat=17.4938316, lng=78.3955591}", null));
    }

    @Test
    void fromApifyItem_returnsNullWhenEmpty() {
        assertNull(LeadLocation.fromApifyItem(Map.of()));
    }
}
