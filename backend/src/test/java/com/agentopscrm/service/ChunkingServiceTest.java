package com.agentopscrm.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ChunkingService} (F-004).
 *
 * Regression guard: chunking MUST always terminate. A prior bug pinned the start
 * index in the tail of the content and produced chunks forever (heap exhaustion).
 */
class ChunkingServiceTest {

    private final ChunkingService service = new ChunkingService();

    @Test
    void chunk_null_returnsEmpty() {
        assertTrue(service.chunkContent(null).isEmpty());
    }

    @Test
    void chunk_empty_returnsEmpty() {
        assertTrue(service.chunkContent("").isEmpty());
    }

    @Test
    void chunk_shortContent_returnsSingleChunk() {
        List<String> chunks = service.chunkContent("Hello world. This is a short document.");
        assertEquals(1, chunks.size());
    }

    /**
     * The key regression test: content larger than the chunk size must terminate
     * quickly and produce a bounded number of chunks (no infinite loop).
     */
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void chunk_largeContent_terminatesWithBoundedChunks() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 60000; i++) {
            sb.append('a');
            if (i % 137 == 0) sb.append(' ');
        }
        String content = sb.toString();

        List<String> chunks = service.chunkContent(content);

        assertFalse(chunks.isEmpty());
        // With ~1000-char chunks and 200 overlap, count must be far below 1 per char.
        assertTrue(chunks.size() < content.length(),
                "chunk count must be bounded, was " + chunks.size());
        assertTrue(chunks.size() < 1000,
                "expected well under 1000 chunks, was " + chunks.size());
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void chunk_contentEndingExactlyAtBoundary_terminates() {
        // Length just over one chunk size — the previous tail bug triggered here.
        String content = "x".repeat(1005);
        List<String> chunks = service.chunkContent(content);
        assertFalse(chunks.isEmpty());
    }

    @Test
    void navigationBoilerplate_isDetected_butProseIsKept() {
        String nav = "[Home](https://x.com/home) [About](https://x.com/about) "
                + "[Clients](https://x.com/clients) [Blog](https://x.com/blog) "
                + "[Contact](https://x.com/contact) [Careers](https://x.com/careers)";
        assertTrue(service.isNavigationBoilerplate(nav), "link/menu list should be flagged");

        String prose = "We are a media buying agency that helps brands plan and execute advertising "
                + "campaigns across many channels with measurable outcomes and dedicated support.";
        assertFalse(service.isNavigationBoilerplate(prose), "real prose should be kept");
    }
}
