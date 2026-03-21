package de.wissensmanagement.service;

import de.wissensmanagement.entity.KnowledgeArticle;
import de.wissensmanagement.entity.KnowledgeChunk;
import de.wissensmanagement.enums.ArticleStatus;
import de.wissensmanagement.repository.KnowledgeChunkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChunkServiceTest {

    @Mock
    private KnowledgeChunkRepository chunkRepo;

    @InjectMocks
    private ChunkService chunkService;

    private KnowledgeArticle createArticle(String title, String content, String summary) {
        KnowledgeArticle article = KnowledgeArticle.builder()
                .tenantId("tenant-1")
                .title(title)
                .content(content)
                .summary(summary)
                .status(ArticleStatus.PUBLISHED)
                .createdBy("user-1")
                .createdAt(LocalDateTime.now())
                .build();
        article.setId("art-1");
        return article;
    }

    @Test
    void rechunk_deletesOldChunksFirst() {
        KnowledgeArticle article = createArticle("Title", "Short content", null);
        when(chunkRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        chunkService.rechunk(article);

        verify(chunkRepo).deleteByArticleId("art-1");
    }

    @Test
    void rechunk_createsChunksWithArticleTitle() {
        KnowledgeArticle article = createArticle("Important Article", "Some content", null);
        when(chunkRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        chunkService.rechunk(article);

        ArgumentCaptor<KnowledgeChunk> captor = ArgumentCaptor.forClass(KnowledgeChunk.class);
        verify(chunkRepo, atLeastOnce()).save(captor.capture());

        KnowledgeChunk savedChunk = captor.getValue();
        assertEquals("Important Article", savedChunk.getArticleTitle());
        assertEquals("tenant-1", savedChunk.getTenantId());
        assertEquals("art-1", savedChunk.getArticleId());
    }

    @Test
    void rechunk_includesSummaryInChunkContent() {
        KnowledgeArticle article = createArticle("Title", "Content here", "This is the summary");
        when(chunkRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        chunkService.rechunk(article);

        ArgumentCaptor<KnowledgeChunk> captor = ArgumentCaptor.forClass(KnowledgeChunk.class);
        verify(chunkRepo).save(captor.capture());
        assertTrue(captor.getValue().getContent().contains("This is the summary"));
    }

    @Test
    void rechunk_splitsLongContentIntoMultipleChunks() {
        String longContent = "Paragraph one. ".repeat(200); // ~3000 chars
        KnowledgeArticle article = createArticle("Title", longContent, null);
        when(chunkRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        chunkService.rechunk(article);

        // Should create more than one chunk
        verify(chunkRepo, atLeast(2)).save(any(KnowledgeChunk.class));
    }

    @Test
    void rechunk_shortContentCreatesOneChunk() {
        KnowledgeArticle article = createArticle("Title", "Short content", null);
        when(chunkRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        chunkService.rechunk(article);

        verify(chunkRepo, times(1)).save(any(KnowledgeChunk.class));
    }

    @Test
    void rechunk_chunksHaveCorrectIndexes() {
        String longContent = "A".repeat(4000); // Will split into multiple chunks
        KnowledgeArticle article = createArticle("Title", longContent, null);
        when(chunkRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        chunkService.rechunk(article);

        ArgumentCaptor<KnowledgeChunk> captor = ArgumentCaptor.forClass(KnowledgeChunk.class);
        verify(chunkRepo, atLeast(2)).save(captor.capture());

        List<KnowledgeChunk> chunks = captor.getAllValues();
        for (int i = 0; i < chunks.size(); i++) {
            assertEquals(i, chunks.get(i).getChunkIndex());
        }
    }

    @Test
    void rechunk_estimatesTokenCount() {
        KnowledgeArticle article = createArticle("Title", "Some content text here for testing", null);
        when(chunkRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        chunkService.rechunk(article);

        ArgumentCaptor<KnowledgeChunk> captor = ArgumentCaptor.forClass(KnowledgeChunk.class);
        verify(chunkRepo).save(captor.capture());
        assertNotNull(captor.getValue().getTokenCount());
        assertTrue(captor.getValue().getTokenCount() > 0);
    }

    @Test
    void rechunk_preservesTenantId() {
        KnowledgeArticle article = createArticle("Title", "Content", null);
        when(chunkRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        chunkService.rechunk(article);

        ArgumentCaptor<KnowledgeChunk> captor = ArgumentCaptor.forClass(KnowledgeChunk.class);
        verify(chunkRepo).save(captor.capture());
        assertEquals("tenant-1", captor.getValue().getTenantId());
    }
}
