package com.agentopscrm.service;

import com.agentopscrm.client.ApifyClient;
import com.agentopscrm.entity.LeadSourceRun;
import com.agentopscrm.entity.enums.LeadSourceRunStatus;
import com.agentopscrm.repository.AgentLogRepository;
import com.agentopscrm.repository.BusinessRepository;
import com.agentopscrm.repository.DiscoveredLeadRepository;
import com.agentopscrm.repository.LeadRepository;
import com.agentopscrm.repository.LeadSourceRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for Bug 4: Apify Lead Finder run reconciliation.
 *
 * Covers:
 * - 401 Unauthorized from Apify is mapped to FAILED with failureCode=APIFY_UNAUTHORIZED
 *   and a safe user-facing message (never the raw token/response).
 * - A SUCCEEDED Apify run is mapped to COMPLETED locally.
 * - Locally stale RUNNING runs (no update within the configured timeout) are marked
 *   FAILED even if Apify itself is unreachable.
 * - Reconciliation is idempotent and safe to run repeatedly / on startup.
 */
@ExtendWith(MockitoExtension.class)
class LeadFinderReconciliationTest {

    @Mock private LeadSourceRunRepository runRepository;
    @Mock private DiscoveredLeadRepository discoveredLeadRepository;
    @Mock private LeadRepository leadRepository;
    @Mock private BusinessRepository businessRepository;
    @Mock private AgentLogRepository agentLogRepository;
    @Mock private ApifyClient apifyClient;

    private LeadFinderService service;

