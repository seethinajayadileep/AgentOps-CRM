# PGVector Migration Implementation Summary

## Date: 2026-07-05
## Task: Fix Settings Warnings - Implement PGVector, Enable Flyway, Set Production Config

## Changes Completed:

### 1. Configuration Updates

####  [`application-prod.yml`](backend/src/main/resources/application-prod.yml)
- ✅ Added `ddl-auto: validate` for production schema validation
- ✅ Added `baseline-on-migrate: true` for Flyway initialization on existing database

#### [`docker-compose.yml`](docker/docker-compose.yml)  
- ✅ Updated PostgreSQL image from `postgres:16-alpine` to `pgvector/pgvector:0.7.4-pg16`
- Preserves existing data volume

### 2. Database Migration

#### [`V10__add_pgvector_support.sql`](backend/src/main/resources/migration/V10__add_pgvector_support.sql)
- ✅ Creates `vector` extension if not exists
- ✅ Adds `embedding_vector vector(1536)` column to `knowledge_chunks`
- ✅ Creates IVFFlat index for efficient similarity search
- Preserves old `embedding` TEXT column for rollback compatibility

### 3. Entity Updates

#### [`KnowledgeChunk.java`](backend/src/main/java/com/agentopscrm/entity/KnowledgeChunk.java)
- ✅ Added `embeddingVector` field with `vector(1536)` column definition
- ✅ Added getter/setter methods
-  Maintains legacy `embedding` field for backward compatibility

### 4. Repository Updates (PENDING)

#### [`KnowledgeChunkRepository.java`](backend/src/main/java/com/agentopscrm/repository/KnowledgeChunkRepository.java)
- TODO: Add native query for pgvector similarity search:
```java
@Query(value = "SELECT kc.*, " +
       "1 - (kc.embedding_vector <=> CAST(:queryVector AS vector)) AS similarity " +
       "FROM knowledge_chunks kc " +
       "WHERE kc.business_id = :businessId " +
       "AND kc.embedding_vector IS NOT NULL " +
       "ORDER BY kc.embedding_vector <=> CAST(:queryVector AS vector) " +
       "LIMIT :topK", nativeQuery = true)
List<KnowledgeChunk> findSimilarByBusinessId(
    @Param("businessId") UUID businessId,
    @Param("queryVector") String queryVector,
    @Param("topK") int topK
);

@Query("SELECT COUNT(kc) FROM KnowledgeChunk kc WHERE kc.business.id = :businessId AND kc.embeddingVector IS NOT NULL")
long countByBusinessIdWithVector(@Param("businessId") UUID businessId);
```

### 5. Service Updates (PENDING)

#### [`KnowledgeBaseService.java`](backend/src/main/java/com/agentopscrm/service/KnowledgeBaseService.java)
- TODO: Update embedding persistence to write to both fields:
  - `chunk.setEmbedding(embeddingStr)` (legacy)
  - `chunk.setEmbeddingVector(embeddingStr)` (new)

#### [`VectorStoreService.java`](backend/src/main/java/com/agentopscrm/service/VectorStoreService.java)
- TODO: Add pgvector detection method
- TODO: Update search to use pgvector when available
- TODO: Fall back to in-memory search for legacy data

#### [`SettingsService.java`](backend/src/main/java/com/agentopscrm/service/SettingsService.java)
- TODO: Add pgvector availability check:
```java
private boolean isPgVectorAvailable() {
    try {
        String query = "SELECT COUNT(*) FROM pg_extension WHERE extname = 'vector'";
        Integer count = jdbcTemplate.queryForObject(query, Integer.class);
        return count != null && count > 0;
    } catch (Exception e) {
        return false;
    }
}
```
- TODO: Update warning logic in `getSystemDiagnostics()`:
  - No MIGRATION warning when `flywayEnabled == true`
  - No SCHEMA warning when `hibernateDdlAuto.equals("validate")`  
  - No VECTOR_STORE warning when vector store is "pgvector" AND pgvector is available
  - Add ERROR when pgvector configured but extension missing

### 6. Configuration (PENDING)

#### [`application.yml`](backend/src/main/resources/application.yml)
- TODO: Update `rag.vector-store` from `postgres-text` to `pgvector`

### 7. RAG Controller Enhancement (PENDING)

#### [`RagController.java`](backend/src/main/java/com/agentopscrm/controller/RagController.java)
- TODO: Add endpoint for rebuilding embeddings:
```java
@PostMapping("/rebuild-embeddings")
public ResponseEntity<ApiResponse> rebuildEmbeddings(
    @RequestParam UUID businessId,
    @RequestParam(defaultValue = "false") boolean dryRun
) {
    // Migration logic for old chunks without embedding_vector
}
```

## Deployment Steps:

1. **Before Migration:**
   ```bash
   # Backup database
   docker exec agentops-postgres pg_dump -U postgres agentops_crm > backup_$(date +%Y%m%d).sql
   ```

2. **Update  Docker:**
   ```bash
   cd docker
   docker-compose down
   docker-compose pull  # Get pgvector image
   docker-compose up -d
   ```

3. **Verify PGVector Extension:**
   ```sql
   docker exec -it agentops-postgres psql -U postgres -d agentops_crm -c "SELECT * FROM pg_extension WHERE extname = 'vector';"
   ```

4. **Run Application** (Flyway will auto-migrate):
   ```bash
   cd backend
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

5. **Verify Migration:**
   ```sql
   docker exec -it agentops-postgres psql -U postgres -d agentops_crm -c "\d knowledge_chunks"
   # Should show embedding_vector column
   ```

6. **Rebuild Existing Embeddings** (if needed):
   ```bash
   curl -X POST "http://localhost:8080/api/rag/rebuild-embeddings?businessId=<UUID>&dryRun=true"
   ```

## Testing:

```bash
# Run tests
cd backend
mvn test

# Build
mvn clean package

# Verify Settings Page
open http://localhost:5173/settings?tab=system
open http://localhost:5173/settings?tab=rag
```

## Expected Outcomes:

1. ✅ Settings > System tab shows NO warnings
   - Flyway: Enabled
   - Schema Mode: validate
   - Vector Store: pgvector

2. ✅ Settings > RAG tab shows:
   - Vector Store Strategy: pgvector
   - No warning about postgres-text

3. ✅ Existing embeddings continue to work (via legacy column)
4. ✅ New embeddings use native pgvector
5. ✅ Database preserves all existing data
6. ✅ Rollback possible by reverting config to `postgres-text`

## Rollback Plan:

If issues occur:
1. Change `rag.vector-store` back to `postgres-text` in application.yml
2. Restart application
3. System will use TEXT column (unchanged)
4. No data loss - both columns exist

## Notes:

- Migration is NON-DESTRUCTIVE
- Old `embedding` column preserved for compatibility
- Can be removed in future migration: `V11__drop_legacy_embedding_column.sql`
- Dual-write approach ensures rollback safety
