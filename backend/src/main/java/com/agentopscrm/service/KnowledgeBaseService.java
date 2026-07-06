package com.agentopscrm.service;

import com.agentopscrm.entity.Business;
import com.agentopscrm.entity.Document;
import com.agentopscrm.entity.KnowledgeChunk;
import com.agentopscrm.entity.enums.AgentActionStatus;
import com.agentopscrm.exception.BusinessNotFoundException;
import com.agentopscrm.repository.BusinessRepository;
import com.agentopscrm.repository.DocumentRepository;
import com.agentopscrm.repository.KnowledgeChunkRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service for building the RAG knowledge base from crawled documents.
 *
 * Responsibilities (F-004):
 *  - Fetch crawled {@link Document}s belonging ONLY to the target business.
 *  - Split each document into chunks via {@link ChunkingService}.
 *  - Generate embeddings via {@link EmbeddingService}.
 *  - Persist {@link KnowledgeChunk}s (with embeddings) scoped to the business.
 *
 * Memory: documents are processed ONE AT A TIME (chunk → embed → save → flush →
 * clear). This bounds peak heap to a single document's chunks/vectors, so large
 * crawls (dozens of pages → thousands of 1536-dim vectors) do not exhaust the heap.
 *
 * Business isolation: every read/write is filtered by businessId; chunks are always
 * attached to the target business, so one business can never write into another's KB.
 *
 * @author AgentOps Team
 * @version 0.3.1
 */
