package de.wissensmanagement.service;

import de.wissensmanagement.dto.*;
import de.wissensmanagement.entity.*;
import de.wissensmanagement.enums.ArticleStatus;
import de.wissensmanagement.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ArticleService {

    private final KnowledgeArticleRepository articleRepo;
    private final KnowledgeCategoryRepository categoryRepo;
    private final KnowledgeTagRepository tagRepo;
    private final ArticleVersionRepository versionRepo;
    private final ChunkService chunkService;

    public ArticleService(KnowledgeArticleRepository articleRepo,
                           KnowledgeCategoryRepository categoryRepo,
                           KnowledgeTagRepository tagRepo,
                           ArticleVersionRepository versionRepo,
                           ChunkService chunkService) {
        this.articleRepo = articleRepo;
        this.categoryRepo = categoryRepo;
        this.tagRepo = tagRepo;
        this.versionRepo = versionRepo;
        this.chunkService = chunkService;
    }

    public Page<ArticleDto> listArticles(String tenantId, ArticleStatus status, String search,
                                          String categoryId, Pageable pageable) {
        Page<KnowledgeArticle> page;

        if (search != null && !search.isBlank()) {
            ArticleStatus searchStatus = status != null ? status : ArticleStatus.PUBLISHED;
            page = articleRepo.searchByTenant(tenantId, searchStatus, search, pageable);
        } else if (status != null) {
            page = articleRepo.findByTenantIdAndStatus(tenantId, status, pageable);
        } else {
            page = articleRepo.findByTenantId(tenantId, pageable);
        }

        return page.map(this::toDto);
    }

    public ArticleDto getArticle(String tenantId, String id) {
        KnowledgeArticle article = findByTenant(tenantId, id);
        return toDto(article);
    }

    @Transactional
    public ArticleDto getArticleAndTrackView(String tenantId, String id) {
        KnowledgeArticle article = findByTenant(tenantId, id);
        articleRepo.incrementViewCount(id);
        article.setViewCount(article.getViewCount() + 1);
        return toDto(article);
    }

    @Transactional
    public ArticleDto createArticle(String tenantId, String userId, ArticleCreateRequest req) {
        KnowledgeArticle article = KnowledgeArticle.builder()
                .tenantId(tenantId)
                .title(req.getTitle())
                .content(req.getContent())
                .summary(req.getSummary())
                .status(ArticleStatus.DRAFT)
                .createdBy(userId)
                .publicWithinTenant(req.isPublicWithinTenant())
                .linkedTaskId(req.getLinkedTaskId())
                .build();

        if (req.getCategoryId() != null) {
            categoryRepo.findByIdAndTenantId(req.getCategoryId(), tenantId)
                    .ifPresent(article::setCategory);
        }

        article = articleRepo.save(article);
        resolveTags(article, tenantId, req.getTagNames());
        article = articleRepo.save(article);

        chunkService.rechunk(article);

        return toDto(article);
    }

    @Transactional
    public ArticleDto updateArticle(String tenantId, String userId, String id, ArticleUpdateRequest req) {
        KnowledgeArticle article = findByTenant(tenantId, id);

        // Alte Version speichern
        ArticleVersion version = ArticleVersion.builder()
                .articleId(article.getId())
                .version(article.getVersion())
                .title(article.getTitle())
                .content(article.getContent())
                .summary(article.getSummary())
                .changedBy(userId)
                .changeNote(req.getChangeNote())
                .build();
        versionRepo.save(version);

        article.setTitle(req.getTitle());
        article.setContent(req.getContent());
        article.setSummary(req.getSummary());
        article.setUpdatedBy(userId);
        article.setVersion(article.getVersion() + 1);
        article.setPublicWithinTenant(req.isPublicWithinTenant());
        article.setLinkedTaskId(req.getLinkedTaskId());

        if (req.getCategoryId() != null) {
            categoryRepo.findByIdAndTenantId(req.getCategoryId(), tenantId)
                    .ifPresent(article::setCategory);
        } else {
            article.setCategory(null);
        }

        resolveTags(article, tenantId, req.getTagNames());
        article = articleRepo.save(article);

        chunkService.rechunk(article);

        return toDto(article);
    }

    @Transactional
    public ArticleDto publishArticle(String tenantId, String id) {
        KnowledgeArticle article = findByTenant(tenantId, id);
        article.setStatus(ArticleStatus.PUBLISHED);
        return toDto(articleRepo.save(article));
    }

    @Transactional
    public ArticleDto archiveArticle(String tenantId, String id) {
        KnowledgeArticle article = findByTenant(tenantId, id);
        article.setStatus(ArticleStatus.ARCHIVED);
        return toDto(articleRepo.save(article));
    }

    @Transactional
    public void deleteArticle(String tenantId, String id) {
        KnowledgeArticle article = findByTenant(tenantId, id);
        articleRepo.delete(article);
    }

    public List<ArticleVersionDto> getVersionHistory(String tenantId, String id) {
        findByTenant(tenantId, id);
        return versionRepo.findByArticleIdOrderByVersionDesc(id).stream()
                .map(v -> ArticleVersionDto.builder()
                        .id(v.getId())
                        .version(v.getVersion())
                        .title(v.getTitle())
                        .content(v.getContent())
                        .summary(v.getSummary())
                        .changedBy(v.getChangedBy())
                        .changedAt(v.getChangedAt())
                        .changeNote(v.getChangeNote())
                        .build())
                .toList();
    }

    public List<ArticleDto> getNewestArticles(String tenantId, int limit) {
        return articleRepo.findNewest(tenantId, PageRequest.of(0, limit)).stream()
                .map(this::toDto).toList();
    }

    public List<ArticleDto> getMostViewedArticles(String tenantId, int limit) {
        return articleRepo.findMostViewed(tenantId, PageRequest.of(0, limit)).stream()
                .map(this::toDto).toList();
    }

    public List<ArticleDto> getRecentlyUsedArticles(String tenantId, int limit) {
        return articleRepo.findRecentlyUsed(tenantId, PageRequest.of(0, limit)).stream()
                .map(this::toDto).toList();
    }

    public List<ArticleDto> getArticlesForTask(String tenantId, String taskId) {
        return articleRepo.findByTenantAndTask(tenantId, taskId).stream()
                .map(this::toDto).toList();
    }

    public StatistikDto getStatistik(String tenantId) {
        return StatistikDto.builder()
                .gesamt(articleRepo.countByTenantId(tenantId))
                .veroeffentlicht(articleRepo.countByTenantIdAndStatus(tenantId, ArticleStatus.PUBLISHED))
                .entwuerfe(articleRepo.countByTenantIdAndStatus(tenantId, ArticleStatus.DRAFT))
                .kategorien(categoryRepo.countByTenantId(tenantId))
                .build();
    }

    private KnowledgeArticle findByTenant(String tenantId, String id) {
        return articleRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Artikel nicht gefunden: " + id));
    }

    private void resolveTags(KnowledgeArticle article, String tenantId, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            article.setTags(new HashSet<>());
            return;
        }
        Set<KnowledgeTag> tags = tagNames.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(name -> tagRepo.findByTenantIdAndName(tenantId, name.trim())
                        .orElseGet(() -> tagRepo.save(KnowledgeTag.builder()
                                .tenantId(tenantId).name(name.trim()).build())))
                .collect(Collectors.toSet());
        article.setTags(tags);
    }

    public ArticleDto toDto(KnowledgeArticle a) {
        return ArticleDto.builder()
                .id(a.getId())
                .tenantId(a.getTenantId())
                .title(a.getTitle())
                .content(a.getContent())
                .summary(a.getSummary())
                .status(a.getStatus())
                .category(a.getCategory() != null ? CategoryDto.builder()
                        .id(a.getCategory().getId())
                        .name(a.getCategory().getName())
                        .description(a.getCategory().getDescription())
                        .parentId(a.getCategory().getParentId())
                        .orderIndex(a.getCategory().getOrderIndex())
                        .build() : null)
                .tags(a.getTags() != null ? a.getTags().stream()
                        .map(t -> TagDto.builder().id(t.getId()).name(t.getName()).build())
                        .sorted(Comparator.comparing(TagDto::getName))
                        .toList() : List.of())
                .createdBy(a.getCreatedBy())
                .updatedBy(a.getUpdatedBy())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .version(a.getVersion())
                .publicWithinTenant(a.isPublicWithinTenant())
                .linkedTaskId(a.getLinkedTaskId())
                .viewCount(a.getViewCount())
                .usageCount(a.getUsageCount())
                .averageRating(a.getAverageRating())
                .ratingCount(a.getRatingCount())
                .lastUsedAt(a.getLastUsedAt())
                .build();
    }
}
