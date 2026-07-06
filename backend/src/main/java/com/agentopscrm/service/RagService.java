package com.agentopscrm.service;

import com.agentopscrm.entity.AgentLog;
import com.agentopscrm.entity.Business;
import com.agentopscrm.entity.Document;
import com.agentopscrm.entity.KnowledgeChunk;
import com.agentopscrm.entity.enums.AgentActionStatus;
import com.agentopscrm.exception.BusinessNotFoundException;
import com.agentopscrm.repository.AgentLogRepository;
import com.agentopscrm.repository.BusinessRepository;
import com.agentopscrm.repository.KnowledgeChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for RAG (Retrieval-Augmented Generation) search.
 *
 * Business isolation (F-004 security requirement): every search loads chunks via
 * {@link KnowledgeChunkRepository#findByBusinessId(UUID)}. The candidate set is
 * therefore constrained to a single business at the repository layer BEFORE any
 * ranking happens. A caller cannot retrieve another business's chunks even if it
 * forges IDs, because the query itself is scoped by businessId.
 *
 * Ranking strategy:
 *  - Semantic (preferred): embed the query, compute cosine similarity against each
 *    chunk's stored embedding (see {@link VectorStoreService}).
 *  - Keyword fallback: used when no chunk has a stored embedding (e.g. legacy data)
 *    or the embedding provider is not configured.
 *
 * Business Profile: For broad business-intent questions, the business profile
 * (name, industry, description, website) is automatically included in the grounding
 * context to ensure the LLM can answer "What is this business about?" reliably.
 *
 * @author AgentOps Team
 * @version 1.0.0
 */
@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private static final String AGENT_NAME = "RagSearch";
    private static final String ANSWER_AGENT_NAME = "RagAnswer";
    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_TOP_K = 50;
    /** Chunks below this cosine similarity are considered too weak to ground an answer. */
    private static final double WEAK_SIMILARITY_THRESHOLD = 0.20;
    /** Max chunks fed to the LLM as grounding context. */
    private static final int MAX_CONTEXT_CHUNKS = 6;

    /** Broad business-intent questions that should prioritize homepage/about/services content. */
    private static final List<Pattern> BROAD_BUSINESS_INTENT_PATTERNS = List.of(
        Pattern.compile(".*what\\s+(is|are)\\s+this\\s+business\\s+(about|for).*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*what\\s+does\\s+this\\s+company\\s+do.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*describe\\s+this\\s+(business|company).*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*summarize\\s+this\\s+(business|company).*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*what\\s+services\\s+do\\s+(they|you)\\s+provide.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*who\\s+do\\s+(they|you)\\s+serve.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*what\\s+(is|are)\\s+the\\s+business.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*tell\\s+me\\s+about\\s+this\\s+(business|company).*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*who\\s+(is|are)\\s+the\\s+customers.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*what.*products.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*what.*solutions.*", Pattern.CASE_INSENSITIVE)
    );

    /** URL patterns that indicate homepage or high-value business overview pages. */
    private static final List<Pattern> PRIORITY_URL_PATTERNS = List.of(
        Pattern.compile(".*/about.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*/services.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*/products.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*/solutions.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^https?://[^/]+/?$", Pattern.CASE_INSENSITIVE) // Homepage (just domain, no path or single /)
    );

    private final BusinessRepository businessRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final AgentLogRepository agentLogRepository;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final AnswerService answerService;

    public RagService(
            BusinessRepository businessRepository,
            KnowledgeChunkRepository knowledgeChunkRepository,
            AgentLogRepository agentLogRepository,
            EmbeddingService embeddingService,
            VectorStoreService vectorStoreService,
            AnswerService answerService) {
        this.businessRepository = businessRepository;
        this.knowledgeChunkRepository = knowledgeChunkRepository;
        this.agentLogRepository = agentLogRepository;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.answerService = answerService;
    }

    /**
     * Convenience overload using the default topK.
     */
    @Transactional
    public SearchResult search(UUID businessId, String query)
            throws BusinessNotFoundException, RagSearchException {
        return search(businessId, query, null);
    }

    /**
     * Search the knowledge base for a business.
     *
     * @param businessId the business ID (isolation boundary; required)
     * @param query      the natural-language query
     * @param topK       optional max results (defaults to {@value #DEFAULT_TOP_K}, capped at {@value #MAX_TOP_K})
     * @return the top matching chunks for this business only
     * @throws BusinessNotFoundException if the business does not exist
     * @throws RagSearchException        on invalid input or search failure
     */
    @Transactional
    public SearchResult search(UUID businessId, String query, Integer topK)
            throws BusinessNotFoundException, RagSearchException {

        if (businessId == null) {
            throw new RagSearchException("businessId is required");
        }
        if (query == null || query.trim().isEmpty()) {
            throw new RagSearchException("Query cannot be empty");
        }
        // Enforce business existence at the service layer (never trust the caller).
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new BusinessNotFoundException("Business not found: " + businessId));

        String trimmedQuery = query.trim();
        int limit = resolveTopK(topK);
        boolean isBroadBusinessQuery = isBroadBusinessIntent(trimmedQuery);

        logAgentAction(business, "SEARCH_STARTED",
                "{\"businessId\":\"" + businessId + "\",\"query\":\"" + safe(trimmedQuery) + "\",\"topK\":" + limit + ",\"broadBusinessIntent\":" + isBroadBusinessQuery + "}",
                "{\"status\":\"started\"}",
                AgentActionStatus.SUCCESS);

        try {
            // SECURITY: candidate chunks are scoped to this business only.
            List<KnowledgeChunk> chunks = knowledgeChunkRepository.findByBusinessId(businessId);

            if (chunks.isEmpty()) {
                logAgentAction(business, "SEARCH_COMPLETED",
                        "{\"businessId\":\"" + businessId + "\",\"query\":\"" + safe(trimmedQuery) + "\"}",
                        "{\"status\":\"NO_CHUNKS\",\"results\":0}",
                        AgentActionStatus.SUCCESS);
                return new SearchResult(trimmedQuery, 0, Collections.emptyList(), "NO_CHUNKS");
            }

            // Check for pgvector availability - count chunks with embedding_vector
            long pgvectorChunkCount = knowledgeChunkRepository.countByBusinessIdWithPgvectorEmbedding(businessId);
            boolean hasPgvectorEmbeddings = pgvectorChunkCount > 0;

            List<RagResult> results;
            String mode;

            if (embeddingService.isConfigured() && hasPgvectorEmbeddings) {
                results = semanticSearch(trimmedQuery, chunks, limit, isBroadBusinessQuery);
                mode = "semantic_pgvector";
            } else if (embeddingService.isConfigured()) {
                // Legacy fallback: check TEXT embedding column
                boolean hasLegacyEmbeddings = chunks.stream()
                        .anyMatch(c -> c.getEmbedding() != null && !c.getEmbedding().isBlank());
                if (hasLegacyEmbeddings) {
                    results = semanticSearchLegacy(trimmedQuery, chunks, limit, isBroadBusinessQuery);
                    mode = "semantic_legacy";
                } else {
                    results = keywordFallback(trimmedQuery, chunks, limit, isBroadBusinessQuery);
                    mode = "keyword";
                }
            } else {
                results = keywordFallback(trimmedQuery, chunks, limit, isBroadBusinessQuery);
                mode = "keyword";
            }

            logAgentAction(business, "SEARCH_COMPLETED",
                    "{\"businessId\":\"" + businessId + "\",\"query\":\"" + safe(trimmedQuery) + "\"}",
                    "{\"status\":\"COMPLETED\",\"mode\":\"" + mode + "\",\"results\":" + results.size() + ",\"pgvectorChunks\":" + pgvectorChunkCount + "}",
                    AgentActionStatus.SUCCESS);

            return new SearchResult(trimmedQuery, results.size(), results, mode);

        } catch (EmbeddingService.EmbeddingException e) {
            // Embedding provider failure -> clean, explicit error.
            log.error("RAG search embedding failure for business {}", businessId, e);
            logAgentAction(business, "SEARCH_FAILED",
                    "{\"businessId\":\"" + businessId + "\",\"query\":\"" + safe(trimmedQuery) + "\"}",
                    "{\"status\":\"EMBEDDING_FAILED\",\"error\":\"" + safe(e.getMessage()) + "\"}",
                    AgentActionStatus.ERROR);
            throw new RagSearchException("Search failed while generating query embedding: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("RAG search failed for business {} with query '{}'", businessId, trimmedQuery, e);
            logAgentAction(business, "SEARCH_FAILED",
                    "{\"businessId\":\"" + businessId + "\",\"query\":\"" + safe(trimmedQuery) + "\"}",
                    "{\"status\":\"FAILED\",\"error\":\"" + safe(e.getMessage()) + "\"}",
                    AgentActionStatus.ERROR);
            throw new RagSearchException("RAG search failed: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieval-Augmented answer: retrieve business-scoped chunks, ground the configured
     * LLM on the cleaned top chunks plus business profile, and return a concise answer plus source URLs.
     * The raw retrieved chunks are still returned (for debugging/transparency).
     *
     * Business isolation is inherited from {@link #search} (chunks are scoped by businessId).
     * No hallucination: if context is weak/empty (or the provider is unavailable) a fixed
     * fallback sentence is returned WITHOUT calling the model.
     */
    @Transactional
    public AnswerResult answer(UUID businessId, String query, Integer topK)
            throws BusinessNotFoundException, RagSearchException {

        // Reuse the existing, business-scoped search (also validates business + query).
        SearchResult search = search(businessId, query, topK);
        int limit = resolveTopK(topK);
        Business business = businessRepository.findById(businessId).orElse(null);
        List<RagResult> results = search.getResults();
        boolean isBroadBusinessQuery = isBroadBusinessIntent(query);

        // Select meaningful, sufficiently-similar chunks to ground the answer.
        List<RagResult> grounding = new ArrayList<>();
        List<String> rejectionReasons = new ArrayList<>();
        
        for (RagResult r : results) {
            double sim = r.getSimilarity() == null ? 0.0 : r.getSimilarity();
            if (sim < WEAK_SIMILARITY_THRESHOLD) {
                rejectionReasons.add("chunk_" + r.getChunkId() + "_weak_similarity_" + String.format("%.2f", sim));
                continue;
            }
            String cleaned = cleanChunkText(r.getContent());
            if (isMeaningful(r.getContent(), cleaned)) {
                grounding.add(r);
            } else {
                rejectionReasons.add("chunk_" + r.getChunkId() + "_not_meaningful");
            }
            if (grounding.size() >= MAX_CONTEXT_CHUNKS) {
                break;
            }
        }

        // Weak/empty context -> try fallback for broad business questions
        if (grounding.isEmpty() && isBroadBusinessQuery && business != null) {
            log.info("No grounding chunks found for broad business query, using business profile + best available chunks");
            // Try to get at least some homepage/about chunks even if weak
            List<RagResult> fallbackChunks = results.stream()
                    .filter(r -> isPriorityChunk(r))
                    .limit(3)
                    .collect(Collectors.toList());
            grounding.addAll(fallbackChunks);
        }

        // Build business profile
        String businessProfile = buildBusinessProfile(business);

        // Weak/empty context OR provider unavailable -> fixed fallback (no model call).
        if (grounding.isEmpty() && businessProfile.isEmpty() || !answerService.isConfigured()) {
            String status = results.isEmpty() ? "NO_RESULTS"
                    : (!answerService.isConfigured() ? "ANSWER_UNAVAILABLE" : "WEAK_CONTEXT");
            logAnswer(business, businessId, query, status, 0, rejectionReasons);
            return new AnswerResult(businessId.toString(), search.getQuery(),
                    AnswerService.INSUFFICIENT_CONTEXT_ANSWER, Collections.emptyList(),
                    results, limit, status, createDiagnostics(business, search, grounding, rejectionReasons));
        }

        // Build grounding context with business profile first, then chunks + distinct source URLs.
        List<String> context = new ArrayList<>();
        List<String> sources = new ArrayList<>();
        
        // Always include business profile as the first context block for grounding
        if (!businessProfile.isEmpty()) {
            context.add(businessProfile);
            if (business != null && business.getWebsiteUrl() != null && !business.getWebsiteUrl().isBlank()) {
                sources.add(business.getWebsiteUrl());
            }
        }
        
        for (RagResult r : grounding) {
            String src = r.getSourceUrl() != null ? r.getSourceUrl() : "";
            context.add((src.isEmpty() ? "" : "Source: " + src + "\n") + cleanChunkText(r.getContent()));
            if (!src.isEmpty() && !sources.contains(src)) {
                sources.add(src);
            }
        }

        String answer;
        String status;
        try {
            answer = answerService.generateAnswer(search.getQuery(), context, business);
            status = AnswerService.INSUFFICIENT_CONTEXT_ANSWER.equals(answer.trim())
                    ? "WEAK_CONTEXT" : "COMPLETED";
        } catch (AnswerService.AnswerException e) {
            log.error("RAG answer generation failed for business {}", businessId, e);
            logAnswer(business, businessId, query, "ANSWER_FAILED", grounding.size(), rejectionReasons);
            throw new RagSearchException("Answer generation failed: " + e.getMessage(), e);
        }

        logAnswer(business, businessId, query, status, grounding.size(), rejectionReasons);
        return new AnswerResult(businessId.toString(), search.getQuery(), answer,
                sources, results, limit, status, createDiagnostics(business, search, grounding, rejectionReasons));
    }

    /**
     * Build a compact business profile for grounding context.
     */
    private String buildBusinessProfile(Business business) {
        if (business == null) {
            return "";
        }
        StringBuilder profile = new StringBuilder();
        profile.append("Business Profile:\n");
        
        if (business.getName() != null && !business.getName().isBlank()) {
            profile.append("Name: ").append(business.getName()).append("\n");
        }
        if (business.getIndustry() != null && !business.getIndustry().isBlank()) {
            profile.append("Industry: ").append(business.getIndustry()).append("\n");
        }
        if (business.getDescription() != null && !business.getDescription().isBlank()) {
            profile.append("Description: ").append(business.getDescription()).append("\n");
        }
        if (business.getWebsiteUrl() != null && !business.getWebsiteUrl().isBlank()) {
            profile.append("Website: ").append(business.getWebsiteUrl()).append("\n");
        }
        if (business.getContactEmail() != null && !business.getContactEmail().isBlank()) {
            profile.append("Contact: ").append(business.getContactEmail()).append("\n");
        }
        
        return profile.toString();
    }

    /**
     * Check if the query is a broad business-intent question.
     */
    private boolean isBroadBusinessIntent(String query) {
        if (query == null || query.isBlank()) {
            return false;
        }
        return BROAD_BUSINESS_INTENT_PATTERNS.stream()
                .anyMatch(pattern -> pattern.matcher(query).matches());
    }

    /**
     * Check if a chunk comes from a priority URL (homepage, about, services, etc.).
     */
    private boolean isPriorityChunk(RagResult result) {
        String url = result.getSourceUrl();
        String title = result.getDocumentTitle();
        
        if (url != null) {
            for (Pattern pattern : PRIORITY_URL_PATTERNS) {
                if (pattern.matcher(url).matches()) {
                    return true;
                }
            }
        }
        
        if (title != null) {
            String lowerTitle = title.toLowerCase();
            if (lowerTitle.contains("home") || lowerTitle.contains("about") || 
                lowerTitle.contains("service") || lowerTitle.contains("product") || 
                lowerTitle.contains("solution")) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Create diagnostics information for the answer result.
     */
    private RagDiagnostics createDiagnostics(Business business, SearchResult search, 
                                             List<RagResult> grounding, List<String> rejectionReasons) {
        long totalChunks = 0;
        long pgvectorChunks = 0;
        long legacyChunks = 0;
        long documentsCount = 0;
        
        if (business != null) {
            totalChunks = knowledgeChunkRepository.countByBusinessId(business.getId());
            pgvectorChunks = knowledgeChunkRepository.countByBusinessIdWithPgvectorEmbedding(business.getId());
            legacyChunks = totalChunks - pgvectorChunks;
            documentsCount = business.getDocuments() != null ? business.getDocuments().size() : 0;
        }
        
        List<ChunkDiagnostic> chunkDiags = grounding.stream()
                .map(r -> new ChunkDiagnostic(
                    r.getChunkId(),
                    r.getSourceUrl(),
                    r.getDocumentTitle(),
                    r.getSimilarity(),
                    "included"
                ))
                .collect(Collectors.toList());
        
        return new RagDiagnostics(
                documentsCount,
                totalChunks,
                legacyChunks,
                pgvectorChunks,
                search.getRetrievalMode(),
                chunkDiags,
                rejectionReasons
        );
    }

    private void logAnswer(Business business, UUID businessId, String query, String status, int contextChunks, List<String> rejectionReasons) {
        try {
            AgentLog entry = new AgentLog();
            entry.setAgentName(ANSWER_AGENT_NAME);
            entry.setAction("ANSWER");
            entry.setInputJson("{\"businessId\":\"" + businessId + "\",\"query\":\"" + safe(query) + "\"}");
            entry.setOutputJson("{\"status\":\"" + status + "\",\"contextChunks\":" + contextChunks + 
                    ",\"rejections\":" + rejectionReasons.size() + "}");
            entry.setStatus("ANSWER_FAILED".equals(status) ? AgentActionStatus.ERROR : AgentActionStatus.SUCCESS);
            entry.setBusiness(business);
            agentLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to log RAG answer action", e);
        }
    }

    /**
     * Strip markdown links/images/bare URLs and collapse whitespace, so navigation
     * boilerplate is reduced and the LLM/UI see readable prose. Source URLs are tracked
     * separately, so removing inline links here does not lose provenance.
     */
    String cleanChunkText(String content) {
        if (content == null) {
            return "";
        }
        String s = content;
        s = s.replaceAll("!\\[[^\\]]*\\]\\([^)]*\\)", " ");      // images ![alt](url)
        s = s.replaceAll("\\[([^\\]]*)\\]\\([^)]*\\)", "$1");    // links [text](url) -> text
        s = s.replaceAll("https?://\\S+", " ");                  // bare URLs
        s = s.replaceAll("[#*`>|_]+", " ");                       // md heading/emphasis/table markers
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }

    /**
     * Heuristic: keep chunks with real paragraph text; drop menus / link lists / footers.
     * Improved to better preserve homepage and about page content.
     */
    boolean isMeaningful(String original, String cleaned) {
        if (cleaned == null || cleaned.length() < 60) {  // Reduced from 80 to allow more content
            return false;
        }
        int words = cleaned.split("\\s+").length;
        if (words < 10) {  // Reduced from 12 to allow shorter but meaningful chunks
            return false;
        }
        
        // Check for cookie banner / navigation boilerplate patterns
        String lower = cleaned.toLowerCase();
        if (lower.contains("cookie") && lower.contains("accept") || 
            lower.contains("privacy") && lower.contains("policy") && words < 20 ||
            lower.matches(".*\\b(home|about|services|products|contact)\\s+(home|about|services|products|contact).*")) {
            return false;
        }
        
        // Link-heavy chunks with little prose per link are navigation/"Useful Links" boilerplate.
        int linkCount = countOccurrences(original, "](");
        if (linkCount >= 5 && (cleaned.length() / Math.max(1, linkCount)) < 30) {  // Reduced from 40
            return false;
        }
        return true;
    }

    private int countOccurrences(String haystack, String needle) {
        if (haystack == null || needle.isEmpty()) {
            return 0;
        }
        int count = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) != -1) {
            count++;
            idx += needle.length();
        }
        return count;
    }

    /**
     * Semantic ranking via pgvector cosine similarity (returns similarity scores directly from DB).
     */
    private List<RagResult> semanticSearch(String query, List<KnowledgeChunk> chunks, int limit, boolean isBroadBusinessQuery)
            throws EmbeddingService.EmbeddingException {

        float[] queryEmbedding = embeddingService.generateEmbedding(query);
        String queryVectorString = vectorStoreService.embeddingToVectorString(queryEmbedding);
        
        // Increase retrieval breadth for broad business queries
        int retrievalLimit = isBroadBusinessQuery ? Math.min(limit * 3, MAX_TOP_K) : limit;
        
        if (chunks.isEmpty()) {
            return Collections.emptyList();
        }
        
        UUID businessId = chunks.get(0).getBusiness().getId();
        
        try {
            log.debug("Using pgvector native similarity search for business {} (limit: {})", businessId, retrievalLimit);
            
            // Use native query with similarity calculation
            List<Object[]> pgvectorResults = 
                knowledgeChunkRepository.findTopKSimilarByPgvectorWithSimilarity(businessId, queryVectorString, retrievalLimit);
            
            List<RagResult> results = new ArrayList<>();
            for (Object[] row : pgvectorResults) {
                KnowledgeChunk chunk = (KnowledgeChunk) row[0];
                Double similarity = (Double) row[1];
                
                RagResult result = toResult(chunk, similarity);
                results.add(result);
            }
            
            // Apply priority boosting for broad business queries
            if (isBroadBusinessQuery) {
                results = applyPriorityBoosting(results);
            }
            
            log.debug("Pgvector returned {} results", results.size());
            return rankAndTrim(results, limit);
            
        } catch (Exception e) {
            log.warn("Pgvector query failed, falling back to legacy search: {}", e.getMessage());
            return semanticSearchLegacy(query, chunks, limit, isBroadBusinessQuery);
        }
    }

    /**
     * Legacy semantic search using TEXT embedding column (for rollback compatibility).
     */
    private List<RagResult> semanticSearchLegacy(String query, List<KnowledgeChunk> chunks, int limit, boolean isBroadBusinessQuery)
            throws EmbeddingService.EmbeddingException {
        
        float[] queryEmbedding = embeddingService.generateEmbedding(query);
        
        // Increase retrieval breadth for broad business queries
        int retrievalLimit = isBroadBusinessQuery ? Math.min(limit * 3, MAX_TOP_K) : limit;
        
        log.debug("Using in-memory similarity search (legacy TEXT embedding)");
        List<RagResult> scored = new ArrayList<>();
        for (KnowledgeChunk chunk : chunks) {
            String stored = chunk.getEmbedding();
            if (stored == null || stored.isBlank()) {
                continue; // skip chunks without an embedding
            }
            float[] chunkEmbedding = vectorStoreService.vectorStringToEmbedding(stored);
            if (chunkEmbedding.length != queryEmbedding.length) {
                log.warn("Skipping chunk {}: embedding dimension {} != query {}",
                        chunk.getId(), chunkEmbedding.length, queryEmbedding.length);
                continue;
            }
            double similarity = vectorStoreService.cosineSimilarity(queryEmbedding, chunkEmbedding);
            scored.add(toResult(chunk, similarity));
        }

        // Apply priority boosting for broad business queries
        if (isBroadBusinessQuery) {
            scored = applyPriorityBoosting(scored);
        }

        scored.sort((a, b) -> Double.compare(
                b.getSimilarity() == null ? 0.0 : b.getSimilarity(),
                a.getSimilarity() == null ? 0.0 : a.getSimilarity()));

        return rankAndTrim(scored, limit);
    }

    /**
     * Boost priority chunks (homepage, about, services) for broad business queries.
     */
    private List<RagResult> applyPriorityBoosting(List<RagResult> results) {
        List<RagResult> priorityChunks = new ArrayList<>();
        List<RagResult> normalChunks = new ArrayList<>();
        
        for (RagResult result : results) {
            if (isPriorityChunk(result)) {
                // Boost similarity score by 15%
                double boostedSim = result.getSimilarity() != null ? result.getSimilarity() * 1.15 : 0.0;
                RagResult boosted = new RagResult(
                    result.getChunkId(),
                    result.getContent(),
                    result.getSourceUrl(),
                    result.getDocumentTitle(),
                    boostedSim
                );
                priorityChunks.add(boosted);
            } else {
                normalChunks.add(result);
            }
        }
        
        // Re-sort all chunks by boosted similarity
        List<RagResult> combined = new ArrayList<>();
        combined.addAll(priorityChunks);
        combined.addAll(normalChunks);
        combined.sort((a, b) -> Double.compare(
                b.getSimilarity() == null ? 0.0 : b.getSimilarity(),
                a.getSimilarity() == null ? 0.0 : a.getSimilarity()));
        
        return combined;
    }

    /**
     * Keyword fallback when embeddings are unavailable.
     */
    private List<RagResult> keywordFallback(String query, List<KnowledgeChunk> chunks, int limit, boolean isBroadBusinessQuery) {
        String[] terms = query.toLowerCase().split("\\s+");

        List<RagResult> scored = new ArrayList<>();
        for (KnowledgeChunk chunk : chunks) {
            if (chunk.getContent() == null || chunk.getContent().isBlank()) {
                continue;
            }
            String content = chunk.getContent().toLowerCase();
            int matches = 0;
            for (String term : terms) {
                if (!term.isBlank() && content.contains(term)) {
                    matches++;
                }
            }
            if (matches > 0) {
                // Normalize to a 0..1 pseudo-similarity for a consistent API shape.
                double pseudo = (double) matches / terms.length;
                scored.add(toResult(chunk, pseudo));
            }
        }

        // Apply priority boosting for broad business queries
        if (isBroadBusinessQuery) {
            scored = applyPriorityBoosting(scored);
        }

        scored.sort((a, b) -> Double.compare(
                b.getSimilarity() == null ? 0.0 : b.getSimilarity(),
                a.getSimilarity() == null ? 0.0 : a.getSimilarity()));

        return rankAndTrim(scored, limit);
    }

    private List<RagResult> rankAndTrim(List<RagResult> scored, int limit) {
        List<RagResult> results = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, scored.size()); i++) {
            RagResult r = scored.get(i);
            r.setRank(i + 1);
            results.add(r);
        }
        return results;
    }

    private RagResult toResult(KnowledgeChunk chunk, double similarity) {
        String documentTitle = null;
        Document document = chunk.getDocument();
        if (document != null) {
            documentTitle = document.getTitle();
        }
        return new RagResult(
                chunk.getId().toString(),
                chunk.getContent(),
                chunk.getSourceUrl(),
                documentTitle,
                similarity);
    }

    private int resolveTopK(Integer topK) {
        if (topK == null || topK <= 0) {
            return DEFAULT_TOP_K;
        }
        return Math.min(topK, MAX_TOP_K);
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "'").replace("\n", " ");
    }

    private void logAgentAction(Business business, String action, String inputJson,
                                 String outputJson, AgentActionStatus status) {
        try {
            AgentLog logEntry = new AgentLog();
            logEntry.setAgentName(AGENT_NAME);
            logEntry.setAction(action);
            logEntry.setInputJson(inputJson);
            logEntry.setOutputJson(outputJson);
            logEntry.setStatus(status);
            logEntry.setBusiness(business);
            agentLogRepository.save(logEntry);
        } catch (Exception e) {
            log.error("Failed to log agent action {}", action, e);
        }
    }

    /**
     * Result of a RAG search.
     */
    public static class SearchResult {
        private final String query;
        private final int totalResults;
        private final List<RagResult> results;
        private final String retrievalMode;

        public SearchResult(String query, int totalResults, List<RagResult> results, String retrievalMode) {
            this.query = query;
            this.totalResults = totalResults;
            this.results = results;
            this.retrievalMode = retrievalMode;
        }

        public String getQuery() { return query; }
        public int getTotalResults() { return totalResults; }
        public List<RagResult> getResults() { return results; }
        public String getRetrievalMode() { return retrievalMode; }
    }

    /**
     * Individual RAG result (one matching chunk).
     */
    public static class RagResult {
        private final String chunkId;
        private final String content;
        private final String sourceUrl;
        private final String documentTitle;
        private final Double similarity;
        private int rank;

        public RagResult(String chunkId, String content, String sourceUrl,
                         String documentTitle, Double similarity) {
            this.chunkId = chunkId;
            this.content = content;
            this.sourceUrl = sourceUrl;
            this.documentTitle = documentTitle;
            this.similarity = similarity;
            this.rank = 0;
        }

        public String getChunkId() { return chunkId; }
        public String getContent() { return content; }
        public String getSourceUrl() { return sourceUrl; }
        public String getDocumentTitle() { return documentTitle; }
        public Double getSimilarity() { return similarity; }
        public int getRank() { return rank; }

        public void setRank(int rank) { this.rank = rank; }
    }

    /**
     * Result of a RAG answer request (grounded LLM answer + retrieved chunks + diagnostics).
     */
    public static class AnswerResult {
        private final String businessId;
        private final String query;
        private final String answer;
        private final List<String> sources;
        private final List<RagResult> results;
        private final int topK;
        private final String status;
        private final RagDiagnostics diagnostics;

        public AnswerResult(String businessId, String query, String answer, List<String> sources,
                            List<RagResult> results, int topK, String status, RagDiagnostics diagnostics) {
            this.businessId = businessId;
            this.query = query;
            this.answer = answer;
            this.sources = sources;
            this.results = results;
            this.topK = topK;
            this.status = status;
            this.diagnostics = diagnostics;
        }

        public String getBusinessId() { return businessId; }
        public String getQuery() { return query; }
        public String getAnswer() { return answer; }
        public List<String> getSources() { return sources; }
        public List<RagResult> getResults() { return results; }
        public int getTopK() { return topK; }
        public String getStatus() { return status; }
        public RagDiagnostics getDiagnostics() { return diagnostics; }
    }

    /**
     * RAG diagnostics for troubleshooting.
     */
    public static class RagDiagnostics {
        private final long documentsCount;
        private final long totalChunks;
        private final long legacyEmbeddedChunks;
        private final long pgvectorEmbeddedChunks;
        private final String retrievalMode;
        private final List<ChunkDiagnostic> includedChunks;
        private final List<String> rejectionReasons;

        public RagDiagnostics(long documentsCount, long totalChunks, long legacyEmbeddedChunks,
                              long pgvectorEmbeddedChunks, String retrievalMode,
                              List<ChunkDiagnostic> includedChunks, List<String> rejectionReasons) {
            this.documentsCount = documentsCount;
            this.totalChunks = totalChunks;
            this.legacyEmbeddedChunks = legacyEmbeddedChunks;
            this.pgvectorEmbeddedChunks = pgvectorEmbeddedChunks;
            this.retrievalMode = retrievalMode;
            this.includedChunks = includedChunks;
            this.rejectionReasons = rejectionReasons;
        }

        public long getDocumentsCount() { return documentsCount; }
        public long getTotalChunks() { return totalChunks; }
        public long getLegacyEmbeddedChunks() { return legacyEmbeddedChunks; }
        public long getPgvectorEmbeddedChunks() { return pgvectorEmbeddedChunks; }
        public String getRetrievalMode() { return retrievalMode; }
        public List<ChunkDiagnostic> getIncludedChunks() { return includedChunks; }
        public List<String> getRejectionReasons() { return rejectionReasons; }
    }

    /**
     * Diagnostic information for a single chunk.
     */
    public static class ChunkDiagnostic {
        private final String chunkId;
        private final String sourceUrl;
        private final String documentTitle;
        private final Double similarity;
        private final String status;

        public ChunkDiagnostic(String chunkId, String sourceUrl, String documentTitle,
                               Double similarity, String status) {
            this.chunkId = chunkId;
            this.sourceUrl = sourceUrl;
            this.documentTitle = documentTitle;
            this.similarity = similarity;
            this.status = status;
        }

        public String getChunkId() { return chunkId; }
        public String getSourceUrl() { return sourceUrl; }
        public String getDocumentTitle() { return documentTitle; }
        public Double getSimilarity() { return similarity; }
        public String getStatus() { return status; }
    }

    /**
     * Custom exception for RAG search errors.
     */
    public static class RagSearchException extends Exception {
        public RagSearchException(String message) {
            super(message);
        }

        public RagSearchException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