    @BeforeEach
    void setUp() {
        // staleRunTimeoutMinutes = 30, matching the production default.
        service = new LeadFinderService(
            runRepository, discoveredLeadRepository, leadRepository,
            businessRepository, agentLogRepository, apifyClient, 30L);
        lenient().when(runRepository.save(any(LeadSourceRun.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void syncRun_on401Unauthorized_marksFailedWithSafeCodeAndMessage() throws Exception {
        UUID runId = UUID.randomUUID();
        LeadSourceRun run = new LeadSourceRun(runId);
        run.setApifyRunId("run-401");
        run.setStatus(LeadSourceRunStatus.RUNNING);

        when(runRepository.findById(runId)).thenReturn(java.util.Optional.of(run));
        when(apifyClient.isConfigured()).thenReturn(true);

        ApifyClient.ApifyException unauthorized = new ApifyClient.ApifyException(
            "Apify credentials were rejected. Update APIFY_API_TOKEN.");
        unauthorized.unauthorized = true;
        when(apifyClient.getRun("run-401")).thenThrow(unauthorized);

        var response = service.syncRun(runId);

        assertEquals(LeadSourceRunStatus.FAILED, response.getStatus());
        assertEquals(LeadFinderService.FAILURE_CODE_APIFY_UNAUTHORIZED, response.getFailureCode());
        assertEquals(LeadFinderService.SAFE_UNAUTHORIZED_MESSAGE, response.getFailureReason());
        // The safe message references the *variable name* APIFY_API_TOKEN (guidance for
        // operators) but must never contain an actual secret/token value.
        assertFalse(response.getFailureReason().contains("sk-"));
        assertFalse(response.getFailureReason().contains("Bearer "));
    }

    @Test
    void startRun_on401Unauthorized_marksFailedImmediately() throws Exception {
        when(apifyClient.isConfigured()).thenReturn(true);
        when(apifyClient.getDefaultActorId()).thenReturn("some-actor");

        ApifyClient.ApifyException unauthorized = new ApifyClient.ApifyException(
            "Apify credentials were rejected. Update APIFY_API_TOKEN.");
        unauthorized.unauthorized = true;
        when(apifyClient.startActorRun(anyString(), any(), any(), any(), any())).thenThrow(unauthorized);

        com.agentopscrm.dto.StartLeadFinderRunRequest req = new com.agentopscrm.dto.StartLeadFinderRunRequest();
        req.setSearchName("Test Search");

        var response = service.startRun(req);

        assertEquals(LeadSourceRunStatus.FAILED, response.getStatus());
        assertEquals(LeadFinderService.FAILURE_CODE_APIFY_UNAUTHORIZED, response.getFailureCode());
        assertEquals(LeadFinderService.SAFE_UNAUTHORIZED_MESSAGE, response.getFailureReason());
    }

    @Test
    void syncRun_whenApifySucceeded_mapsToCompletedLocally() throws Exception {
        UUID runId = UUID.randomUUID();
        LeadSourceRun run = new LeadSourceRun(runId);
        run.setApifyRunId("run-ok");
        run.setApifyDatasetId("ds-ok");
        run.setStatus(LeadSourceRunStatus.RUNNING);

        when(runRepository.findById(runId)).thenReturn(java.util.Optional.of(run));
        when(apifyClient.isConfigured()).thenReturn(true);

        ApifyClient.ApifyRunInfo info = new ApifyClient.ApifyRunInfo();
        info.runId = "run-ok";
        info.datasetId = "ds-ok";
        info.status = "SUCCEEDED";
        when(apifyClient.getRun("run-ok")).thenReturn(info);
        when(apifyClient.fetchDatasetItems("ds-ok")).thenReturn(List.of());
        when(discoveredLeadRepository.countByLeadSourceRunId(runId)).thenReturn(0L);

        var response = service.syncRun(runId);

        assertEquals(LeadSourceRunStatus.COMPLETED, response.getStatus());
        assertNull(response.getFailureCode());
    }

    @Test
    void syncRun_whenApifyFailedAbortedOrTimedOut_mapsToFailedLocally() throws Exception {
        for (String apifyStatus : List.of("FAILED", "ABORTED", "TIMED-OUT")) {
            UUID runId = UUID.randomUUID();
            LeadSourceRun run = new LeadSourceRun(runId);
            run.setApifyRunId("run-x");
            run.setStatus(LeadSourceRunStatus.RUNNING);

            when(runRepository.findById(runId)).thenReturn(java.util.Optional.of(run));
            when(apifyClient.isConfigured()).thenReturn(true);

            ApifyClient.ApifyRunInfo info = new ApifyClient.ApifyRunInfo();
            info.runId = "run-x";
            info.status = apifyStatus;
            when(apifyClient.getRun("run-x")).thenReturn(info);

            var response = service.syncRun(runId);

            assertEquals(LeadSourceRunStatus.FAILED, response.getStatus(),
                "Apify status " + apifyStatus + " should map to local FAILED");
        }
    }

    @Test
    void reconcileActiveRuns_marksStaleRunningRunAsFailed_withoutCallingApify() {
        LeadSourceRun staleRun = new LeadSourceRun(UUID.randomUUID());
        staleRun.setStatus(LeadSourceRunStatus.RUNNING);
        staleRun.setApifyRunId("run-stale");
        staleRun.setLastSyncedAt(LocalDateTime.now().minusHours(2)); // older than 30 min timeout

        when(runRepository.findByStatusOrderByCreatedAtDesc(LeadSourceRunStatus.RUNNING))
            .thenReturn(List.of(staleRun));

        int reconciled = service.reconcileActiveRuns();

        assertEquals(1, reconciled);
        assertEquals(LeadSourceRunStatus.FAILED, staleRun.getStatus());
        assertEquals(LeadFinderService.FAILURE_CODE_STALE_TIMEOUT, staleRun.getFailureCode());
        try {
            verify(apifyClient, never()).getRun(anyString());
        } catch (ApifyClient.ApifyException e) {
            fail("Unexpected exception during verify: " + e.getMessage());
        }
    }

    @Test
    void reconcileActiveRuns_leavesFreshRunningRunUntouched_whenApifyStillRunning() throws Exception {
        LeadSourceRun freshRun = new LeadSourceRun(UUID.randomUUID());
        freshRun.setStatus(LeadSourceRunStatus.RUNNING);
        freshRun.setApifyRunId("run-fresh");
        freshRun.setLastSyncedAt(LocalDateTime.now().minusMinutes(2)); // within timeout

        when(runRepository.findByStatusOrderByCreatedAtDesc(LeadSourceRunStatus.RUNNING))
            .thenReturn(List.of(freshRun));
        when(apifyClient.isConfigured()).thenReturn(true);

        ApifyClient.ApifyRunInfo info = new ApifyClient.ApifyRunInfo();
        info.status = "RUNNING";
        when(apifyClient.getRun("run-fresh")).thenReturn(info);

        int reconciled = service.reconcileActiveRuns();

        assertEquals(0, reconciled);
        assertEquals(LeadSourceRunStatus.RUNNING, freshRun.getStatus());
    }

    @Test
    void reconcileActiveRuns_on401DuringReconciliation_marksFailedUnauthorized() throws Exception {
        LeadSourceRun run = new LeadSourceRun(UUID.randomUUID());
        run.setStatus(LeadSourceRunStatus.RUNNING);
        run.setApifyRunId("run-401b");
        run.setLastSyncedAt(LocalDateTime.now().minusMinutes(2));

        when(runRepository.findByStatusOrderByCreatedAtDesc(LeadSourceRunStatus.RUNNING))
            .thenReturn(List.of(run));
        when(apifyClient.isConfigured()).thenReturn(true);

        ApifyClient.ApifyException unauthorized = new ApifyClient.ApifyException("rejected");
        unauthorized.unauthorized = true;
        when(apifyClient.getRun("run-401b")).thenThrow(unauthorized);

        int reconciled = service.reconcileActiveRuns();

        assertEquals(1, reconciled);
        assertEquals(LeadSourceRunStatus.FAILED, run.getStatus());
        assertEquals(LeadFinderService.FAILURE_CODE_APIFY_UNAUTHORIZED, run.getFailureCode());
    }

    @Test
    void reconcileOnStartup_doesNotThrow_evenWhenApifyUnreachable() throws Exception {
        LeadSourceRun run = new LeadSourceRun(UUID.randomUUID());
        run.setStatus(LeadSourceRunStatus.RUNNING);
        run.setApifyRunId("run-restart");
        run.setLastSyncedAt(LocalDateTime.now().minusMinutes(2));

        when(runRepository.findByStatusOrderByCreatedAtDesc(LeadSourceRunStatus.RUNNING))
            .thenReturn(List.of(run));
        when(apifyClient.isConfigured()).thenReturn(true);
        when(apifyClient.getRun("run-restart")).thenThrow(new ApifyClient.ApifyException("network down"));

        // Simulates reconciliation firing after an application restart; must never throw,
        // regardless of Apify reachability, so application startup is never blocked.
        assertDoesNotThrow(() -> service.reconcileOnStartup());
    }
}
