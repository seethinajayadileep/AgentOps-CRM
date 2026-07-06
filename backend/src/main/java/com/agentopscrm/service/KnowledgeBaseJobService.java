package com.agentopscrm.service;

import com.agentopscrm.dto.KnowledgeBaseJobResponse;
import com.agentopscrm.entity.KnowledgeBaseJob;
import com.agentopscrm.entity.enums.KnowledgeBaseJobStatus;
import com.agentopscrm.exception.BusinessNotFoundException;
import com.agentopscrm.repository.BusinessRepository;
import com.agentopscrm.repository.KnowledgeBaseJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates the asynchronous knowledge-base build job workflow (Bug 2 fix).
 *
 * Responsibilities:
 *  - Accept a build request, create a QUEUED job, and immediately return (202)
 *    while the actual chunk/embed work runs on a background thread via
 *    {@link KnowledgeBaseAsyncRunner}.
 *  - Prevent duplicate active builds for the same business.
 *  - Expose job status/progress for polling.
 *  - Restore the active job for a business (page-refresh recovery).
 *  - Reconcile stale jobs (no progress within a configured timeout) to FAILED,
 *    both periodically and once on application startup.
 *
 * @author AgentOps Team
 * @version 0.1.0
 */
@Service
public class KnowledgeBaseJobService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseJobService.class);

    private static final List<KnowledgeBaseJobStatus> ACTIVE_STATUSES = List.of(
            KnowledgeBaseJobStatus.QUEUED,
            KnowledgeBaseJobStatus.CRAWLING,
            KnowledgeBaseJobStatus.CHUNKING,
            KnowledgeBaseJobStatus.EMBEDDING);

    private final KnowledgeBaseJobRepository jobRepository;
    private final BusinessRepository businessRepository;
    private final KnowledgeBaseAsyncRunner asyncRunner;

    @Value("${kb.job-stale-timeout-minutes:15}")
    private long staleTimeoutMinutes;

    public KnowledgeBaseJobService(
            KnowledgeBaseJobRepository jobRepository,
            BusinessRepository businessRepository,
            KnowledgeBaseAsyncRunner asyncRunner) {
        this.jobRepository = jobRepository;
        this.businessRepository = businessRepository;
        this.asyncRunner = asyncRunner;
    }

    /**
     * Start (or return the existing) build job for a business.
     *
     * @throws BusinessNotFoundException if the business does not exist
     */
    @Transactional
    public KnowledgeBaseJobResponse startBuild(UUID businessId) {
        if (!businessRepository.existsById(businessId)) {
            throw new BusinessNotFoundException("Business not found: " + businessId);
        }

        // Prevent duplicate active builds for the same business (Bug 2 requirement 7).
        List<KnowledgeBaseJob> active = jobRepository.findByBusinessIdAndStatusIn(businessId, ACTIVE_STATUSES);
        if (!active.isEmpty()) {
            log.info("Build already in progress for business {}; returning existing job", businessId);
            return toResponse(active.get(0));
        }

        KnowledgeBaseJob job = new KnowledgeBaseJob();
        job.setBusinessId(businessId);
        job.setStatus(KnowledgeBaseJobStatus.QUEUED);
        job.setStartedAt(LocalDateTime.now());
        job = jobRepository.save(job);

        // Kick off the background work. This call returns immediately because
        // KnowledgeBaseAsyncRunner#runBuild is @Async on a distinct bean, so
        // Spring's async proxy intercepts it rather than running it inline.
        asyncRunner.runBuild(job.getId(), businessId);

        return toResponse(job);
    }

    /**
     * Get the current status/progress of a job.
     */
    @Transactional(readOnly = true)
    public KnowledgeBaseJobResponse getJob(UUID jobId) {
        KnowledgeBaseJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NoSuchElementException("Knowledge base job not found: " + jobId));
        return toResponse(job);
    }

    /**
     * Find the most recent active (in-progress) job for a business, if any -
     * used by the frontend to restore polling after a page refresh.
     */
    @Transactional(readOnly = true)
    public Optional<KnowledgeBaseJobResponse> getActiveJob(UUID businessId) {
        return jobRepository.findByBusinessIdAndStatusIn(businessId, ACTIVE_STATUSES).stream()
                .findFirst()
                .map(this::toResponse);
    }

    /**
     * Reconcile stale jobs once the application context is fully started, so
     * jobs left active across a restart are not stuck forever in the UI.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void reconcileOnStartup() {
        try {
            int reconciled = reconcileStaleJobs();
            log.info("Startup reconciliation of knowledge-base jobs complete: {} updated", reconciled);
        } catch (Exception e) {
            log.error("Startup reconciliation of knowledge-base jobs failed", e);
        }
    }

    /**
     * Periodic stale-job reconciliation.
     */
    @Scheduled(fixedDelayString = "${kb.job-reconcile-interval-ms:300000}")
    public void scheduledReconciliation() {
        try {
            reconcileStaleJobs();
        } catch (Exception e) {
            log.error("Scheduled reconciliation of knowledge-base jobs failed", e);
        }
    }

    /**
     * Mark any active job with no update within the configured timeout as
     * FAILED (stale-job recovery, Bug 2 requirement 9).
     *
     * @return number of jobs reconciled
     */
    @Transactional
    public int reconcileStaleJobs() {
        List<KnowledgeBaseJob> activeJobs = jobRepository.findByStatusIn(ACTIVE_STATUSES);
        int reconciled = 0;
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(staleTimeoutMinutes);
        for (KnowledgeBaseJob job : activeJobs) {
            LocalDateTime reference = job.getUpdatedAt() != null ? job.getUpdatedAt() : job.getStartedAt();
            if (reference != null && reference.isBefore(threshold)) {
                job.setStatus(KnowledgeBaseJobStatus.FAILED);
                job.setErrorMessage("Knowledge base build did not complete within " + staleTimeoutMinutes
                        + " minutes and was marked FAILED automatically.");
                job.setProgressPercentage(100);
                job.setCompletedAt(LocalDateTime.now());
                jobRepository.save(job);
                reconciled++;
            }
        }
        if (reconciled > 0) {
            log.info("Reconciled {} stale knowledge-base job(s)", reconciled);
        }
        return reconciled;
    }

    private KnowledgeBaseJobResponse toResponse(KnowledgeBaseJob job) {
        KnowledgeBaseJobResponse r = new KnowledgeBaseJobResponse();
        r.setJobId(job.getId());
        r.setBusinessId(job.getBusinessId());
        r.setStatus(job.getStatus());
        r.setProgressPercentage(job.getProgressPercentage());
        r.setDocumentsTotal(job.getDocumentsTotal());
        r.setDocumentsProcessed(job.getDocumentsProcessed());
        r.setChunksCreated(job.getChunksCreated());
        r.setEmbeddingsCreated(job.getEmbeddingsCreated());
        r.setErrorMessage(job.getErrorMessage());
        r.setStartedAt(job.getStartedAt());
        r.setUpdatedAt(job.getUpdatedAt());
        r.setCompletedAt(job.getCompletedAt());
        return r;
    }
}
