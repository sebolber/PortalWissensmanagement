package de.wissensmanagement.service;

import de.wissensmanagement.entity.KnowledgeArticle;
import de.wissensmanagement.entity.KnowledgeChunk;
import de.wissensmanagement.enums.ArticleStatus;
import de.wissensmanagement.repository.KnowledgeArticleRepository;
import de.wissensmanagement.repository.KnowledgeChunkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetrievalServiceTest {

    @Mock
    private KnowledgeChunkRepository chunkRepo;

    @Mock
    private KnowledgeArticleRepository articleRepo;

    @InjectMocks
    private RetrievalService retrievalService;

    private static final String TENANT_ID = "tenant-1";

    private KnowledgeChunk createChunk(String id, String articleId, String content, int index) {
        return KnowledgeChunk.builder()
                .tenantId(TENANT_ID)
                .articleId(articleId)
                .articleTitle("Test Article")
                .chunkIndex(index)
                .content(content)
                .tokenCount((int) Math.ceil(content.length() / 3.5))
                .build();
    }

    private KnowledgeArticle createArticle(String id, String title) {
        KnowledgeArticle article = KnowledgeArticle.builder()
                .tenantId(TENANT_ID)
                .title(title)
                .content("Content of " + title)
                .status(ArticleStatus.PUBLISHED)
                .createdBy("user-1")
                .createdAt(LocalDateTime.now())
                .build();
        article.setId(id);
        return article;
    }

    @Test
    void retrieve_returnsContextFromFulltextChunks() {
        KnowledgeChunk chunk = createChunk("c1", "art-1", "Relevant content about topic", 0);
        chunk.setId("c1");
        KnowledgeArticle article = createArticle("art-1", "Topic Article");

        when(chunkRepo.fullTextSearchChunks(TENANT_ID, "topic", 12)).thenReturn(List.of(chunk));
        when(articleRepo.findById("art-1")).thenReturn(Optional.of(article));
        when(articleRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RetrievalService.RetrievalResult result = retrievalService.retrieve(TENANT_ID, "topic");

        assertFalse(result.context().isEmpty());
        assertTrue(result.context().contains("Relevant content about topic"));
        assertEquals(1, result.sources().size());
        assertEquals("Topic Article", result.sources().get(0).title());
    }

    @Test
    void retrieve_fallsBackToSimilarityWhenFulltextFindsNothing() {
        KnowledgeChunk chunk = createChunk("c1", "art-1", "Informationen zu Urlaubsanspruch und Ferientagen", 0);
        chunk.setId("c1");
        KnowledgeArticle article = createArticle("art-1", "Urlaubsregelung");

        when(chunkRepo.fullTextSearchChunks(TENANT_ID, "Wie viele freie Tage bekomme ich?", 12))
                .thenReturn(List.of());
        when(chunkRepo.similaritySearchChunks(TENANT_ID, "Wie viele freie Tage bekomme ich?", 12))
                .thenReturn(List.of(chunk));
        when(articleRepo.findById("art-1")).thenReturn(Optional.of(article));
        when(articleRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RetrievalService.RetrievalResult result = retrievalService.retrieve(TENANT_ID, "Wie viele freie Tage bekomme ich?");

        assertFalse(result.context().isEmpty());
        assertTrue(result.context().contains("Urlaubsanspruch"));
        assertEquals(1, result.sources().size());
    }

    @Test
    void retrieve_fallsBackToArticleLevelSearch() {
        KnowledgeArticle article = createArticle("art-1", "Datenschutzrichtlinie");
        KnowledgeChunk chunk = createChunk("c1", "art-1", "Regelungen zum Umgang mit personenbezogenen Daten", 0);
        chunk.setId("c1");

        when(chunkRepo.fullTextSearchChunks(any(), any(), anyInt())).thenReturn(List.of());
        when(chunkRepo.similaritySearchChunks(any(), any(), anyInt())).thenReturn(List.of());
        when(articleRepo.fullTextSearch(TENANT_ID, "Datenschutz", 5)).thenReturn(List.of(article));
        when(chunkRepo.findByArticleIdOrderByChunkIndexAsc("art-1")).thenReturn(List.of(chunk));
        when(articleRepo.findById("art-1")).thenReturn(Optional.of(article));
        when(articleRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RetrievalService.RetrievalResult result = retrievalService.retrieve(TENANT_ID, "Datenschutz");

        assertFalse(result.context().isEmpty());
        assertEquals(1, result.sources().size());
    }

    @Test
    void retrieve_returnsEmptyForNoResults() {
        when(chunkRepo.fullTextSearchChunks(any(), any(), anyInt())).thenReturn(List.of());
        when(chunkRepo.similaritySearchChunks(any(), any(), anyInt())).thenReturn(List.of());
        when(articleRepo.fullTextSearch(any(), any(), anyInt())).thenReturn(List.of());
        when(articleRepo.similaritySearch(any(), any(), anyInt())).thenReturn(List.of());

        RetrievalService.RetrievalResult result = retrievalService.retrieve(TENANT_ID, "xyznonexistent");

        assertTrue(result.context().isEmpty());
        assertTrue(result.sources().isEmpty());
    }

    @Test
    void retrieve_respectsTokenLimit() {
        // Create chunks that together exceed the token limit
        String longContent = "A".repeat(7000); // ~2000 tokens each
        KnowledgeChunk c1 = createChunk("c1", "art-1", longContent, 0);
        c1.setId("c1");
        KnowledgeChunk c2 = createChunk("c2", "art-1", longContent, 1);
        c2.setId("c2");
        KnowledgeChunk c3 = createChunk("c3", "art-2", longContent, 0);
        c3.setId("c3");
        KnowledgeChunk c4 = createChunk("c4", "art-2", longContent, 1);
        c4.setId("c4");

        when(chunkRepo.fullTextSearchChunks(any(), any(), anyInt())).thenReturn(List.of(c1, c2, c3, c4));
        when(articleRepo.findById("art-1")).thenReturn(Optional.of(createArticle("art-1", "Article 1")));
        when(articleRepo.findById("art-2")).thenReturn(Optional.of(createArticle("art-2", "Article 2")));
        when(articleRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RetrievalService.RetrievalResult result = retrievalService.retrieve(TENANT_ID, "test");

        // Should have limited context, not all 4 chunks
        assertTrue(result.context().length() < longContent.length() * 4);
    }

    @Test
    void retrieve_includesTraceInformation() {
        when(chunkRepo.fullTextSearchChunks(any(), any(), anyInt())).thenReturn(List.of());
        when(chunkRepo.similaritySearchChunks(any(), any(), anyInt())).thenReturn(List.of());
        when(articleRepo.fullTextSearch(any(), any(), anyInt())).thenReturn(List.of());
        when(articleRepo.similaritySearch(any(), any(), anyInt())).thenReturn(List.of());

        RetrievalService.RetrievalResult result = retrievalService.retrieve(TENANT_ID, "query");

        assertNotNull(result.traces());
        assertFalse(result.traces().isEmpty());
        assertTrue(result.traces().stream().anyMatch(t -> "FULLTEXT_CHUNKS".equals(t.strategy())));
    }

    @Test
    void retrieve_mandantIsolation_onlySearchesOwnTenant() {
        when(chunkRepo.fullTextSearchChunks(eq("tenant-A"), any(), anyInt())).thenReturn(List.of());
        when(chunkRepo.similaritySearchChunks(eq("tenant-A"), any(), anyInt())).thenReturn(List.of());
        when(articleRepo.fullTextSearch(eq("tenant-A"), any(), anyInt())).thenReturn(List.of());
        when(articleRepo.similaritySearch(eq("tenant-A"), any(), anyInt())).thenReturn(List.of());

        retrievalService.retrieve("tenant-A", "query");

        verify(chunkRepo).fullTextSearchChunks(eq("tenant-A"), any(), anyInt());
        verify(chunkRepo, never()).fullTextSearchChunks(eq("tenant-B"), any(), anyInt());
    }

    @Test
    void retrieve_addsArticleTitleToContext() {
        KnowledgeChunk chunk = createChunk("c1", "art-1", "Chunk content here", 0);
        chunk.setId("c1");
        chunk.setArticleTitle("Important Article");
        KnowledgeArticle article = createArticle("art-1", "Important Article");

        when(chunkRepo.fullTextSearchChunks(any(), any(), anyInt())).thenReturn(List.of(chunk));
        when(articleRepo.findById("art-1")).thenReturn(Optional.of(article));
        when(articleRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RetrievalService.RetrievalResult result = retrievalService.retrieve(TENANT_ID, "query");

        assertTrue(result.context().contains("[Quelle: Important Article]"));
    }
}
