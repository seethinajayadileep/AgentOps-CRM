package com.agentopscrm.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShowcaseActionFilterTest {

    @Test
    void blocksPaidAndDestructivePreviewActions() {
        assertTrue(ShowcaseActionFilter.isBlocked("POST", "/api/leads/abc/voice-calls/start"));
        assertTrue(ShowcaseActionFilter.isBlocked("POST", "/api/lead-finder/runs"));
        assertTrue(ShowcaseActionFilter.isBlocked("PUT", "/api/approvals/abc/approve"));
        assertTrue(ShowcaseActionFilter.isBlocked("DELETE", "/api/businesses/abc"));
    }

    @Test
    void allowsInspectionAndReadPaths() {
        assertFalse(ShowcaseActionFilter.isBlocked("GET", "/api/dashboard/stats"));
        assertFalse(ShowcaseActionFilter.isBlocked("GET", "/api/lead-finder/runs"));
        assertFalse(ShowcaseActionFilter.isBlocked("PUT", "/api/approvals/abc/reject"));
        assertFalse(ShowcaseActionFilter.isBlocked("POST", "/api/lead-finder/runs/abc/sync"));
    }
}
