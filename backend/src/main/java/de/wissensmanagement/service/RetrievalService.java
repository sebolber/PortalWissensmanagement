package de.wissensmanagement.service;

import de.wissensmanagement.entity.KnowledgeArticle;
import de.wissensmanagement.entity.KnowledgeChunk;
import de.wissensmanagement.repository.KnowledgeArticleRepository;
import de.wissensmanagement.repository.KnowledgeChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Multi-strategy retrieval service for knowledge base RAG.
 * Uses a layered approach:
 * 1. Fulltext search on chunks (exact keyword match via tsvector)
 * 2. Similarity search on chunks (trigram-based for paraphrased queries)
 * 3. Article-level fallback search when chunk search yields insufficient results
 *
 * Each step adds results incrementally, deduplicating by article ID.
 */
@Service
@Transactional(readOnly = true)
public class RetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RetrievalService.class);

    private static final int MAX_CHUNKS = 12;
    private static final int MAX_CONTEXT_TOKENS = 6000;
    private static final int MIN_DESIRED_CHUNKS = 3;

    private final KnowledgeChunkRepository chunkRepo;
    private final KnowledgeArticleRepository articleRepo;

    public RetrievalService(KnowledgeChunkRepository chunkRepo, KnowledgeArticleRepository articleRepo) {
        this.chunkRepo = chunkRepo;
        this.articleRepo = articleRepo;
    }

    @Transactional
    public RetrievalResult retrieve(String tenantId, String query) {
        List<RetrievalTrace> traces = new ArrayList<>();
        Map<String, KnowledgeChunk> candidateChunks = new LinkedHashMap<>();

        // Strategy 1: Fulltext search on chunks (most precise)
        List<KnowledgeChunk> fulltextChunks = chunkRepo.fullTextSearchChunks(tenantId, query, MAX_CHUNKS);
        traces.add(new RetrievalTrace("FULLTEXT_CHUNKS", query, fulltextChunks.size()));
        for (KnowledgeChunk chunk : fulltextChunks) {
            candidateChunks.putIfAbsent(chunk.getId(), chunk);
        }

        // Strategy 2: Trigram similarity search on chunks (for paraphrased/synonymous queries)
        if (candidateChunks.size() < MIN_DESIRED_CHUNKS) {
            List<KnowledgeChunk> similarChunks = chunkRepo.similaritySearchChunks(tenantId, query, MAX_CHUNKS);
            traces.add(new RetrievalTrace("SIMILARITY_CHUNKS", query, similarChunks.size()));
            for (KnowledgeChunk chunk : similarChunks) {
                candidateChunks.putIfAbsent(chunk.getId(), chunk);
            }
        }

        // Strategy 3: Article-level fulltext search, then fetch chunks from matching articles
        if (candidateChunks.size() < MIN_DESIRED_CHUNKS) {
            List<KnowledgeArticle> fulltextArticles = articleRepo.fullTextSearch(tenantId, query, 5);
            traces.add(new RetrievalTrace("FULLTEXT_ARTICLES", query, fulltextArticles.size()));
            Set<String> existingArticleIds = candidateChunks.values().stream()
                    .map(KnowledgeChunk::getArticleId).collect(Collectors.toSet());
            for (KnowledgeArticle article : fulltextArticles) {
                if (!existingArticleIds.contains(article.getId())) {
                    List<KnowledgeChunk> articleChunks = chunkRepo.findByArticleIdOrderByChunkIndexAsc(article.getId());
                    for (KnowledgeChunk chunk : articleChunks) {
                        candidateChunks.putIfAbsent(chunk.getId(), chunk);
                    }
                }
            }
        }

        // Strategy 4: Article-level similarity search as last resort
        if (candidateChunks.size() < MIN_DESIRED_CHUNKS) {
            List<KnowledgeArticle> similarArticles = articleRepo.similaritySearch(tenantId, query, 5);
            traces.add(new RetrievalTrace("SIMILARITY_ARTICLES", query, similarArticles.size()));
            Set<String> existingArticleIds = candidateChunks.values().stream()
                    .map(KnowledgeChunk::getArticleId).collect(Collectors.toSet());
            for (KnowledgeArticle article : similarArticles) {
                if (!existingArticleIds.contains(article.getId())) {
                    List<KnowledgeChunk> articleChunks = chunkRepo.findByArticleIdOrderByChunkIndexAsc(article.getId());
                    for (KnowledgeChunk chunk : articleChunks) {
                        candidateChunks.putIfAbsent(chunk.getId(), chunk);
                    }
                }
            }
        }

        if (candidateChunks.isEmpty()) {
            log.info("Retrieval found no results for query='{}' in tenant={}", query, tenantId);
            return new RetrievalResult("", List.of(), traces);
        }

        // Build context from chunks while respecting token limit
        StringBuilder context = new StringBuilder();
        int totalTokens = 0;
        Set<String> usedArticleIds = new LinkedHashSet<>();

        for (KnowledgeChunk chunk : candidateChunks.values()) {
            int tokens = chunk.getTokenCount() != null ? chunk.getTokenCount() : estimateTokens(chunk.getContent());
            if (totalTokens + tokens > MAX_CONTEXT_TOKENS && !context.isEmpty()) {
                break;
            }

            if (!context.isEmpty()) {
                context.append("\n\n---\n\n");
            }

            // Add article title as context header for each chunk
            if (chunk.getArticleTitle() != null && !chunk.getArticleTitle().isBlank()) {
                context.append("[Quelle: ").append(chunk.getArticleTitle());
                if (chunk.getHeading() != null && !chunk.getHeading().isBlank()) {
                    context.append(" > ").append(chunk.getHeading());
                }
                context.append("]\n");
            }
            context.append(chunk.getContent());
            totalTokens += tokens;
            usedArticleIds.add(chunk.getArticleId());
        }

        log.info("Retrieval for query='{}': {} candidate chunks, {} used articles, {} tokens",
                query, candidateChunks.size(), usedArticleIds.size(), totalTokens);

        // Resolve article metadata for source references
        List<SourceReference> sources = new ArrayList<>();
        for (String articleId : usedArticleIds) {
            articleRepo.findById(articleId).ifPresent(article -> {
                sources.add(new SourceReference(
                        article.getId(),
                        article.getTitle(),
                        article.getCategory() != null ? article.getCategory().getName() : null
                ));
                // Increment usage count
                article.setUsageCount(article.getUsageCount() + 1);
                article.setLastUsedAt(java.time.LocalDateTime.now());
                articleRepo.save(article);
            });
        }

        return new RetrievalResult(context.toString(), sources, traces);
    }

    private int estimateTokens(String text) {
        return (int) Math.ceil(text.length() / 3.5);
    }

    public record RetrievalResult(String context, List<SourceReference> sources, List<RetrievalTrace> traces) {}

    public record SourceReference(String articleId, String title, String categoryName) {}

    public record RetrievalTrace(String strategy, String query, int resultCount) {}
}
