package com.agentopscrm.service;

import com.agentopscrm.dto.KnowledgeBaseJobResponse;
import com.agentopscrm.entity.KnowledgeBaseJob;
import com.agentopscrm.entity.enums.KnowledgeBaseJobStatus;
import com.agentopscrm.exception.BusinessNotFoundException;
import com.agentopscrm.repository.BusinessRepository;
import com.agentopscrm.repository.KnowledgeBaseJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for Bug 2: asynchronous knowledge-base build job workflow.
 *
 * Covers:
 * - POST build immediately returns a QUEUED job (never blocks on the actual work).
 * - Duplicate active builds for the same business are prevented.
 * - Job completion is observable via polling even after the frontend's own
 *   timeout duration would have elapsed (simulated by asserting on job state
 *   directly rather than the HTTP request/response cycle).
 * - Page refresh restores the active job (getActiveJob).
 * - Stale jobs are reconciled to FAILED.
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeBaseJobServiceTest {

    @Mock private KnowledgeBaseJobRepository jobRepository;
    @Mock private BusinessRepository businessRepository;
    @Mock private KnowledgeBaseAsyncRunner asyncRunner;

    private KnowledgeBaseJobService service;

    @BeforeEach
    void setUp() {
        service = new KnowledgeBaseJobService(jobRepository, businessRepository, asyncRunner);
        ReflectionTestUtils.setField(service, "staleTimeoutMinutes", 15L);
        lenient().when(jobRepository.save(any(KnowledgeBaseJob.class))).thenAnswer(inv -> {
            KnowledgeBaseJob job = inv.getArgument(0);
            if (job.getId() == null) {
                job.setId(UUID.randomUUID());
            }
            return job;
        });
    }

    @Test
    void startBuild_returnsQueuedJobImmediately_andTriggersAsyncRunner() {
        UUID businessId = UUID.randomUUID();
        when(businessRepository.existsById(businessId)).thenReturn(true);
        when(jobRepository.findByBusinessIdAndStatusIn(eq(businessId), any())).thenReturn(List.of());

        KnowledgeBaseJobResponse response = service.startBuild(businessId);

        assertNotNull(response.getJobId());
        assertEquals(businessId, response.getBusinessId());
        assertEquals(KnowledgeBaseJobStatus.QUEUED, response.getStatus());
        assertNotNull(response.getStartedAt());
        verify(asyncRunner, times(1)).runBuild(any(), eq(businessId));
    }

    @Test
    void startBuild_whenBusinessMissing_throwsBusinessNotFoundException() {
        UUID businessId = UUID.randomUUID();
        when(businessRepository.existsById(businessId)).thenReturn(false);

        assertThrows(BusinessNotFoundException.class, () -> service.startBuild(businessId));
        verify(asyncRunner, never()).runBuild(any(), any());
    }

    @Test
    void startBuild_whenActiveJobExists_returnsExistingJob_preventsDuplicate() {
        UUID businessId = UUID.randomUUID();
        UUID existingJobId = UUID.randomUUID();
        KnowledgeBaseJob existing = new KnowledgeBaseJob(existingJobId);
        existing.setBusinessId(businessId);
        existing.setStatus(KnowledgeBaseJobStatus.EMBEDDING);
        existing.setStartedAt(LocalDateTime.now());

        when(businessRepository.existsById(businessId)).thenReturn(true);
        when(jobRepository.findByBusinessIdAndStatusIn(eq(businessId), any())).thenReturn(List.of(existing));

        KnowledgeBaseJobResponse response = service.startBuild(businessId);

        assertEquals(existingJobId, response.getJobId());
        assertEquals(KnowledgeBaseJobStatus.EMBEDDING, response.getStatus());
        // No new job created, no duplicate async run triggered.
        verify(jobRepository, never()).save(any());
        verify(asyncRunner, never()).runBuild(any(), any());
    }

    @Test
    void getJob_afterAsyncCompletion_reflectsCompletedStatus_evenAfterTimeoutWindow() {
        // Simulates polling after the frontend's own request timeout would have
        // elapsed: the job is looked up directly (as a poll would), and by the
        // time this call happens the async runner has already completed it.
        UUID jobId = UUID.randomUUID();
        KnowledgeBaseJob job = new KnowledgeBaseJob(jobId);
        job.setBusinessId(UUID.randomUUID());
        job.setStatus(KnowledgeBaseJobStatus.COMPLETED);
        job.setProgressPercentage(100);
        job.setChunksCreated(42);
        job.setEmbeddingsCreated(42);
        job.setStartedAt(LocalDateTime.now().minusSeconds(45)); // longer than a typical 30s frontend timeout
        job.setCompletedAt(LocalDateTime.now());

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        KnowledgeBaseJobResponse response = service.getJob(jobId);

        assertEquals(KnowledgeBaseJobStatus.COMPLETED, response.getStatus());
        assertEquals(100, response.getProgressPercentage());
        assertEquals(42, response.getChunksCreated());
        assertNotNull(response.getCompletedAt());
    }

    @Test
    void getJob_whenMissing_throwsNoSuchElementException() {
        UUID jobId = UUID.randomUUID();
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.getJob(jobId));
    }

    @Test
    void getActiveJob_restoresInProgressJob_forPageRefresh() {
        UUID businessId = UUID.randomUUID();
        KnowledgeBaseJob active = new KnowledgeBaseJob(UUID.randomUUID());
        active.setBusinessId(businessId);
        active.setStatus(KnowledgeBaseJobStatus.CHUNKING);
        active.setStartedAt(LocalDateTime.now());

        when(jobRepository.findByBusinessIdAndStatusIn(eq(businessId), any())).thenReturn(List.of(active));

        Optional<KnowledgeBaseJobResponse> result = service.getActiveJob(businessId);

        assertTrue(result.isPresent());
        assertEquals(KnowledgeBaseJobStatus.CHUNKING, result.get().getStatus());
    }

    @Test
    void getActiveJob_whenNoneActive_returnsEmpty() {
        UUID businessId = UUID.randomUUID();
        when(jobRepository.findByBusinessIdAndStatusIn(eq(businessId), any())).thenReturn(List.of());

        assertTrue(service.getActiveJob(businessId).isEmpty());
    }

    @Test
    void reconcileStaleJobs_marksOldActiveJobAsFailed() {
        KnowledgeBaseJob stale = new KnowledgeBaseJob(UUID.randomUUID());
        stale.setBusinessId(UUID.randomUUID());
        stale.setStatus(KnowledgeBaseJobStatus.EMBEDDING);
        stale.setStartedAt(LocalDateTime.now().minusHours(2));
        stale.setUpdatedAt(LocalDateTime.now().minusHours(1)); // older than 15 min timeout

        when(jobRepository.findByStatusIn(any())).thenReturn(List.of(stale));

        int reconciled = service.reconcileStaleJobs();

        assertEquals(1, reconciled);
        assertEquals(KnowledgeBaseJobStatus.FAILED, stale.getStatus());
        assertNotNull(stale.getErrorMessage());
        assertNotNull(stale.getCompletedAt());
    }

    @Test
    void reconcileStaleJobs_leavesRecentActiveJobUntouched() {
        KnowledgeBaseJob recent = new KnowledgeBaseJob(UUID.randomUUID());
        recent.setBusinessId(UUID.randomUUID());
        recent.setStatus(KnowledgeBaseJobStatus.CHUNKING);
        recent.setStartedAt(LocalDateTime.now());
        recent.setUpdatedAt(LocalDateTime.now().minusMinutes(1)); // within 15 min timeout

        when(jobRepository.findByStatusIn(any())).thenReturn(List.of(recent));

        int reconciled = service.reconcileStaleJobs();

        assertEquals(0, reconciled);
        assertEquals(KnowledgeBaseJobStatus.CHUNKING, recent.getStatus());
    }

    @Test
    void reconcileOnStartup_doesNotThrow_recoveringUnfinishedJobsAfterRestart() {
        KnowledgeBaseJob stale = new KnowledgeBaseJob(UUID.randomUUID());
        stale.setBusinessId(UUID.randomUUID());
        stale.setStatus(KnowledgeBaseJobStatus.QUEUED);
        stale.setStartedAt(LocalDateTime.now().minusHours(3));
        stale.setUpdatedAt(LocalDateTime.now().minusHours(3));

        when(jobRepository.findByStatusIn(any())).thenReturn(List.of(stale));

        assertDoesNotThrow(() -> service.reconcileOnStartup());
        assertEquals(KnowledgeBaseJobStatus.FAILED, stale.getStatus());
    }
}
