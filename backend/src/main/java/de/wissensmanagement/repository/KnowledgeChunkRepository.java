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
           "JOIN wm_articles a ON a.id = c.article_id " +
           "WHERE c.tenant_id = :tenantId " +
           "AND a.status = 'PUBLISHED' " +
           "AND to_tsvector('german', c.content) @@ plainto_tsquery('german', :query) " +
           "ORDER BY ts_rank(to_tsvector('german', c.content), plainto_tsquery('german', :query)) DESC " +
           "LIMIT :limit", nativeQuery = true)
    List<KnowledgeChunk> fullTextSearchChunks(String tenantId, String query, int limit);

    /**
     * Trigram similarity search on chunks - finds content even when query uses
     * different wording (synonyms, paraphrases).
     */
    @Query(value = "SELECT c.* FROM wm_chunks c " +
           "JOIN wm_articles a ON a.id = c.article_id " +
           "WHERE c.tenant_id = :tenantId " +
           "AND a.status = 'PUBLISHED' " +
           "AND (similarity(c.content, :query) > 0.05 " +
           "     OR c.content ILIKE CONCAT('%', :query, '%') " +
           "     OR c.article_title ILIKE CONCAT('%', :query, '%')) " +
           "ORDER BY similarity(c.content, :query) DESC " +
           "LIMIT :limit", nativeQuery = true)
    List<KnowledgeChunk> similaritySearchChunks(String tenantId, String query, int limit);

    /**
     * Combined search using both fulltext and trigram similarity.
     * Uses UNION to merge results from both strategies.
     */
    @Query(value = """
           SELECT * FROM (
             SELECT c.*, ts_rank(to_tsvector('german', c.content), plainto_tsquery('german', :query)) AS rank_score
             FROM wm_chunks c
             JOIN wm_articles a ON a.id = c.article_id
             WHERE c.tenant_id = :tenantId
             AND a.status = 'PUBLISHED'
             AND to_tsvector('german', c.content) @@ plainto_tsquery('german', :query)
             UNION ALL
             SELECT c.*, similarity(c.content, :query) AS rank_score
             FROM wm_chunks c
             JOIN wm_articles a ON a.id = c.article_id
             WHERE c.tenant_id = :tenantId
             AND a.status = 'PUBLISHED'
             AND NOT (to_tsvector('german', c.content) @@ plainto_tsquery('german', :query))
             AND (similarity(c.content, :query) > 0.05
                  OR c.content ILIKE CONCAT('%', :query, '%')
                  OR c.article_title ILIKE CONCAT('%', :query, '%'))
           ) combined
           ORDER BY rank_score DESC
           LIMIT :limit
           """, nativeQuery = true)
    List<KnowledgeChunk> hybridSearchChunks(String tenantId, String query, int limit);
}
