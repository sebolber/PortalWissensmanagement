package de.wissensmanagement.service;

import de.wissensmanagement.entity.KnowledgeArticle;
import de.wissensmanagement.entity.KnowledgeChunk;
import de.wissensmanagement.repository.KnowledgeArticleRepository;
import de.wissensmanagement.repository.KnowledgeChunkRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RetrievalService {

    private static final int MAX_CHUNKS = 10;
    private static final int MAX_CONTEXT_TOKENS = 4000;

    private final KnowledgeChunkRepository chunkRepo;
    private final KnowledgeArticleRepository articleRepo;

    public RetrievalService(KnowledgeChunkRepository chunkRepo, KnowledgeArticleRepository articleRepo) {
        this.chunkRepo = chunkRepo;
        this.articleRepo = articleRepo;
    }

    public RetrievalResult retrieve(String tenantId, String query) {
        List<KnowledgeChunk> chunks = chunkRepo.fullTextSearchChunks(tenantId, query, MAX_CHUNKS);

        if (chunks.isEmpty()) {
            return new RetrievalResult("", List.of());
        }

        // Build context from chunks while respecting token limit
        StringBuilder context = new StringBuilder();
        int totalTokens = 0;
        Set<String> usedArticleIds = new LinkedHashSet<>();

        for (KnowledgeChunk chunk : chunks) {
            int tokens = chunk.getTokenCount() != null ? chunk.getTokenCount() : estimateTokens(chunk.getContent());
            if (totalTokens + tokens > MAX_CONTEXT_TOKENS && !context.isEmpty()) {
                break;
            }
            if (!context.isEmpty()) {
                context.append("\n\n---\n\n");
            }
            context.append(chunk.getContent());
            totalTokens += tokens;
            usedArticleIds.add(chunk.getArticleId());
        }

        // Resolve article metadata for source references
        List<SourceReference> sources = new ArrayList<>();
        for (String articleId : usedArticleIds) {
            articleRepo.findById(articleId).ifPresent(article -> {
                sources.add(new SourceReference(
                        article.getId(),
                        article.getTitle(),
                        article.getCategory() != null ? article.getCategory().getName() : null
                ));
            });

            // Increment usage count
            articleRepo.findById(articleId).ifPresent(article -> {
                article.setUsageCount(article.getUsageCount() + 1);
                article.setLastUsedAt(java.time.LocalDateTime.now());
                articleRepo.save(article);
            });
        }

        return new RetrievalResult(context.toString(), sources);
    }

    private int estimateTokens(String text) {
        return (int) Math.ceil(text.length() / 3.5);
    }

    public record RetrievalResult(String context, List<SourceReference> sources) {}

    public record SourceReference(String articleId, String title, String categoryName) {}
}
