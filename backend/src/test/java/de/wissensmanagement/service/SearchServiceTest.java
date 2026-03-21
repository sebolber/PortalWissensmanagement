package de.wissensmanagement.service;

import de.wissensmanagement.dto.ArticleDto;
import de.wissensmanagement.entity.KnowledgeArticle;
import de.wissensmanagement.enums.ArticleStatus;
import de.wissensmanagement.repository.KnowledgeArticleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private KnowledgeArticleRepository articleRepo;

    @Mock
    private HierarchyService hierarchyService;

    @InjectMocks
    private SearchService searchService;

    private static final String TENANT_ID = "tenant-1";

    private KnowledgeArticle createArticle(String id, String title, String content) {
        KnowledgeArticle article = KnowledgeArticle.builder()
                .tenantId(TENANT_ID)
                .title(title)
                .content(content)
                .summary("Summary of " + title)
                .status(ArticleStatus.PUBLISHED)
                .createdBy("user-1")
                .createdAt(LocalDateTime.now())
                .depth(0)
                .build();
        article.setId(id);
        return article;
    }

    @Test
    void search_returnsEmptyForBlankQuery() {
        List<SearchService.SearchResult> results = searchService.search(TENANT_ID, "", SearchService.SearchMode.HYBRID, 20);
        assertTrue(results.isEmpty());
    }

    @Test
    void search_returnsEmptyForNullQuery() {
        List<SearchService.SearchResult> results = searchService.search(TENANT_ID, null, SearchService.SearchMode.HYBRID, 20);
        assertTrue(results.isEmpty());
    }

    @Test
    void search_fulltextMode_callsFullTextSearch() {
        KnowledgeArticle article = createArticle("art-1", "Test Article", "Content about testing");
        when(articleRepo.fullTextSearch(TENANT_ID, "test", 10)).thenReturn(List.of(article));
        when(hierarchyService.getBreadcrumb(eq(TENANT_ID), anyString()))
                .thenReturn(List.of(ArticleDto.BreadcrumbItem.builder().id("art-1").title("Test Article").build()));

        List<SearchService.SearchResult> results = searchService.search(TENANT_ID, "test", SearchService.SearchMode.FULLTEXT, 10);

        assertFalse(results.isEmpty());
        assertEquals("art-1", results.get(0).id());
        assertEquals("Test Article", results.get(0).title());
        assertEquals("FULLTEXT", results.get(0).searchType());
    }

    @Test
    void search_semanticMode_callsSimilaritySearch() {
        KnowledgeArticle article = createArticle("art-1", "Urlaubsregelung", "Informationen zu Urlaubsanspruch");
        when(articleRepo.similaritySearch(TENANT_ID, "Wie viele Urlaubstage habe ich?", 10))
                .thenReturn(List.of(article));
        when(hierarchyService.getBreadcrumb(eq(TENANT_ID), anyString())).thenReturn(List.of());

        List<SearchService.SearchResult> results = searchService.search(TENANT_ID, "Wie viele Urlaubstage habe ich?",
                SearchService.SearchMode.SEMANTIC, 10);

        assertFalse(results.isEmpty());
        assertEquals("SEMANTIC", results.get(0).searchType());
        assertEquals("Urlaubsregelung", results.get(0).title());
    }

    @Test
    void search_hybridMode_boostsArticlesFoundByBothMethods() {
        KnowledgeArticle article = createArticle("art-1", "Shared Result", "Content");
        when(articleRepo.fullTextSearch(eq(TENANT_ID), eq("query"), anyInt())).thenReturn(List.of(article));
        when(articleRepo.similaritySearch(eq(TENANT_ID), eq("query"), anyInt())).thenReturn(List.of(article));
        when(hierarchyService.getBreadcrumb(eq(TENANT_ID), anyString())).thenReturn(List.of());

        List<SearchService.SearchResult> results = searchService.search(TENANT_ID, "query", SearchService.SearchMode.HYBRID, 20);

        assertFalse(results.isEmpty());
        assertEquals("HYBRID", results.get(0).searchType());
        // Score should be boosted
        assertTrue(results.get(0).relevanceScore() > 1.0 - 0.01);
    }

    @Test
    void search_resultsAreSortedByRelevance() {
        KnowledgeArticle a1 = createArticle("a1", "First", "Content 1");
        KnowledgeArticle a2 = createArticle("a2", "Second", "Content 2");
        when(articleRepo.fullTextSearch(eq(TENANT_ID), eq("query"), anyInt())).thenReturn(List.of(a1, a2));
        when(hierarchyService.getBreadcrumb(eq(TENANT_ID), anyString())).thenReturn(List.of());

        List<SearchService.SearchResult> results = searchService.search(TENANT_ID, "query", SearchService.SearchMode.FULLTEXT, 20);

        assertEquals(2, results.size());
        assertTrue(results.get(0).relevanceScore() >= results.get(1).relevanceScore());
    }

    @Test
    void search_resultsIncludeBreadcrumb() {
        KnowledgeArticle article = createArticle("art-1", "Article", "Content");
        when(articleRepo.fullTextSearch(eq(TENANT_ID), eq("test"), anyInt())).thenReturn(List.of(article));
        when(hierarchyService.getBreadcrumb(TENANT_ID, "art-1"))
                .thenReturn(List.of(
                        ArticleDto.BreadcrumbItem.builder().id("root").title("Root").build(),
                        ArticleDto.BreadcrumbItem.builder().id("art-1").title("Article").build()
                ));

        List<SearchService.SearchResult> results = searchService.search(TENANT_ID, "test", SearchService.SearchMode.FULLTEXT, 10);

        assertEquals(List.of("Root", "Article"), results.get(0).breadcrumb());
    }

    @Test
    void search_respectsLimit() {
        List<KnowledgeArticle> articles = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            articles.add(createArticle("art-" + i, "Article " + i, "Content " + i));
        }
        when(articleRepo.fullTextSearch(eq(TENANT_ID), eq("query"), eq(5))).thenReturn(articles.subList(0, 5));
        when(hierarchyService.getBreadcrumb(eq(TENANT_ID), anyString())).thenReturn(List.of());

        List<SearchService.SearchResult> results = searchService.search(TENANT_ID, "query", SearchService.SearchMode.FULLTEXT, 5);

        assertTrue(results.size() <= 5);
    }

    @Test
    void search_snippetFallsBackToContent() {
        KnowledgeArticle article = createArticle("art-1", "Article", "Content text here");
        article.setSummary(null);
        when(articleRepo.fullTextSearch(eq(TENANT_ID), eq("test"), anyInt())).thenReturn(List.of(article));
        when(hierarchyService.getBreadcrumb(eq(TENANT_ID), anyString())).thenReturn(List.of());

        List<SearchService.SearchResult> results = searchService.search(TENANT_ID, "test", SearchService.SearchMode.FULLTEXT, 10);

        assertNotNull(results.get(0).snippet());
        assertTrue(results.get(0).snippet().contains("Content text here"));
    }

    @Test
    void search_hybridDeduplicatesResults() {
        KnowledgeArticle a1 = createArticle("a1", "Article One", "Content One");
        KnowledgeArticle a2 = createArticle("a2", "Article Two", "Content Two");
        when(articleRepo.fullTextSearch(eq(TENANT_ID), eq("test"), anyInt())).thenReturn(List.of(a1));
        when(articleRepo.similaritySearch(eq(TENANT_ID), eq("test"), anyInt())).thenReturn(List.of(a1, a2));
        when(hierarchyService.getBreadcrumb(eq(TENANT_ID), anyString())).thenReturn(List.of());

        List<SearchService.SearchResult> results = searchService.search(TENANT_ID, "test", SearchService.SearchMode.HYBRID, 20);

        // a1 should appear once (as HYBRID) and a2 should appear once (as SEMANTIC)
        assertEquals(2, results.size());
        long hybridCount = results.stream().filter(r -> "HYBRID".equals(r.searchType())).count();
        long semanticCount = results.stream().filter(r -> "SEMANTIC".equals(r.searchType())).count();
        assertEquals(1, hybridCount);
        assertEquals(1, semanticCount);
    }
}
