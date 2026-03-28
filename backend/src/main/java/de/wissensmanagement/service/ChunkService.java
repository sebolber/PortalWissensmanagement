package de.wissensmanagement.service;

import de.wissensmanagement.entity.KnowledgeArticle;
import de.wissensmanagement.entity.KnowledgeChunk;
import de.wissensmanagement.repository.KnowledgeChunkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits knowledge articles into retrieval-optimized chunks.
 * Enriches each chunk with article title and section heading for better
 * search relevance and context attribution.
 */
@Service
@Transactional(readOnly = true)
public class ChunkService {

    private static final int MAX_CHUNK_CHARS = 1500;
    private static final int OVERLAP_CHARS = 200;
    private static final Pattern HEADING_PATTERN = Pattern.compile(
            "^(#{1,6}\\s+.+|[A-Z\u00C4\u00D6\u00DC][^\\n]{2,80})$", Pattern.MULTILINE);

    private final KnowledgeChunkRepository chunkRepo;

    public ChunkService(KnowledgeChunkRepository chunkRepo) {
        this.chunkRepo = chunkRepo;
    }

    @Transactional
    public void rechunk(KnowledgeArticle article) {
        chunkRepo.deleteByArticleId(article.getId());

        String fullText = buildFullText(article);
        List<ChunkWithHeading> chunks = splitIntoChunks(fullText);

        for (int i = 0; i < chunks.size(); i++) {
            ChunkWithHeading cwh = chunks.get(i);
            KnowledgeChunk chunk = KnowledgeChunk.builder()
                    .tenantId(article.getTenantId())
                    .articleId(article.getId())
                    .articleTitle(article.getTitle())
                    .heading(cwh.heading())
                    .chunkIndex(i)
                    .content(cwh.content())
                    .tokenCount(estimateTokens(cwh.content()))
                    .build();
            chunkRepo.save(chunk);
        }
    }

    private String buildFullText(KnowledgeArticle article) {
        StringBuilder sb = new StringBuilder();
        sb.append(article.getTitle());
        if (article.getSummary() != null && !article.getSummary().isBlank()) {
            sb.append("\n\n").append(article.getSummary());
        }
        sb.append("\n\n").append(article.getContent());
        return sb.toString();
    }

    private List<ChunkWithHeading> splitIntoChunks(String text) {
        List<ChunkWithHeading> chunks = new ArrayList<>();
        if (text.length() <= MAX_CHUNK_CHARS) {
            chunks.add(new ChunkWithHeading(text, detectHeading(text)));
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

            String chunkText = text.substring(start, end).trim();
            if (!chunkText.isEmpty()) {
                chunks.add(new ChunkWithHeading(chunkText, detectHeading(chunkText)));
            }
            start = Math.max(start + 1, end - OVERLAP_CHARS);
        }

        return chunks;
    }

    /**
     * Detects the first heading-like line in a chunk for context attribution.
     */
    private String detectHeading(String chunkText) {
        Matcher matcher = HEADING_PATTERN.matcher(chunkText);
        if (matcher.find()) {
            String heading = matcher.group().replaceAll("^#+\\s+", "").trim();
            if (heading.length() <= 200) {
                return heading;
            }
        }
        return null;
    }

    private int estimateTokens(String text) {
        return (int) Math.ceil(text.length() / 3.5);
    }

    private record ChunkWithHeading(String content, String heading) {}
}