@Service
public class KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);

    private static final String AGENT_NAME = "KnowledgeBaseBuilder";

    private final BusinessRepository businessRepository;
    private final DocumentRepository documentRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final AuditLogService auditLogService;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;

    @PersistenceContext
    private EntityManager entityManager;

    public KnowledgeBaseService(
            BusinessRepository businessRepository,
            DocumentRepository documentRepository,
            KnowledgeChunkRepository knowledgeChunkRepository,
            AuditLogService auditLogService,
            ChunkingService chunkingService,
            EmbeddingService embeddingService,
            VectorStoreService vectorStoreService) {
        this.businessRepository = businessRepository;
        this.documentRepository = documentRepository;
        this.knowledgeChunkRepository = knowledgeChunkRepository;
        this.auditLogService = auditLogService;
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
    }

    /**
     * Build the knowledge base for a business from its crawled documents.
     *
     * @param businessId the business ID (isolation boundary)
     * @return a {@link BuildResult} with detailed metrics
     * @throws BusinessNotFoundException if the business does not exist
     */
    @Transactional
    public BuildResult buildKnowledgeBase(UUID businessId) throws BusinessNotFoundException {
        long startTime = System.currentTimeMillis();
        
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new BusinessNotFoundException("Business not found: " + businessId));

        auditLogService.logAgentAction(businessId, AGENT_NAME, "BUILD_KB_STARTED",
                "{\"businessId\":\"" + businessId + "\"}",
                "{\"status\":\"started\"}",
                AgentActionStatus.SUCCESS,
                0L);

        // Only documents belonging to THIS business.
        List<Document> documents = documentRepository.findByBusinessId(businessId);

        // Clean response for the empty-documents case (not an error).
        if (documents.isEmpty()) {
            String message = "No crawled documents found. Please crawl the website first.";
            auditLogService.logAgentAction(businessId, AGENT_NAME, "BUILD_KB_COMPLETED",
                    "{\"businessId\":\"" + businessId + "\"}",
                    "{\"status\":\"NO_DOCUMENTS\",\"documentsProcessed\":0}",
                    AgentActionStatus.SUCCESS,
                    System.currentTimeMillis() - startTime);
            return new BuildResult(true, "NO_DOCUMENTS", message, businessId, 0, 0, 0, 0);
        }

        // Embedding provider must be configured to store vectors.
        if (!embeddingService.isConfigured()) {
            String message = "Embedding provider is not configured. Set OPENAI_API_KEY to build the knowledge base.";
            auditLogService.logAgentActionWithError(businessId, AGENT_NAME, "BUILD_KB_FAILED",
                    "{\"businessId\":\"" + businessId + "\"}",
                    "{\"status\":\"EMBEDDING_NOT_CONFIGURED\"}",
                    AgentActionStatus.ERROR,
                    "Embedding provider not configured",
                    System.currentTimeMillis() - startTime);
            return new BuildResult(false, "EMBEDDING_NOT_CONFIGURED", message, businessId,
                    documents.size(), 0, 0, 0);
        }

        // Snapshot ids so we don't rely on managed Document instances after em.clear().
        List<UUID> documentIds = new ArrayList<>(documents.size());
        for (Document d : documents) {
            documentIds.add(d.getId());
        }

        try {
            Set<String> existingHashes = getExistingContentHashes(businessId);
            int chunksCreated = 0;
            int embeddingsCreated = 0;
            int skipped = 0;

            // Process ONE document at a time to bound memory.
            for (UUID documentId : documentIds) {
                Document document = documentRepository.findById(documentId).orElse(null);
                if (document == null) {
                    continue;
                }
                String content = document.getContent();
                String sourceUrl = document.getUrl();
                if (content == null || content.isBlank()) {
                    continue;
                }

                List<String> chunks = chunkingService.chunkContent(content);

                // Filter out duplicates, keeping original chunk index.
                List<Integer> newIndexes = new ArrayList<>();
                List<String> newTexts = new ArrayList<>();
                for (int index = 0; index < chunks.size(); index++) {
                    String text = chunks.get(index);
                    String hash = contentHash(text);
                    if (existingHashes.contains(hash)) {
                        skipped++;
                        continue;
                    }
                    existingHashes.add(hash);
                    newIndexes.add(index);
                    newTexts.add(text);
                }

                if (newTexts.isEmpty()) {
                    continue;
                }

                // Embed just THIS document's new chunks.
                List<float[]> vectors;
                try {
                    vectors = embeddingService.generateEmbeddings(newTexts);
                } catch (EmbeddingService.EmbeddingException e) {
                    log.error("Embedding generation failed for business {} (document {})", businessId, documentId, e);
                    auditLogService.logAgentActionWithError(businessId, AGENT_NAME, "BUILD_KB_FAILED",
                            "{\"businessId\":\"" + businessId + "\",\"documentsProcessed\":" + documents.size() + "}",
                            "{\"status\":\"EMBEDDING_FAILED\",\"chunksCreated\":" + chunksCreated 
                                    + ",\"embeddingsCreated\":" + embeddingsCreated + "}",
                            AgentActionStatus.ERROR,
                            safe(e.getMessage()),
                            System.currentTimeMillis() - startTime);
                    return new BuildResult(false, "EMBEDDING_FAILED",
                            "Failed to generate embeddings: " + e.getMessage(), businessId,
                            documents.size(), chunksCreated, embeddingsCreated, skipped);
                }

                if (vectors.size() != newTexts.size()) {
                    throw new IllegalStateException("Embedding count (" + vectors.size()
                            + ") != chunk count (" + newTexts.size() + ") for document " + documentId);
                }

                // Persist this document's chunks.
                Business businessRef = entityManager.getReference(Business.class, businessId);
                Document documentRef = entityManager.getReference(Document.class, documentId);
                for (int i = 0; i < newTexts.size(); i++) {
                    float[] vector = vectors.get(i);
                    KnowledgeChunk chunk = new KnowledgeChunk();
                    chunk.setBusiness(businessRef);
                    chunk.setDocument(documentRef);
                    chunk.setContent(newTexts.get(i));
                    chunk.setSourceUrl(sourceUrl);
                    chunk.setChunkIndex(newIndexes.get(i));
                    
                    // Store embedding in TEXT column (legacy for rollback compatibility)
                    if (vector != null && vector.length > 0) {
                        String vectorString = vectorStoreService.embeddingToVectorString(vector);
                        chunk.setEmbedding(vectorString);
                        embeddingsCreated++;
                    }
                    
                    // Save entity first (without vector column)
                    knowledgeChunkRepository.save(chunk);
                    chunksCreated++;
                    
                    // Update vector column separately using native SQL (Hibernate can't bind vector type)
                    if (vector != null && vector.length > 0) {
                        try {
                            String vectorString = vectorStoreService.embeddingToVectorString(vector);
                            knowledgeChunkRepository.updateEmbeddingVector(chunk.getId(), vectorString);
                        } catch (Exception e) {
                            log.warn("Failed to update pgvector embedding for chunk {}: {}. TEXT embedding preserved.", 
                                    chunk.getId(), e.getMessage());
                        }
                    }
                }

                // Flush + clear so this document's entities/vectors can be GC'd.
                entityManager.flush();
                entityManager.clear();
            }

            String message = String.format(
                    "Knowledge base built. %d chunks created, %d embeddings stored, %d duplicates skipped.",
                    chunksCreated, embeddingsCreated, skipped);

            auditLogService.logAgentAction(businessId, AGENT_NAME, "BUILD_KB_COMPLETED",
                    "{\"businessId\":\"" + businessId + "\",\"documentsProcessed\":" + documents.size() + "}",
                    "{\"status\":\"COMPLETED\",\"chunksCreated\":" + chunksCreated
                            + ",\"embeddingsCreated\":" + embeddingsCreated + ",\"skipped\":" + skipped + "}",
                    AgentActionStatus.SUCCESS,
                    System.currentTimeMillis() - startTime);

            return new BuildResult(true, "COMPLETED", message, businessId,
                    documents.size(), chunksCreated, embeddingsCreated, skipped);

        } catch (Exception e) {
            log.error("Failed to build knowledge base for business {}", businessId, e);
            long duration = System.currentTimeMillis() - startTime;
            auditLogService.logAgentActionWithError(businessId, AGENT_NAME, "BUILD_KB_FAILED",
                    "{\"businessId\":\"" + businessId + "\",\"documentsProcessed\":" + documents.size() + "}",
                    "{\"status\":\"FAILED\"}",
                    AgentActionStatus.ERROR,
                    safe(e.getMessage()),
                    duration);
            return new BuildResult(false, "FAILED",
                    "Failed to build knowledge base: " + e.getMessage(), businessId,
                    documents.size(), 0, 0, 0);
        }
    }

    /**
     * Existing chunk content hashes for a business (for deduplication).
     */
    private Set<String> getExistingContentHashes(UUID businessId) {
        Set<String> hashes = new HashSet<>();
        knowledgeChunkRepository.findByBusinessId(businessId).forEach(chunk -> {
            if (chunk.getContent() != null) {
                hashes.add(contentHash(chunk.getContent()));
            }
        });
        return hashes;
    }

    private String contentHash(String content) {
        return Integer.toHexString(content.hashCode());
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "'").replace("\n", " ");
    }

    /**
     * Result of a knowledge base build operation.
     */
    public static class BuildResult {
        private final boolean success;
        private final String status;
        private final String message;
        private final UUID businessId;
        private final int documentsProcessed;
        private final int chunksCreated;
        private final int embeddingsCreated;
        private final int skipped;

        public BuildResult(boolean success, String status, String message, UUID businessId,
                           int documentsProcessed, int chunksCreated, int embeddingsCreated, int skipped) {
            this.success = success;
            this.status = status;
            this.message = message;
            this.businessId = businessId;
            this.documentsProcessed = documentsProcessed;
            this.chunksCreated = chunksCreated;
            this.embeddingsCreated = embeddingsCreated;
            this.skipped = skipped;
        }

        public boolean isSuccess() { return success; }
        public String getStatus() { return status; }
        public String getMessage() { return message; }
        public UUID getBusinessId() { return businessId; }
        public int getDocumentsProcessed() { return documentsProcessed; }
        public int getChunksCreated() { return chunksCreated; }
        public int getEmbeddingsCreated() { return embeddingsCreated; }
        public int getSkipped() { return skipped; }
    }
}
