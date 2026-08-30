package com.agentopscrm.service;

import com.agentopscrm.entity.enums.KnowledgeBaseJobStatus;
import com.agentopscrm.repository.KnowledgeBaseJobRepository;
import com.agentopscrm.util.SafeErrorMessages;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Commits knowledge-base job progress in independent transactions so polling
 * sees live stages while the long build transaction is still open.
 */
@Component
public class KnowledgeBaseJobProgressWriter {

    private final KnowledgeBaseJobRepository jobRepository;

    public KnowledgeBaseJobProgressWriter(KnowledgeBaseJobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateProgress(UUID jobId, KnowledgeBaseJobStatus status, int percent,
                               int documentsProcessed, int documentsTotal,
                               int chunksCreated, int embeddingsCreated) {
        jobRepository.findById(jobId).ifPresent(job -> {
            if (job.getStatus() != null && job.getStatus().isTerminal()) {
                return;
            }
            job.setStatus(status);
            job.setProgressPercentage(Math.max(0, Math.min(100, percent)));
            job.setDocumentsProcessed(documentsProcessed);
            job.setDocumentsTotal(documentsTotal);
            job.setChunksCreated(chunksCreated);
            job.setEmbeddingsCreated(embeddingsCreated);
            job.setUpdatedAt(LocalDateTime.now());
            jobRepository.saveAndFlush(job);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(UUID jobId, KnowledgeBaseJobStatus status, int documentsProcessed,
                         int chunksCreated, int embeddingsCreated, String errorMessage) {
        jobRepository.findById(jobId).ifPresent(job -> {
            if (job.getStatus() != null && job.getStatus().isTerminal()) {
                return;
            }
            job.setStatus(status);
            job.setProgressPercentage(100);
            job.setDocumentsProcessed(documentsProcessed);
            job.setChunksCreated(chunksCreated);
            job.setEmbeddingsCreated(embeddingsCreated);
            if (errorMessage != null) {
                job.setErrorMessage(SafeErrorMessages.sanitize(errorMessage));
            }
            job.setUpdatedAt(LocalDateTime.now());
            job.setCompletedAt(LocalDateTime.now());
            jobRepository.saveAndFlush(job);
        });
    }
}
