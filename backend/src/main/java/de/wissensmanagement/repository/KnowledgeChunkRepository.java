package de.wissensmanagement.repository;

import de.wissensmanagement.entity.KnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, String> {

    List<KnowledgeChunk> findByArticleIdOrderByChunkIndexAsc(String articleId);

    void deleteByArticleId(String articleId);

    @Query(value = "SELECT c.* FROM wm_chunks c " +
           "WHERE c.tenant_id = :tenantId " +
           "AND to_tsvector('german', c.content) @@ plainto_tsquery('german', :query) " +
           "ORDER BY ts_rank(to_tsvector('german', c.content), plainto_tsquery('german', :query)) DESC " +
           "LIMIT :limit", nativeQuery = true)
    List<KnowledgeChunk> fullTextSearchChunks(String tenantId, String query, int limit);
}
