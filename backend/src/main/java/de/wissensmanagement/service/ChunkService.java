package de.wissensmanagement.service;

import de.wissensmanagement.entity.KnowledgeArticle;
import de.wissensmanagement.entity.KnowledgeChunk;
import de.wissensmanagement.repository.KnowledgeChunkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkService {

    private static final int MAX_CHUNK_CHARS = 1500;
    private static final int OVERLAP_CHARS = 200;

    private final KnowledgeChunkRepository chunkRepo;

    public ChunkService(KnowledgeChunkRepository chunkRepo) {
        this.chunkRepo = chunkRepo;
    }

    @Transactional
    public void rechunk(KnowledgeArticle article) {
        chunkRepo.deleteByArticleId(article.getId());

        String fullText = article.getTitle() + "\n\n" + article.getContent();
        if (article.getSummary() != null && !article.getSummary().isBlank()) {
            fullText = article.getTitle() + "\n\n" + article.getSummary() + "\n\n" + article.getContent();
        }

        List<String> chunks = splitIntoChunks(fullText);

        for (int i = 0; i < chunks.size(); i++) {
            String chunkContent = chunks.get(i);
            KnowledgeChunk chunk = KnowledgeChunk.builder()
                    .tenantId(article.getTenantId())
                    .articleId(article.getId())
                    .chunkIndex(i)
                    .content(chunkContent)
                    .tokenCount(estimateTokens(chunkContent))
                    .build();
            chunkRepo.save(chunk);
        }
    }

    private List<String> splitIntoChunks(String text) {
        List<String> chunks = new ArrayList<>();
        if (text.length() <= MAX_CHUNK_CHARS) {
            chunks.add(text);
            return chunks;
        }

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + MAX_CHUNK_CHARS, text.length());

            if (end < text.length()) {
                int lastParagraph = text.lastIndexOf("\n\n", end);
                int lastSentence = text.lastIndexOf(". ", end);
                int lastNewline = text.lastIndexOf("\n", end);

                if (lastParagraph > start + MAX_CHUNK_CHARS / 3) {
                    end = lastParagraph + 2;
                } else if (lastSentence > start + MAX_CHUNK_CHARS / 3) {
                    end = lastSentence + 2;
                } else if (lastNewline > start + MAX_CHUNK_CHARS / 3) {
                    end = lastNewline + 1;
                }
            }

            chunks.add(text.substring(start, end).trim());
            start = Math.max(start + 1, end - OVERLAP_CHARS);
        }

        return chunks;
    }

    private int estimateTokens(String text) {
        return (int) Math.ceil(text.length() / 3.5);
    }
}
