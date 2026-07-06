package com.agentopscrm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for chunking document content into smaller pieces for RAG.
 *
 * @author AgentOps Team
 * @version 0.2.0
 */
@Service
public class ChunkingService {

    private static final Logger log = LoggerFactory.getLogger(ChunkingService.class);

    private static final int CHUNK_SIZE = 1000; // characters
    private static final int OVERLAP_SIZE = 200; // characters
    private static final int MIN_CHUNK_SIZE = 50; // characters

    /**
     * Split document content into chunks with overlap.
     *
     * @param content The document content to chunk
     * @return List of chunks
     */
    public List<String> chunkContent(String content) {
        if (content == null || content.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < content.length()) {
            int end = Math.min(start + CHUNK_SIZE, content.length());

            // Try to find a good breaking point (period, newline, space)
            if (end < content.length()) {
                int idealBreak = findBreakPoint(content, end);
                if (idealBreak > start + MIN_CHUNK_SIZE) {
                    end = idealBreak;
                }
            }

            String chunk = content.substring(start, end).trim();
            // Skip navigation/link-list/"Useful Links" boilerplate; keep real prose.
            if (!chunk.isEmpty() && !isNavigationBoilerplate(chunk)) {
                chunks.add(chunk);
            }

            // Reached the end of the content — stop.
            if (end >= content.length()) {
                break;
            }

            // Advance with overlap, but ALWAYS make forward progress to avoid an
            // infinite loop when (end - OVERLAP_SIZE) <= start (which previously
            // pinned `start` and generated chunks forever, exhausting the heap).
            int nextStart = end - OVERLAP_SIZE;
            start = (nextStart > start) ? nextStart : end;
        }

        log.info("Chunked content into {} chunks (total chars: {})", chunks.size(), content.length());
        return chunks;
    }

    /**
     * Find a good breaking point near the end position.
     * Priorities: newline, period followed by space.
     */
    private int findBreakPoint(String content, int end) {
        // Look for newline
        for (int i = end - 1; i > Math.max(0, end - 100); i--) {
            if (content.charAt(i) == '\n') {
                return i;
            }
        }

        // Look for period followed by space
        for (int i = end - 1; i > Math.max(0, end - 50); i--) {
            if (content.charAt(i) == '.' && i + 1 < content.length() && content.charAt(i + 1) == ' ') {
                return i + 2; // Include the period and space
            }
        }

        // Look for space
        for (int i = end - 1; i > Math.max(0, end - 30); i--) {
            if (Character.isWhitespace(content.charAt(i))) {
                return i + 1;
            }
        }

        return end;
    }

    /**
     * Heuristic: a chunk is navigation/link-list boilerplate if it is dominated by
     * markdown links with little prose per link (menus, "Useful Links", footers).
     * Prose-only chunks (no/few links) are always kept.
     */
    boolean isNavigationBoilerplate(String chunk) {
        int linkCount = 0;
        int idx = 0;
        while ((idx = chunk.indexOf("](", idx)) != -1) {
            linkCount++;
            idx += 2;
        }
        if (linkCount < 5) {
            return false;
        }
        String prose = chunk
                .replaceAll("!\\[[^\\]]*\\]\\([^)]*\\)", " ")   // images
                .replaceAll("\\[([^\\]]*)\\]\\([^)]*\\)", "$1") // links -> label
                .replaceAll("https?://\\S+", " ")               // bare URLs
                .replaceAll("[#*`>|_]+", " ")                    // md markers
                .replaceAll("\\s+", " ").trim();
        return (prose.length() / Math.max(1, linkCount)) < 40;
    }

    /**
     * Chunk size in tokens (approximately).
     */
    public int getChunkSizeInTokens() {
        // Rough estimate: 4 chars = 1 token
        return CHUNK_SIZE / 4;
    }
}