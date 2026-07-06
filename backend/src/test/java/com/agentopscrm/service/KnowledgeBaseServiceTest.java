package com.agentopscrm.service;

import com.agentopscrm.entity.Business;
import com.agentopscrm.entity.Document;
import com.agentopscrm.exception.BusinessNotFoundException;
import com.agentopscrm.repository.AgentLogRepository;
import com.agentopscrm.repository.BusinessRepository;
import com.agentopscrm.repository.DocumentRepository;
import com.agentopscrm.repository.KnowledgeChunkRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link KnowledgeBaseService} (F-004).
 *
 * All collaborators are mocked; no database or network access is performed.
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeBaseServiceTest {

    @Mock private BusinessRepository businessRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private KnowledgeChunkRepository knowledgeChunkRepository;
    @Mock private AgentLogRepository agentLogRepository;
    @Mock private ChunkingService chunkingService;
    @Mock private EmbeddingService embeddingService;
    @Mock private VectorStoreService vectorStoreService;
    @Mock private EntityManager entityManager;

    @InjectMocks private KnowledgeBaseService knowledgeBaseService;

    @BeforeEach
    void injectEntityManager() {
        // @InjectMocks uses constructor injection and skips the @PersistenceContext
        // field, so wire the EntityManager mock in manually.
        ReflectionTestUtils.setField(knowledgeBaseService, "entityManager", entityManager);
    }

    private Business business(UUID id) {
        Business b = new Business(id);
        b.setName("Acme");
        b.setWebsiteUrl("https://acme.test");
        return b;
    }

    private Document document(UUID id, Business b, String content) {
        Document d = new Document(id);
        d.setBusiness(b);
        d.setUrl("https://acme.test/page");
        d.setTitle("Page");
        d.setContent(content);
        return d;
    }

    // 1. Build knowledge base for a business (happy path) + vectors persisted.
    @Test
    void build_success_persistsChunksAndEmbeddings() throws Exception {
        UUID businessId = UUID.randomUUID();
        Business b = business(businessId);
        Document doc = document(UUID.randomUUID(), b, "Some meaningful business content about pricing and services.");

        when(businessRepository.findById(businessId)).thenReturn(Optional.of(b));
        when(documentRepository.findByBusinessId(businessId)).thenReturn(List.of(doc));
        when(documentRepository.findById(any())).thenReturn(Optional.of(doc));
        when(embeddingService.isConfigured()).thenReturn(true);
        when(knowledgeChunkRepository.findByBusinessId(businessId)).thenReturn(List.of());
        when(chunkingService.chunkContent(anyString())).thenReturn(List.of("chunk one", "chunk two"));
        when(embeddingService.generateEmbeddings(anyList()))
                .thenReturn(List.of(new float[]{0.1f, 0.2f}, new float[]{0.3f, 0.4f}));
        when(vectorStoreService.embeddingToVectorString(any())).thenReturn("[0.1,0.2]");
        when(entityManager.getReference(eq(Business.class), any())).thenReturn(b);
        when(entityManager.getReference(eq(Document.class), any())).thenReturn(doc);

        KnowledgeBaseService.BuildResult result = knowledgeBaseService.buildKnowledgeBase(businessId);

        assertTrue(result.isSuccess());
        assertEquals("COMPLETED", result.getStatus());
        assertEquals(1, result.getDocumentsProcessed());
        assertEquals(2, result.getChunksCreated());
        assertEquals(2, result.getEmbeddingsCreated());
        assertEquals(businessId, result.getBusinessId());
        verify(knowledgeChunkRepository, times(2)).save(any());
    }

    // 4. Empty crawled documents return a clean (non-error) response.
    @Test
    void build_noDocuments_returnsCleanResponse() throws Exception {
        UUID businessId = UUID.randomUUID();
        when(businessRepository.findById(businessId)).thenReturn(Optional.of(business(businessId)));
        when(documentRepository.findByBusinessId(businessId)).thenReturn(List.of());

        KnowledgeBaseService.BuildResult result = knowledgeBaseService.buildKnowledgeBase(businessId);

        assertTrue(result.isSuccess());
        assertEquals("NO_DOCUMENTS", result.getStatus());
        assertEquals(0, result.getDocumentsProcessed());
        assertEquals(0, result.getChunksCreated());
        verify(knowledgeChunkRepository, never()).save(any());
    }

    // 5. Invalid business ID returns a clean error.
    @Test
    void build_businessNotFound_throwsBusinessNotFoundException() {
        UUID businessId = UUID.randomUUID();
        when(businessRepository.findById(businessId)).thenReturn(Optional.empty());

        assertThrows(BusinessNotFoundException.class,
                () -> knowledgeBaseService.buildKnowledgeBase(businessId));
    }

    // 7. Embedding provider failure is handled properly (clean failure, nothing saved).
    @Test
    void build_embeddingFailure_returnsFailureResult() throws Exception {
        UUID businessId = UUID.randomUUID();
        Business b = business(businessId);
        Document doc = document(UUID.randomUUID(), b, "content to embed");

        when(businessRepository.findById(businessId)).thenReturn(Optional.of(b));
        when(documentRepository.findByBusinessId(businessId)).thenReturn(List.of(doc));
        when(documentRepository.findById(any())).thenReturn(Optional.of(doc));
        when(embeddingService.isConfigured()).thenReturn(true);
        when(knowledgeChunkRepository.findByBusinessId(businessId)).thenReturn(List.of());
        when(chunkingService.chunkContent(anyString())).thenReturn(List.of("c1"));
        when(embeddingService.generateEmbeddings(anyList()))
                .thenThrow(new EmbeddingService.EmbeddingException("provider down"));

        KnowledgeBaseService.BuildResult result = knowledgeBaseService.buildKnowledgeBase(businessId);

        assertFalse(result.isSuccess());
        assertEquals("EMBEDDING_FAILED", result.getStatus());
        verify(knowledgeChunkRepository, never()).save(any());
    }

    // Embedding provider not configured -> clean failure (no vectors can be built).
    @Test
    void build_embeddingNotConfigured_returnsFailureResult() throws Exception {
        UUID businessId = UUID.randomUUID();
        Business b = business(businessId);
        Document doc = document(UUID.randomUUID(), b, "content");

        when(businessRepository.findById(businessId)).thenReturn(Optional.of(b));
        when(documentRepository.findByBusinessId(businessId)).thenReturn(List.of(doc));
        when(embeddingService.isConfigured()).thenReturn(false);

        KnowledgeBaseService.BuildResult result = knowledgeBaseService.buildKnowledgeBase(businessId);

        assertFalse(result.isSuccess());
        assertEquals("EMBEDDING_NOT_CONFIGURED", result.getStatus());
        verify(knowledgeChunkRepository, never()).save(any());
    }
}
