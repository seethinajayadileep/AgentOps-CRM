package com.agentopscrm.service;

import com.agentopscrm.entity.KnowledgeBaseJob;
import com.agentopscrm.entity.enums.KnowledgeBaseJobStatus;
import com.agentopscrm.repository.DocumentRepository;
import com.agentopscrm.repository.KnowledgeBaseJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Executes the knowledge-base build (chunk + embed) on a background thread
 * (Bug 2 fix), separate from {@link KnowledgeBaseJobService} to allow Spring's
 * {@code @Async} proxy to intercept the call (self-invocation on the same bean
 * would otherwise bypass the async proxy).
 *
 * @author AgentOps Team
 * @version 0.1.0
 */
@Component
public class KnowledgeBaseAsyncRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseAsyncRunner.class);

    private final KnowledgeBaseJobRepository jobRepository;
    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentRepository documentRepository;

    public KnowledgeBaseAsyncRunner(
            KnowledgeBaseJobRepository jobRepository,
            KnowledgeBaseService knowledgeBaseService,
            DocumentRepository documentRepository) {
        this.jobRepository = jobRepository;
        this.knowledgeBaseService = knowledgeBaseService;
        this.documentRepository = documentRepository;
    }

    /**
     * Runs the full build workflow for the given job on a background thread.
     * Never throws - all failures are captured and persisted on the job so the
     * frontend can display a safe error message instead of the request
     * silently failing.
     */
    @Async
    public void runBuild(UUID jobId, UUID businessId) {
        try {
            markStatus(jobId, KnowledgeBaseJobStatus.CHUNKING, null);

            long documentsTotal = documentRepository.countByBusinessId(businessId);
            updateDocumentsTotal(jobId, (int) documentsTotal);

            markStatus(jobId, KnowledgeBaseJobStatus.EMBEDDING, null);

            KnowledgeBaseService.BuildResult result = knowledgeBaseService.buildKnowledgeBase(businessId);

            applyResult(jobId, result);
        } catch (Exception e) {
            log.error("Async knowledge-base build failed for job {} (business {})", jobId, businessId, e);
            failJob(jobId, safeMessage(e.getMessage()));
        }
    }

    @Transactional
    protected void markStatus(UUID jobId, KnowledgeBaseJobStatus status, String errorMessage) {
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(status);
            if (errorMessage != null) {
                job.setErrorMessage(errorMessage);
            }
            job.setUpdatedAt(LocalDateTime.now());
            job.setProgressPercentage(progressFor(status));
            jobRepository.save(job);
        });
    }

    @Transactional
    protected void updateDocumentsTotal(UUID jobId, int documentsTotal) {
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setDocumentsTotal(documentsTotal);
            job.setUpdatedAt(LocalDateTime.now());
            jobRepository.save(job);
        });
    }

    @Transactional
    protected void applyResult(UUID jobId, KnowledgeBaseService.BuildResult result) {
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setDocumentsProcessed(result.getDocumentsProcessed());
            job.setChunksCreated(result.getChunksCreated());
            job.setEmbeddingsCreated(result.getEmbeddingsCreated());
            job.setUpdatedAt(LocalDateTime.now());
            job.setCompletedAt(LocalDateTime.now());

            if (result.isSuccess()) {
                job.setStatus(KnowledgeBaseJobStatus.COMPLETED);
                job.setProgressPercentage(100);
            } else if ("NO_DOCUMENTS".equals(result.getStatus()) || "NO_EMBEDDINGS".equals(result.getStatus())) {
                // Not a hard failure - documents processed but nothing usable was produced.
                job.setStatus(KnowledgeBaseJobStatus.PARTIAL);
                job.setProgressPercentage(100);
                job.setErrorMessage(safeMessage(result.getMessage()));
            } else {
                job.setStatus(KnowledgeBaseJobStatus.FAILED);
                job.setProgressPercentage(100);
                job.setErrorMessage(safeMessage(result.getMessage()));
            }
            jobRepository.save(job);
        });
    }

    @Transactional
    protected void failJob(UUID jobId, String safeMessage) {
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(KnowledgeBaseJobStatus.FAILED);
            job.setErrorMessage(safeMessage);
            job.setProgressPercentage(100);
            job.setUpdatedAt(LocalDateTime.now());
            job.setCompletedAt(LocalDateTime.now());
            jobRepository.save(job);
        });
    }

    private int progressFor(KnowledgeBaseJobStatus status) {
        return switch (status) {
            case QUEUED -> 0;
            case CRAWLING -> 10;
            case CHUNKING -> 30;
            case EMBEDDING -> 60;
            case COMPLETED, PARTIAL, FAILED -> 100;
        };
    }

    /**
     * Strip anything that could resemble an API key/secret from error messages
     * before persisting/returning them to the frontend.
     */
    private String safeMessage(String message) {
        if (message == null) {
            return "An unexpected error occurred while building the knowledge base.";
        }
        return message.replaceAll("(sk-[a-zA-Z0-9]{10,})", "[REDACTED]")
                .replaceAll("(Bearer\\s+[a-zA-Z0-9._-]{10,})", "Bearer [REDACTED]");
    }
}
