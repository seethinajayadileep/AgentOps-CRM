package com.agentopscrm.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisUrlTest {

    @Test
    void parsesRailwayInternalUrl() {
        RedisUrl.Parsed parsed = RedisUrl.parse("redis://default:s3cret@redis.railway.internal:6379");
        assertEquals("redis.railway.internal", parsed.host());
        assertEquals(6379, parsed.port());
        assertEquals("s3cret", parsed.password());
        assertNull(parsed.username());
        assertFalse(parsed.ssl());
    }

    @Test
    void parsesTlsUrl() {
        RedisUrl.Parsed parsed = RedisUrl.parse("rediss://user:p%40ss@proxy.rlwy.net:12345");
        assertEquals("proxy.rlwy.net", parsed.host());
        assertEquals(12345, parsed.port());
        assertEquals("user", parsed.username());
        assertEquals("p@ss", parsed.password());
        assertTrue(parsed.ssl());
    }
}
