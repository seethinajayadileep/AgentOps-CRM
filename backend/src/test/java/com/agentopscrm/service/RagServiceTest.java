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
 * Unit tests for {@link RagService} (F-004).
 *
 * Focus: business isolation and clean error handling. All collaborators are mocked;
 * no database or network access is performed.
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
        b.setName("Acme");
        b.setWebsiteUrl("https://acme.test");
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

    // 6. Empty search query validation.
    @Test
    void search_emptyQuery_throwsRagSearchException() {
        UUID businessId = UUID.randomUUID();
        assertThrows(RagService.RagSearchException.class,
                () -> ragService.search(businessId, "   "));
        // Validation happens before any repository access.
        verifyNoInteractions(businessRepository, knowledgeChunkRepository);
    }

    // 5. Invalid business ID returns clean error.
    @Test
    void search_businessNotFound_throwsBusinessNotFoundException() {
        UUID businessId = UUID.randomUUID();
        when(businessRepository.findById(businessId)).thenReturn(Optional.empty());

        assertThrows(BusinessNotFoundException.class,
                () -> ragService.search(businessId, "pricing"));
        verify(knowledgeChunkRepository, never()).findByBusinessId(any());
    }

    // 4. Empty knowledge base returns a clean, empty response.
    @Test
    void search_noChunks_returnsEmptyResult() throws Exception {
        UUID businessId = UUID.randomUUID();
        when(businessRepository.findById(businessId)).thenReturn(Optional.of(business(businessId)));
        when(knowledgeChunkRepository.findByBusinessId(businessId)).thenReturn(List.of());

        RagService.SearchResult result = ragService.search(businessId, "pricing");

        assertEquals(0, result.getTotalResults());
        assertTrue(result.getResults().isEmpty());
    }

    // 2 + 3. Search is scoped to the requested business and never leaks another business's chunks.
    @Test
    void search_isBusinessScoped_doesNotLeakOtherBusiness() throws Exception {
        UUID businessAId = UUID.randomUUID();
        UUID businessBId = UUID.randomUUID();
        Business businessA = business(businessAId);

        UUID chunkA1 = UUID.randomUUID();
        UUID chunkA2 = UUID.randomUUID();
        UUID chunkBId = UUID.randomUUID(); // must NEVER appear in results for business A

        when(businessRepository.findById(businessAId)).thenReturn(Optional.of(businessA));
        when(embeddingService.isConfigured()).thenReturn(true);
        when(embeddingService.generateEmbedding(anyString())).thenReturn(new float[]{1f, 0f});
        when(vectorStoreService.vectorStringToEmbedding(anyString())).thenReturn(new float[]{1f, 0f});
        when(vectorStoreService.cosineSimilarity(any(), any())).thenReturn(0.9f);

        // Repository returns ONLY business A's chunks (isolation enforced at the query layer).
        when(knowledgeChunkRepository.findByBusinessId(businessAId)).thenReturn(List.of(
                chunk(chunkA1, businessA, "Acme pricing details", "[1,0]", "https://acme.test/pricing", "Pricing"),
                chunk(chunkA2, businessA, "Acme plans overview", "[1,0]", "https://acme.test/plans", "Plans")
        ));

        RagService.SearchResult result = ragService.search(businessAId, "pricing");

        assertEquals(2, result.getTotalResults());
        List<String> ids = result.getResults().stream().map(RagService.RagResult::getChunkId).toList();
        assertTrue(ids.contains(chunkA1.toString()));
        assertTrue(ids.contains(chunkA2.toString()));
        assertFalse(ids.contains(chunkBId.toString()), "Business B chunk must not leak into business A results");

        // Source URL + document title are surfaced.
        assertNotNull(result.getResults().get(0).getSourceUrl());
        assertNotNull(result.getResults().get(0).getDocumentTitle());

        // Only business A was ever queried.
        verify(knowledgeChunkRepository).findByBusinessId(businessAId);
        verify(knowledgeChunkRepository, never()).findByBusinessId(businessBId);
    }

    // 7. Embedding provider failure is handled properly.
    @Test
    void search_embeddingFailure_throwsRagSearchException() throws Exception {
        UUID businessId = UUID.randomUUID();
        Business b = business(businessId);
        when(businessRepository.findById(businessId)).thenReturn(Optional.of(b));
        when(embeddingService.isConfigured()).thenReturn(true);
        when(knowledgeChunkRepository.findByBusinessId(businessId)).thenReturn(List.of(
                chunk(UUID.randomUUID(), b, "content", "[1,0]", "https://acme.test", null)
        ));
        when(embeddingService.generateEmbedding(anyString()))
                .thenThrow(new EmbeddingService.EmbeddingException("provider down"));

        assertThrows(RagService.RagSearchException.class,
                () -> ragService.search(businessId, "pricing"));
    }

    // 8. Vector store failure is handled properly.
    @Test
    void search_vectorStoreFailure_throwsRagSearchException() throws Exception {
        UUID businessId = UUID.randomUUID();
        Business b = business(businessId);
        when(businessRepository.findById(businessId)).thenReturn(Optional.of(b));
        when(embeddingService.isConfigured()).thenReturn(true);
        when(knowledgeChunkRepository.findByBusinessId(businessId)).thenReturn(List.of(
                chunk(UUID.randomUUID(), b, "content", "[1,0]", "https://acme.test", null)
        ));
        when(embeddingService.generateEmbedding(anyString())).thenReturn(new float[]{1f, 0f});
        when(vectorStoreService.vectorStringToEmbedding(anyString()))
                .thenThrow(new RuntimeException("vector store failure"));

        assertThrows(RagService.RagSearchException.class,
                () -> ragService.search(businessId, "pricing"));
    }

    // ANSWER: grounded LLM answer + sources when context is strong.
    @Test
    void answer_success_returnsGroundedAnswerAndSources() throws Exception {
        UUID businessId = UUID.randomUUID();
        Business b = business(businessId);
        String prose = "The Media Ant is a media buying agency that helps brands plan and book "
                + "advertising across television, radio, print, outdoor and digital channels.";

        when(businessRepository.findById(businessId)).thenReturn(Optional.of(b));
        when(embeddingService.isConfigured()).thenReturn(true);
        when(embeddingService.generateEmbedding(anyString())).thenReturn(new float[]{1f, 0f});
        when(knowledgeChunkRepository.findByBusinessId(businessId)).thenReturn(List.of(
                chunk(UUID.randomUUID(), b, prose, "[1,0]", "https://acme.test/services", "Services")
        ));
        when(vectorStoreService.vectorStringToEmbedding(anyString())).thenReturn(new float[]{1f, 0f});
        when(vectorStoreService.cosineSimilarity(any(), any())).thenReturn(0.9f);
        when(answerService.isConfigured()).thenReturn(true);
        when(answerService.generateAnswer(anyString(), anyList()))
                .thenReturn("The Media Ant offers media buying and advertising services.");

        RagService.AnswerResult result = ragService.answer(businessId, "what services do you offer", 5);

        assertEquals("COMPLETED", result.getStatus());
        assertEquals("The Media Ant offers media buying and advertising services.", result.getAnswer());
        assertTrue(result.getSources().contains("https://acme.test/services"));
        assertEquals(1, result.getResults().size()); // raw chunks still returned for debugging
        verify(answerService).generateAnswer(anyString(), anyList());
    }

    // ANSWER: weak similarity -> fixed fallback, WITHOUT calling the LLM (no hallucination).
    @Test
    void answer_weakContext_returnsFallbackWithoutCallingLlm() throws Exception {
        UUID businessId = UUID.randomUUID();
        Business b = business(businessId);

        when(businessRepository.findById(businessId)).thenReturn(Optional.of(b));
        when(embeddingService.isConfigured()).thenReturn(true);
        when(embeddingService.generateEmbedding(anyString())).thenReturn(new float[]{1f, 0f});
        when(knowledgeChunkRepository.findByBusinessId(businessId)).thenReturn(List.of(
                chunk(UUID.randomUUID(), b, "Some loosely related paragraph of text here.",
                        "[1,0]", "https://acme.test", null)
        ));
        when(vectorStoreService.vectorStringToEmbedding(anyString())).thenReturn(new float[]{1f, 0f});
        when(vectorStoreService.cosineSimilarity(any(), any())).thenReturn(0.05f); // below threshold
        when(answerService.isConfigured()).thenReturn(true);

        RagService.AnswerResult result = ragService.answer(businessId, "unrelated question", 5);

        assertEquals("WEAK_CONTEXT", result.getStatus());
        assertEquals(AnswerService.INSUFFICIENT_CONTEXT_ANSWER, result.getAnswer());
        assertTrue(result.getSources().isEmpty());
        verify(answerService, never()).generateAnswer(any(), any());
    }

    // ANSWER: empty knowledge base -> NO_RESULTS fallback, business-scoped (never queries another business).
    @Test
    void answer_emptyKnowledgeBase_returnsNoResultsFallback() throws Exception {
        UUID businessId = UUID.randomUUID();
        when(businessRepository.findById(businessId)).thenReturn(Optional.of(business(businessId)));
        when(knowledgeChunkRepository.findByBusinessId(businessId)).thenReturn(List.of());

        RagService.AnswerResult result = ragService.answer(businessId, "anything", 5);

        assertEquals("NO_RESULTS", result.getStatus());
        assertEquals(AnswerService.INSUFFICIENT_CONTEXT_ANSWER, result.getAnswer());
        assertTrue(result.getResults().isEmpty());
        verify(answerService, never()).generateAnswer(any(), any());
    }
}
