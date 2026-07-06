package com.agentopscrm.service;

import com.agentopscrm.repository.KnowledgeChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for vector operations and similarity search.
 *
 * Note: Currently using text-based similarity as fallback until pgvector is available.
 * For pgvector, the vector column would be added and native SQL queries with <=> would be used.
 *
 * @author AgentOps Team
 * @version 0.2.0
 */
@Service
public class VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreService.class);

    @Autowired
    private KnowledgeChunkRepository knowledgeChunkRepository;

    /**
     * Convert embedding array to vector string format for pgvector.
     * Format: '[0.1, 0.2, 0.3, ...]'
     */
    public String embeddingToVectorString(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Convert vector string to float array.
     */
    public float[] vectorStringToEmbedding(String vectorString) {
        if (vectorString == null || vectorString.isEmpty()) {
            return new float[0];
        }

        // Remove brackets and split
        String content = vectorString.replace("[", "").replace("]", "").trim();
        if (content.isEmpty()) {
            return new float[0];
        }

        String[] parts = content.split(",");
        float[] embedding = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                embedding[i] = Float.parseFloat(parts[i].trim());
            } catch (NumberFormatException e) {
                log.warn("Failed to parse embedding value at index {}: {}", i, parts[i]);
                embedding[i] = 0.0f;
            }
        }

        return embedding;
    }

    /**
     * Calculate cosine similarity between two embeddings.
     */
    public float cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Embeddings must have same length");
        }

        float dotProduct = 0.0f;
        float normA = 0.0f;
        float normB = 0.0f;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0.0f || normB == 0.0f) {
            return 0.0f;
        }

        return dotProduct / ((float) Math.sqrt(normA) * (float) Math.sqrt(normB));
    }

    /**
     * Calculate Euclidean distance between two embeddings.
     */
    public float euclideanDistance(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Embeddings must have same length");
        }

        float sum = 0.0f;
        for (int i = 0; i < a.length; i++) {
            float diff = a[i] - b[i];
            sum += diff * diff;
        }

        return (float) Math.sqrt(sum);
    }

    /**
     * Find top k similar chunks using cosine similarity.
     * In-memory fallback when pgvector is not available.
     *
     * @param queryEmbedding The query embedding
     * @param chunks List of chunks with embeddings
     * @param topK Number of top results
     * @return List of chunk indices sorted by similarity
     */
    public List<Integer> findTopKSimilar(float[] queryEmbedding, List<float[]> chunkEmbeddings, int topK) {
        List<ChunkSimilarity> similarities = new ArrayList<>();

        for (int i = 0; i < chunkEmbeddings.size(); i++) {
            float[] chunkEmbedding = chunkEmbeddings.get(i);
            if (chunkEmbedding == null || chunkEmbedding.length == 0) {
                continue;
            }

            try {
                float similarity = cosineSimilarity(queryEmbedding, chunkEmbedding);
                similarities.add(new ChunkSimilarity(i, similarity));
            } catch (IllegalArgumentException e) {
                log.warn("Chunk {} has incompatible embedding dimension", i);
            }
        }

        // Sort by similarity descending and take top K
        similarities.sort((a, b) -> Float.compare(b.similarity, a.similarity));

        List<Integer> topKIndices = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, similarities.size()); i++) {
            topKIndices.add(similarities.get(i).chunkIndex);
        }

        return topKIndices;
    }

    /**
     * Helper class for tracking chunk similarity.
     */
    private static class ChunkSimilarity {
        final int chunkIndex;
        final float similarity;

        ChunkSimilarity(int chunkIndex, float similarity) {
            this.chunkIndex = chunkIndex;
            this.similarity = similarity;
        }
    }

    /**
     * Check if pgvector extension is available in the database.
     * 
     * @return true if pgvector extension is installed, false otherwise
     */
    public boolean isPgvectorAvailable() {
        try {
            int result = knowledgeChunkRepository.isPgvectorAvailable();
            log.debug("Pgvector availability check result: {}", result);
            return result == 1;
        } catch (Exception e) {
            log.warn("Failed to check pgvector availability: {}", e.getMessage());
            return false;
        }
    }
}