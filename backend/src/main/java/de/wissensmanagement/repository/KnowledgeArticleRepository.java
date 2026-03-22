package de.wissensmanagement.repository;

import de.wissensmanagement.entity.KnowledgeArticle;
import de.wissensmanagement.enums.ArticleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, String> {

    Optional<KnowledgeArticle> findByIdAndTenantId(String id, String tenantId);

    Page<KnowledgeArticle> findByTenantIdAndStatus(String tenantId, ArticleStatus status, Pageable pageable);

    Page<KnowledgeArticle> findByTenantId(String tenantId, Pageable pageable);

    @Query("SELECT a FROM KnowledgeArticle a WHERE a.tenantId = :tenantId AND a.status = :status " +
           "AND (LOWER(a.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(a.content) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<KnowledgeArticle> searchByTenant(String tenantId, ArticleStatus status, String q, Pageable pageable);

    @Query("SELECT a FROM KnowledgeArticle a WHERE a.tenantId = :tenantId AND a.status = 'PUBLISHED' " +
           "AND a.category.id = :categoryId ORDER BY a.createdAt DESC")
    List<KnowledgeArticle> findByTenantAndCategory(String tenantId, String categoryId);

    @Query("SELECT a FROM KnowledgeArticle a WHERE a.tenantId = :tenantId AND a.status = 'PUBLISHED' " +
           "ORDER BY a.viewCount DESC")
    List<KnowledgeArticle> findMostViewed(String tenantId, Pageable pageable);

    @Query("SELECT a FROM KnowledgeArticle a WHERE a.tenantId = :tenantId AND a.status = 'PUBLISHED' " +
           "ORDER BY a.createdAt DESC")
    List<KnowledgeArticle> findNewest(String tenantId, Pageable pageable);

    @Query("SELECT a FROM KnowledgeArticle a WHERE a.tenantId = :tenantId AND a.status = 'PUBLISHED' " +
           "ORDER BY a.lastUsedAt DESC NULLS LAST")
    List<KnowledgeArticle> findRecentlyUsed(String tenantId, Pageable pageable);

    long countByTenantIdAndStatus(String tenantId, ArticleStatus status);

    long countByTenantId(String tenantId);

    @Query("SELECT a FROM KnowledgeArticle a WHERE a.tenantId = :tenantId AND a.linkedTaskId = :taskId")
    List<KnowledgeArticle> findByTenantAndTask(String tenantId, String taskId);

    @Modifying
    @Query("UPDATE KnowledgeArticle a SET a.viewCount = a.viewCount + 1 WHERE a.id = :id")
    void incrementViewCount(String id);

    @Query(value = "SELECT a.* FROM wm_articles a " +
           "WHERE a.tenant_id = :tenantId AND a.status = 'PUBLISHED' " +
           "AND to_tsvector('german', coalesce(a.title,'') || ' ' || coalesce(a.content,'') || ' ' || coalesce(a.summary,'')) " +
           "@@ plainto_tsquery('german', :query) " +
           "ORDER BY ts_rank(to_tsvector('german', coalesce(a.title,'') || ' ' || coalesce(a.content,'') || ' ' || coalesce(a.summary,'')), " +
           "plainto_tsquery('german', :query)) DESC " +
           "LIMIT :limit", nativeQuery = true)
    List<KnowledgeArticle> fullTextSearch(String tenantId, String query, int limit);

    /**
     * Trigram similarity search - finds articles even with paraphrased queries.
     */
    @Query(value = "SELECT a.* FROM wm_articles a " +
           "WHERE a.tenant_id = :tenantId AND a.status = 'PUBLISHED' " +
           "AND (similarity(a.title, :query) > 0.1 " +
           "     OR similarity(coalesce(a.summary, ''), :query) > 0.05 " +
           "     OR similarity(a.content, :query) > 0.03 " +
           "     OR a.title ILIKE CONCAT('%', :query, '%') " +
           "     OR a.content ILIKE CONCAT('%', :query, '%') " +
           "     OR coalesce(a.summary, '') ILIKE CONCAT('%', :query, '%')) " +
           "ORDER BY greatest(similarity(a.title, :query) * 3, " +
           "                  similarity(coalesce(a.summary, ''), :query) * 2, " +
           "                  similarity(a.content, :query)) DESC " +
           "LIMIT :limit", nativeQuery = true)
    List<KnowledgeArticle> similaritySearch(String tenantId, String query, int limit);

    // --- Hierarchy queries ---

    List<KnowledgeArticle> findByTenantIdAndParentArticleIdIsNullOrderBySortOrderAsc(String tenantId);

    List<KnowledgeArticle> findByTenantIdAndParentArticleIdOrderBySortOrderAsc(String tenantId, String parentArticleId);

    @Query("SELECT COUNT(a) FROM KnowledgeArticle a WHERE a.tenantId = :tenantId AND a.parentArticleId = :parentId")
    int countChildren(String tenantId, String parentId);

    @Query("SELECT a FROM KnowledgeArticle a WHERE a.tenantId = :tenantId AND a.treePath LIKE CONCAT(:parentPath, '%') ORDER BY a.depth, a.sortOrder")
    List<KnowledgeArticle> findSubtree(String tenantId, String parentPath);

    @Query("SELECT MAX(a.sortOrder) FROM KnowledgeArticle a WHERE a.tenantId = :tenantId AND a.parentArticleId = :parentId")
    Integer findMaxSortOrder(String tenantId, String parentId);

    @Query("SELECT MAX(a.sortOrder) FROM KnowledgeArticle a WHERE a.tenantId = :tenantId AND a.parentArticleId IS NULL")
    Integer findMaxRootSortOrder(String tenantId);

    @Query("SELECT a FROM KnowledgeArticle a WHERE a.tenantId = :tenantId AND a.parentArticleId = :parentId AND a.sortOrder > :sortOrder ORDER BY a.sortOrder ASC")
    List<KnowledgeArticle> findSiblingsAfter(String tenantId, String parentId, int sortOrder);
}
