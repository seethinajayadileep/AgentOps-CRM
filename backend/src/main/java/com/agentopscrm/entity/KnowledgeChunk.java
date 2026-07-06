package com.agentopscrm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * KnowledgeChunk entity representing a processed chunk of content.
 *
 * Why exists: Stores content chunks with vector IDs for semantic search (RAG).
 * Each chunk is a piece of content that can be retrieved for AI responses.
 *
 * @author AgentOps Team
 * @version 0.2.0
 */
@Entity
@Table(name = "knowledge_chunks", indexes = {
    @Index(name = "idx_knowledge_chunks_business_id", columnList = "business_id"),
    @Index(name = "idx_knowledge_chunks_document_id", columnList = "document_id")
})
public class KnowledgeChunk extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "vector_id", length = 255)
    private String vectorId;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    /**
     * Chunk position within its source document (0-based).
     * Used for ordering and traceability of chunks back to the original content.
     */
    @Column(name = "chunk_index")
    private Integer chunkIndex;

    /**
     * Serialized embedding vector for semantic search (legacy TEXT format).
     *
     * Stored as a pgvector-style string, e.g. "[0.12,0.34,...]".
     * NOTE: This TEXT-column is kept for rollback compatibility.
     * New embeddings are stored in both embedding and embeddingVector fields.
     * Will be removed in a future migration once pgvector is fully validated.
     */
    @Column(name = "embedding", columnDefinition = "TEXT")
    private String embedding;

    /**
     * Native pgvector embedding for efficient similarity search.
     *
     * Stored as a native vector(1536) column for OpenAI text-embedding-3-small.
     * Enables efficient vector similarity search using pgvector extension.
     * This is the primary embedding storage for production use.
     * 
     * NOTE: Hibernate cannot directly persist PostgreSQL vector type,
     * so we mark this as insertable=false, updatable=false and use
     * native SQL queries for vector persistence.
     */
    @Column(name = "embedding_vector", columnDefinition = "vector(1536)", insertable = false, updatable = false)
    private String embeddingVector;

    public KnowledgeChunk() {
        super();
    }

    public KnowledgeChunk(UUID id) {
        super();
        this.id = id;
    }

    public Business getBusiness() {
        return business;
    }

    public void setBusiness(Business business) {
        this.business = business;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getVectorId() {
        return vectorId;
    }

    public void setVectorId(String vectorId) {
        this.vectorId = vectorId;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public String getEmbedding() {
        return embedding;
    }

    public void setEmbedding(String embedding) {
        this.embedding = embedding;
    }

    public String getEmbeddingVector() {
        return embeddingVector;
    }

    public void setEmbeddingVector(String embeddingVector) {
        this.embeddingVector = embeddingVector;
    }
}