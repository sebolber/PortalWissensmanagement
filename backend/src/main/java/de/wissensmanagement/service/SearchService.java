package de.wissensmanagement.service;

import de.wissensmanagement.entity.KnowledgeArticle;
import de.wissensmanagement.repository.KnowledgeArticleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Unified search service combining fulltext and similarity-based search.
 * Supports three modes:
 * - FULLTEXT: PostgreSQL tsvector-based search (exact word matching with stemming)
 * - SEMANTIC: Trigram similarity search (fuzzy matching for paraphrased queries)
 * - HYBRID: Combines both, boosting articles found by multiple strategies
 */
@Service
@Transactional(readOnly = true)
public class SearchService {

    private final KnowledgeArticleRepository articleRepo;
    private final HierarchyService hierarchyService;

    public SearchService(KnowledgeArticleRepository articleRepo, HierarchyService hierarchyService) {
        this.articleRepo = articleRepo;
        this.hierarchyService = hierarchyService;
    }

    public enum SearchMode {
        FULLTEXT, SEMANTIC, HYBRID
    }

    public List<SearchResult> search(String tenantId, String query, SearchMode mode, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        Map<String, SearchResult> results = new LinkedHashMap<>();

        // Fulltext search (always included in HYBRID and FULLTEXT modes)
        if (mode == SearchMode.FULLTEXT || mode == SearchMode.HYBRID) {
            List<KnowledgeArticle> fulltextResults = articleRepo.fullTextSearch(tenantId, query, limit);
            for (int i = 0; i < fulltextResults.size(); i++) {
                KnowledgeArticle article = fulltextResults.get(i);
                double score = 1.0 - (i * 0.05);
                results.put(article.getId(), toSearchResult(article, score, "FULLTEXT"));
            }
        }

        // Semantic/similarity search (HYBRID and SEMANTIC modes)
        if (mode == SearchMode.SEMANTIC || mode == SearchMode.HYBRID) {
            List<KnowledgeArticle> similarResults = articleRepo.similaritySearch(tenantId, query, limit);

            for (int i = 0; i < similarResults.size(); i++) {
                KnowledgeArticle article = similarResults.get(i);
                if (!results.containsKey(article.getId())) {
                    double score = 0.8 - (i * 0.04);
                    results.put(article.getId(), toSearchResult(article, score, "SEMANTIC"));
                } else {
                    // Boost score for articles found by both methods
                    SearchResult existing = results.get(article.getId());
                    results.put(article.getId(), new SearchResult(
                            existing.id(), existing.title(), existing.summary(),
                            existing.snippet(), Math.min(1.0, existing.relevanceScore() + 0.15),
                            "HYBRID", existing.breadcrumb(), existing.status(),
                            existing.parentArticleId(), existing.depth()));
                }
            }
        }

        return results.values().stream()
                .sorted(Comparator.comparingDouble(SearchResult::relevanceScore).reversed())
                .limit(limit)
                .toList();
    }

    private SearchResult toSearchResult(KnowledgeArticle article, double score, String searchType) {
        String snippet = buildSnippet(article);
        var breadcrumb = hierarchyService.getBreadcrumb(article.getTenantId(), article.getId());

        List<String> breadcrumbTitles = breadcrumb.stream()
                .map(de.wissensmanagement.dto.ArticleDto.BreadcrumbItem::getTitle)
                .toList();

        return new SearchResult(
                article.getId(),
                article.getTitle(),
                article.getSummary(),
                snippet,
                score,
                searchType,
                breadcrumbTitles,
                article.getStatus().name(),
                article.getParentArticleId(),
                article.getDepth()
        );
    }

    private String buildSnippet(KnowledgeArticle article) {
        String text = article.getSummary() != null && !article.getSummary().isBlank()
                ? article.getSummary()
                : article.getContent();
        if (text.length() > 250) {
            return text.substring(0, 250) + "...";
        }
        return text;
    }

    public record SearchResult(
            String id,
            String title,
            String summary,
            String snippet,
            double relevanceScore,
            String searchType,
            List<String> breadcrumb,
            String status,
            String parentArticleId,
            int depth
    ) {}
}
