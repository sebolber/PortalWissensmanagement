package de.wissensmanagement.service;

import de.wissensmanagement.entity.KnowledgeArticle;
import de.wissensmanagement.repository.KnowledgeArticleRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
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

    /**
     * Unified search combining fulltext and semantic search.
     * Currently fulltext-only; semantic search is prepared for when embeddings are available.
     */
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
                double score = 1.0 - (i * 0.05); // decreasing relevance
                results.put(article.getId(), toSearchResult(article, score, "FULLTEXT"));
            }
        }

        // Semantic search placeholder (HYBRID and SEMANTIC modes)
        if (mode == SearchMode.SEMANTIC || mode == SearchMode.HYBRID) {
            // When vector embeddings are available, this will query the vector store.
            // For now, fall back to a LIKE-based similarity heuristic for broader matches.
            List<KnowledgeArticle> likeResults = articleRepo.searchByTenant(tenantId,
                    de.wissensmanagement.enums.ArticleStatus.PUBLISHED, query,
                    org.springframework.data.domain.PageRequest.of(0, limit)).getContent();

            for (int i = 0; i < likeResults.size(); i++) {
                KnowledgeArticle article = likeResults.get(i);
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
