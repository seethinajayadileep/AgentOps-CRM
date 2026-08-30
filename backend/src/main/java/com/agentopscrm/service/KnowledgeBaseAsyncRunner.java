package com.agentopscrm.service;

import com.agentopscrm.entity.enums.AgentActionStatus;
import com.agentopscrm.entity.enums.KnowledgeBaseJobStatus;
import com.agentopscrm.repository.DocumentRepository;
import com.agentopscrm.repository.KnowledgeBaseJobRepository;
import com.agentopscrm.util.SafeErrorMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Executes the knowledge-base build on a background thread and persists
 * live stage/progress updates.
 */
@Component
public class KnowledgeBaseAsyncRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseAsyncRunner.class);

    private final KnowledgeBaseJobRepository jobRepository;
    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentRepository documentRepository;
    private final KnowledgeBaseJobProgressWriter progressWriter;
    private final AuditLogService auditLogService;

    public KnowledgeBaseAsyncRunner(
            KnowledgeBaseJobRepository jobRepository,
            KnowledgeBaseService knowledgeBaseService,
            DocumentRepository documentRepository,
            KnowledgeBaseJobProgressWriter progressWriter,
            AuditLogService auditLogService) {
        this.jobRepository = jobRepository;
        this.knowledgeBaseService = knowledgeBaseService;
        this.documentRepository = documentRepository;
        this.progressWriter = progressWriter;
        this.auditLogService = auditLogService;
    }

    @Async
    public void runBuild(UUID jobId, UUID businessId) {
        try {
            long documentsTotal = documentRepository.countByBusinessId(businessId);
            progressWriter.updateProgress(jobId, KnowledgeBaseJobStatus.CHUNKING, 20,
                    0, (int) documentsTotal, 0, 0);

            KnowledgeBaseService.BuildResult result = knowledgeBaseService.buildKnowledgeBase(
                    businessId,
                    (stage, documentsProcessed, total, chunksCreated, embeddingsCreated, percent) -> {
                        progressWriter.updateProgress(jobId, stage, percent,
                                documentsProcessed, total, chunksCreated, embeddingsCreated);
                        if (stage == KnowledgeBaseJobStatus.EMBEDDING && documentsProcessed == 1) {
                            auditLogService.logAgentAction(businessId, "KnowledgeBaseBuilder", "BUILD_KB_PROGRESS",
                                    "{\"jobId\":\"" + jobId + "\"}",
                                    "{\"status\":\"EMBEDDING\"}",
                                    AgentActionStatus.SUCCESS, 0L);
                        }
                    });

            applyResult(jobId, businessId, result);
        } catch (Exception e) {
            log.error("Async knowledge-base build failed for job {} (business {})", jobId, businessId, e);
            failJob(jobId, businessId, SafeErrorMessages.KB_FAILED);
        }
    }

    private void applyResult(UUID jobId, UUID businessId, KnowledgeBaseService.BuildResult result) {
        KnowledgeBaseJobStatus status;
        if (result.isSuccess()) {
            status = KnowledgeBaseJobStatus.COMPLETED;
        } else if ("NO_DOCUMENTS".equals(result.getStatus()) || "NO_EMBEDDINGS".equals(result.getStatus())) {
            status = KnowledgeBaseJobStatus.PARTIAL;
        } else {
            status = KnowledgeBaseJobStatus.FAILED;
        }
        progressWriter.complete(jobId, status, result.getDocumentsProcessed(),
                result.getChunksCreated(), result.getEmbeddingsCreated(),
                status == KnowledgeBaseJobStatus.COMPLETED ? null : result.getMessage());
    }

    private void failJob(UUID jobId, UUID businessId, String safeMessage) {
        progressWriter.complete(jobId, KnowledgeBaseJobStatus.FAILED, 0, 0, 0, safeMessage);
        auditLogService.logAgentActionWithError(businessId, "KnowledgeBaseBuilder", "BUILD_KB_FAILED",
                "{\"jobId\":\"" + jobId + "\"}",
                "{\"status\":\"FAILED\"}",
                AgentActionStatus.ERROR,
                safeMessage,
                0L);
    }
}
