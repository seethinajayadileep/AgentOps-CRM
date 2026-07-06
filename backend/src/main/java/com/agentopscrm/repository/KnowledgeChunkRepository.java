package com.agentopscrm.repository;

import com.agentopscrm.entity.KnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for KnowledgeChunk entity.
 *
 * @author AgentOps Team
 * @version 0.2.0
 */
@Repository
public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, UUID> {

    List<KnowledgeChunk> findByBusinessId(UUID businessId);

    List<KnowledgeChunk> findByDocumentId(UUID documentId);

    List<KnowledgeChunk> findByVectorId(String vectorId);

    @Query("SELECT kc FROM KnowledgeChunk kc WHERE kc.business.id = :businessId ORDER BY kc.createdAt DESC")
    List<KnowledgeChunk> findLatestChunksByBusiness(@Param("businessId") UUID businessId);

    long countByBusinessId(UUID businessId);

    /**
     * Find top K similar chunks using pgvector cosine similarity.
     * Uses the <=> operator for vector distance (1 - cosine_similarity).
     * 
     * @param businessId Business ID to filter chunks
     * @param queryVector Query embedding vector in pgvector format "[0.1,0.2,...]"
     * @param topK Number of top results to return
     * @return List of knowledge chunks ordered by similarity (most similar first)
     */
    @Query(value = "SELECT * FROM knowledge_chunks " +
                   "WHERE business_id = :businessId " +
                   "AND embedding_vector IS NOT NULL " +
                   "ORDER BY embedding_vector <=> CAST(:queryVector AS vector) " +
                   "LIMIT :topK", 
           nativeQuery = true)
    List<KnowledgeChunk> findTopKSimilarByPgvector(
        @Param("businessId") UUID businessId,
        @Param("queryVector") String queryVector,
        @Param("topK") int topK
    );

    /**
     * Find top K similar chunks using pgvector with explicit similarity scores.
     * Returns both the chunk and the cosine similarity (1 - distance).
     * 
     * @param businessId Business ID to filter chunks
     * @param queryVector Query embedding vector in pgvector format "[0.1,0.2,...]"
     * @param topK Number of top results to return
     * @return List of Object arrays where [0] is KnowledgeChunk and [1] is Double similarity
     */
    @Query(value = "SELECT kc.*, (1 - (kc.embedding_vector <=> CAST(:queryVector AS vector))) AS similarity " +
                   "FROM knowledge_chunks kc " +
                   "WHERE kc.business_id = :businessId " +
                   "AND kc.embedding_vector IS NOT NULL " +
                   "ORDER BY kc.embedding_vector <=> CAST(:queryVector AS vector) " +
                   "LIMIT :topK", 
           nativeQuery = true)
    List<Object[]> findTopKSimilarByPgvectorWithSimilarity(
        @Param("businessId") UUID businessId,
        @Param("queryVector") String queryVector,
        @Param("topK") int topK
    );

    /**
     * Check if pgvector extension is available by attempting to use it.
     * If this query executes without error, pgvector is available.
     * 
     * @return 1 if pgvector is available, 0 otherwise
     */
    @Query(value = "SELECT CASE WHEN EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector') THEN 1 ELSE 0 END", 
           nativeQuery = true)
    int isPgvectorAvailable();

    /**
     * Count chunks with non-null embedding_vector for a business.
     * Used to check if pgvector migration has been applied to existing data.
     * 
     * @param businessId Business ID to count chunks for
     * @return Number of chunks with pgvector embeddings
     */
    @Query(value = "SELECT COUNT(*) FROM knowledge_chunks WHERE business_id = :businessId AND embedding_vector IS NOT NULL", 
           nativeQuery = true)
    long countByBusinessIdWithPgvectorEmbedding(@Param("businessId") UUID businessId);

    /**
     * Update the embedding_vector column using native SQL with proper type casting.
     * Hibernate cannot directly persist PostgreSQL vector type, so we use native SQL.
     * 
     * @param chunkId Knowledge chunk ID to update
     * @param vectorString Vector in pgvector format "[0.1,0.2,...]"
     * @return Number of rows updated (should be 1)
     */
    @Modifying
    @Query(value = "UPDATE knowledge_chunks SET embedding_vector = CAST(:vectorString AS vector) WHERE id = :chunkId", 
           nativeQuery = true)
    int updateEmbeddingVector(@Param("chunkId") UUID chunkId, @Param("vectorString") String vectorString);
}