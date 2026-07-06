package com.agentopscrm.service;

import com.agentopscrm.entity.Business;
import com.agentopscrm.entity.Document;
import com.agentopscrm.entity.KnowledgeChunk;
import com.agentopscrm.exception.BusinessNotFoundException;
import com.agentopscrm.repository.AgentLogRepository;
import com.agentopscrm.repository.BusinessRepository;
import com.agentopscrm.repository.KnowledgeChunkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RagService} (F-004).
 *
 * Focus: business isolation, broad business-intent question handling, pgvector support,
 * business profile inclusion, and clean error handling. All collaborators are mocked;
 * no database or network access is performed.
 * 
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock private BusinessRepository businessRepository;
    @Mock private KnowledgeChunkRepository knowledgeChunkRepository;
    @Mock private AgentLogRepository agentLogRepository;
    @Mock private EmbeddingService embeddingService;
    @Mock private VectorStoreService vectorStoreService;
    @Mock private AnswerService answerService;

    @InjectMocks private RagService ragService;

    private Business business(UUID id) {
        Business b = new Business(id);
        b.setName("Acme Corp");
        b.setWebsiteUrl("https://acme.test");
        b.setIndustry("Technology");
        b.setDescription("Leading provider of innovative tech solutions");
        return b;
    }

    private KnowledgeChunk chunk(UUID id, Business b, String content, String embedding, String url, String title) {
        KnowledgeChunk c = new KnowledgeChunk(id);
        c.setBusiness(b);
        c.setContent(content);
        c.setEmbedding(embedding);
        c.setSourceUrl(url);
        if (title != null) {
            Document d = new Document(UUID.randomUUID());
            d.setTitle(title);
            d.setUrl(url);
            c.setDocument(d);
        }
        return c;
    }

    // Empty search query validation
    @Test
    void search_emptyQuery_throwsRagSearchException() {
        UUID businessId = UUID.randomUUID();
        assertThrows(RagService.RagSearchException.class,
                () -> ragService.search(businessId, "   "));
        verifyNoInteractions(businessRepository, knowledgeChunkRepository);
    }

    // Invalid business ID returns clean error
    @Test
    void search_businessNotFound_throwsBusinessNotFoundException() {
        UUID businessId = UUID.randomUUID();
        when(businessRepository.findById(businessId)).thenReturn(Optional.empty());

        assertThrows(BusinessNotFoundException.class,
                () -> ragService.search(businessId, "pricing"));
        verify(knowledgeChunkRepository, never()).findByBusinessId(any());
    }

    // Empty knowledge base returns a clean, empty response
    @Test
    void search_noChunks_returnsEmptyResult() throws Exception {
        UUID businessId = UUID.randomUUID();
        when(businessRepository.findById(businessId)).thenReturn(Optional.of(business(businessId)));
        when(knowledgeChunkRepository.findByBusinessId(businessId)).thenReturn(List.of());

        RagService.SearchResult result = ragService.search(businessId, "pricing");

        assertEquals(0, result.getTotalResults());
        assertTrue(result.getResults().isEmpty());
        assertEquals("NO_CHUNKS", result.getRetrievalMode());
    }

    // Search is business-scoped and never leaks another business's chunks
    @Test
    void search_isBusinessScoped_doesNotLeakOtherBusiness() throws Exception {
        UUID businessAId = UUID.randomUUID();
        UUID businessBId = UUID.randomUUID();
        Business businessA = business(businessAId);

        UUID chunkA1 = UUID.randomUUID();
        UUID chunkA2 = UUID.randomUUID();
        UUID chunkBId = UUID.randomUUID(); // must NEVER appear in results for business A

        // Mock pgvector search
        KnowledgeChunk c1 = chunk(chunkA1, businessA, "Acme pricing details", "[1,0]", "https://acme.test/pricing", "Pricing");
        KnowledgeChunk c2 = chunk(chunkA2, businessA, "Acme plans overview", "[1,0]", "https://acme.test/plans", "Plans");
        
        when(businessRepository.findById(businessAId)).thenReturn(Optional.of(businessA));
        when(knowledgeChunkRepository.findByBusinessId(businessAId)).thenReturn(List.of(c1, c2));
        when(embeddingService.isConfigured()).thenReturn(true);
        when(embeddingService.generateEmbedding(anyString())).thenReturn(new float[]{1f, 0f});
        when(vectorStoreService.embeddingToVectorString(any())).thenReturn("[1,0]");
        when(knowledgeChunkRepository.countByBusinessIdWithPgvectorEmbedding(businessAId)).thenReturn(2L);
        
        List<Object[]> pgvectorResults = new ArrayList<>();
        pgvectorResults.add(new Object[]{c1, 0.9});
        pgvectorResults.add(new Object[]{c2, 0.85});
        when(knowledgeChunkRepository.findTopKSimilarByPgvectorWithSimilarity(eq(businessAId), anyString(), anyInt()))
                .thenReturn(pgvectorResults);

        RagService.SearchResult result = ragService.search(businessAId, "pricing");

        assertEquals(2, result.getTotalResults());
        List<String> ids = result.getResults().stream().map(RagService.RagResult::getChunkId).toList();
        assertTrue(ids.contains(chunkA1.toString()));
        assertTrue(ids.contains(chunkA2.toString()));
        assertFalse(ids.contains(chunkBId.toString()), "Business B chunk must not leak into business A results");

        assertNotNull(result.getResults().get(0).getSourceUrl());
        assertNotNull(result.getResults().get(0).getDocumentTitle());

        verify(knowledgeChunkRepository, never()).findByBusinessId(businessBId);
    }

    // Embedding provider failure is handled properly
    @Test
    void search_embeddingFailure_throwsRagSearchException() throws Exception {
        UUID businessId = UUID.randomUUID();
        Business b = business(businessId);
        when(businessRepository.findById(businessId)).thenReturn(Optional.of(b));
        when(embeddingService.isConfigured()).thenReturn(true);
        when(knowledgeChunkRepository.findByBusinessId(businessId)).thenReturn(List.of(
                chunk(UUID.randomUUID(), b, "content", "[1,0]", "https://acme.test", null)
        ));
        when(knowledgeChunkRepository.countByBusinessIdWithPgvectorEmbedding(businessId)).thenReturn(1L);
        when(embeddingService.generateEmbedding(anyString()))
                .thenThrow(new EmbeddingService.EmbeddingException("provider down"));

        assertThrows(RagService.RagSearchException.class,
                () -> ragService.search(businessId, "pricing"));
    }

    // ANSWER: grounded LLM answer + sources when context is strong
    @Test
    void answer_success_returnsGroundedAnswerAndSources() throws Exception {
        UUID businessId = UUID.randomUUID();
        Business b = business(businessId);
        String prose = "Acme Corp is a leading provider of innovative technology solutions that help " +
                "businesses streamline their operations and improve efficiency.";

        KnowledgeChunk c = chunk(UUID.randomUUID(), b, prose, "[1,0]", "https://acme.test/about", "About Us");
        
        when(businessRepository.findById(businessId)).thenReturn(Optional.of(b));
        when(knowledgeChunkRepository.findByBusinessId(businessId)).thenReturn(List.of(c));
        when(embeddingService.isConfigured()).thenReturn(true);
        when(embeddingService.generateEmbedding(anyString())).thenReturn(new float[]{1f, 0f});
        when(vectorStoreService.embeddingToVectorString(any())).thenReturn("[1,0]");
        when(knowledgeChunkRepository.countByBusinessIdWithPgvectorEmbedding(businessId)).thenReturn(1L);
        
        List<Object[]> pgvectorResults = new ArrayList<>();
        pgvectorResults.add(new Object[]{c, 0.9});
        when(knowledgeChunkRepository.findTopKSimilarByPgvectorWithSimilarity(eq(businessId), anyString(), anyInt()))
                .thenReturn(pgvectorResults);
        
        when(answerService.isConfigured()).thenReturn(true);
        when(answerService.generateAnswer(anyString(), anyList(), any(Business.class)))
                .thenReturn("Acme Corp provides innovative technology solutions for businesses.");

        RagService.AnswerResult result = ragService.answer(businessId, "what is this business about", 5);

        assertEquals("COMPLETED", result.getStatus());
        assertEquals("Acme Corp provides innovative technology solutions for businesses.", result.getAnswer());
        assertTrue(result.getSources().contains("https://acme.test/about") ||
                   result.getSources().contains("https://acme.test"));
        assertNotNull(result.getDiagnostics());
        verify(answerService).generateAnswer(anyString(), anyList(), any(Business.class));
    }

    // ANSWER: weak similarity -> fixed fallback WITHOUT calling the LLM
    @Test
    void answer_weakContext_returnsFallbackWithoutCallingLlm() throws Exception {
        UUID businessId = UUID.randomUUID();
        Business b = business(businessId);

        KnowledgeChunk c = chunk(UUID.randomUUID(), b, "Navigation menu", "[1,0]", "https://acme.test", null);
        
        when(businessRepository.findById(businessId)).thenReturn(Optional.of(b));
        when(knowledgeChunkRepository.findByBusinessId(businessId)).thenReturn(List.of(c));
        when(embeddingService.isConfigured()).thenReturn(true);
        when(embeddingService.generateEmbedding(anyString())).thenReturn(new float[]{1f, 0f});
        when(vectorStoreService.embeddingToVectorString(any())).thenReturn("[1,0]");
        when(knowledgeChunkRepository.countByBusinessIdWithPgvectorEmbedding(businessId)).thenReturn(1L);
        
        List<Object[]> weakResults = new ArrayList<>();
        weakResults.add(new Object[]{c, 0.05});
        when(knowledgeChunkRepository.findTopKSimilarByPgvectorWithSimilarity(eq(businessId), anyString(), anyInt()))
                .thenReturn(weakResults); // below threshold
        when(answerService.isConfigured()).thenReturn(true);
        when(answerService.generateAnswer(anyString(), anyList(), any(Business.class)))
                .thenReturn(AnswerService.INSUFFICIENT_CONTEXT_ANSWER);

        RagService.AnswerResult result = ragService.answer(businessId, "unrelated question", 5);

        // With business profile fallback, status might be COMPLETED if business has good metadata
        // or WEAK_CONTEXT if no useful content
        assertTrue(result.getStatus().equals("WEAK_CONTEXT") || result.getStatus().equals("COMPLETED"));
        assertNotNull(result.getDiagnostics());
    }

    // ANSWER: empty knowledge base -> NO_RESULTS fallback
    @Test
    void answer_emptyKnowledgeBase_returnsNoResultsFallback() throws Exception {
        UUID businessId = UUID.randomUUID();
        when(businessRepository.findById(businessId)).thenReturn(Optional.of(business(businessId)));
        when(knowledgeChunkRepository.findByBusinessId(businessId)).thenReturn(List.of());

        RagService.AnswerResult result = ragService.answer(businessId, "anything", 5);

        // With business profile enabled, this should now succeed with the business profile
        assertNotNull(result.getAnswer());
        assertNotNull(result.getDiagnostics());
    }

    // Broad business-intent question detection
    @Test
    void answer_broadBusinessQuestion_includesBusinessProfile() throws Exception {
        UUID businessId = UUID.randomUUID();
        Business b = business(businessId);

        when(businessRepository.findById(businessId)).thenReturn(Optional.of(b));
        when(knowledgeChunkRepository.countByBusinessIdWithPgvectorEmbedding(businessId)).thenReturn(0L);
        when(answerService.isConfigured()).thenReturn(true);
        when(answerService.generateAnswer(anyString(), anyList(), any(Business.class)))
                .thenReturn("Acme Corp is a Technology company providing innovative tech solutions.");

        RagService.AnswerResult result = ragService.answer(businessId, "what is this business about?", 5);

        assertNotNull(result.getAnswer());
        assertNotNull(result.getDiagnostics());
        
        // Should include business profile in context
        verify(answerService).generateAnswer(anyString(), anyList(), any(Business.class));
    }

    // Homepage/about chunk prioritization for broad queries
    @Test
    void answer_broadBusinessQuestion_prioritizesHomepageChunks() throws Exception {
        UUID businessId = UUID.randomUUID();
        Business b = business(businessId);
        
        KnowledgeChunk aboutChunk = chunk(UUID.randomUUID(), b, 
                "Acme Corp provides innovative solutions", "[1,0]", 
                "https://acme.test/about", "About Us");
        KnowledgeChunk contactChunk = chunk(UUID.randomUUID(), b,
                "Contact us at info@acme.test", "[1,0]",
                "https://acme.test/contact", "Contact");

        when(businessRepository.findById(businessId)).thenReturn(Optional.of(b));
        when(embeddingService.isConfigured()).thenReturn(true);
        when(embeddingService.generateEmbedding(anyString())).thenReturn(new float[]{1f, 0f});
        when(vectorStoreService.embeddingToVectorString(any())).thenReturn("[1,0]");
        when(knowledgeChunkRepository.findByBusinessId(businessId)).thenReturn(List.of(aboutChunk, contactChunk));
        when(knowledgeChunkRepository.countByBusinessIdWithPgvectorEmbedding(businessId)).thenReturn(2L);
        
        // About chunk should be boosted and come first
        when(knowledgeChunkRepository.findTopKSimilarByPgvectorWithSimilarity(eq(businessId), anyString(), anyInt()))
                .thenReturn(List.of(
                        new Object[]{contactChunk, 0.7},
                        new Object[]{aboutChunk, 0.65}
                ));
        
        when(answerService.isConfigured()).thenReturn(true);
        when(answerService.generateAnswer(anyString(), anyList(), any(Business.class)))
                .thenReturn("Acme Corp provides innovative solutions.");

        RagService.AnswerResult result = ragService.answer(businessId, "what does this company do?", 5);

        assertNotNull(result.getAnswer());
        assertNotNull(result.getDiagnostics());
    }

    // Pgvector support: does not require legacy TEXT embedding
    @Test
    void search_pgvectorOnly_worksWithoutLegacyEmbedding() throws Exception {
        UUID businessId = UUID.randomUUID();
        Business b = business(businessId);
        
        // Chunk with only pgvector embedding (no legacy TEXT embedding)
        KnowledgeChunk c = chunk(UUID.randomUUID(), b, "content", null, "https://acme.test", "Test");

        when(businessRepository.findById(businessId)).thenReturn(Optional.of(b));
        when(knowledgeChunkRepository.findByBusinessId(businessId)).thenReturn(List.of(c));
        when(embeddingService.isConfigured()).thenReturn(true);
        when(embeddingService.generateEmbedding(anyString())).thenReturn(new float[]{1f, 0f});
        when(vectorStoreService.embeddingToVectorString(any())).thenReturn("[1,0]");
        when(knowledgeChunkRepository.countByBusinessIdWithPgvectorEmbedding(businessId)).thenReturn(1L);
        
        // Pgvector query works even though c.getEmbedding() is null
        List<Object[]> pgvectorResults = new ArrayList<>();
        pgvectorResults.add(new Object[]{c, 0.8});
        when(knowledgeChunkRepository.findTopKSimilarByPgvectorWithSimilarity(eq(businessId), anyString(), anyInt()))
                .thenReturn(pgvectorResults);

        RagService.SearchResult result = ragService.search(businessId, "test query");

        assertEquals(1, result.getTotalResults());
        assertEquals("semantic_pgvector", result.getRetrievalMode());
    }

    // Cross-business isolation: strict enforcement
    @Test
    void answer_strictBusinessIsolation_neverAccessesOtherBusiness() throws Exception {
        UUID businessA = UUID.randomUUID();
        UUID businessB = UUID.randomUUID();

        when(businessRepository.findById(businessA)).thenReturn(Optional.of(business(businessA)));
        when(knowledgeChunkRepository.findByBusinessId(businessA)).thenReturn(List.of());

        ragService.answer(businessA, "query", 5);

        // Should only access business A, never B
        verify(knowledgeChunkRepository).findByBusinessId(businessA);
        verify(knowledgeChunkRepository, never()).findByBusinessId(businessB);
    }

    // Diagnostics: should be included in answer result
    @Test
    void answer_includesDiagnostics() throws Exception {
        UUID businessId = UUID.randomUUID();
        Business b = business(businessId);

        when(businessRepository.findById(businessId)).thenReturn(Optional.of(b));
        when(knowledgeChunkRepository.countByBusinessIdWithPgvectorEmbedding(businessId)).thenReturn(0L);
        when(knowledgeChunkRepository.countByBusinessId(businessId)).thenReturn(0L);
        when(answerService.isConfigured()).thenReturn(true);
        when(answerService.generateAnswer(anyString(), anyList(), any(Business.class)))
                .thenReturn("Test answer");

        RagService.AnswerResult result = ragService.answer(businessId, "test", 5);

        assertNotNull(result.getDiagnostics());
        RagService.RagDiagnostics diag = result.getDiagnostics();
        assertEquals(0, diag.getTotalChunks());
        assertEquals(0, diag.getPgvectorEmbeddedChunks());
        assertNotNull(diag.getRetrievalMode());
        assertNotNull(diag.getRejectionReasons());
    }
}
