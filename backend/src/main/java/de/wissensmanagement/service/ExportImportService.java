package de.wissensmanagement.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.wissensmanagement.entity.*;
import de.wissensmanagement.enums.ArticleStatus;
import de.wissensmanagement.enums.PromptType;
import de.wissensmanagement.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExportImportService {

    private static final Logger log = LoggerFactory.getLogger(ExportImportService.class);

    private final KnowledgeArticleRepository articleRepo;
    private final KnowledgeCategoryRepository categoryRepo;
    private final KnowledgeTagRepository tagRepo;
    private final KnowledgeGroupingRepository groupingRepo;
    private final PromptConfigRepository promptRepo;
    private final ArticleVersionRepository versionRepo;
    private final KnowledgeFeedbackRepository feedbackRepo;
    private final ChatSessionRepository chatSessionRepo;
    private final ChatMessageRepository chatMessageRepo;

    public ExportImportService(KnowledgeArticleRepository articleRepo,
                               KnowledgeCategoryRepository categoryRepo,
                               KnowledgeTagRepository tagRepo,
                               KnowledgeGroupingRepository groupingRepo,
                               PromptConfigRepository promptRepo,
                               ArticleVersionRepository versionRepo,
                               KnowledgeFeedbackRepository feedbackRepo,
                               ChatSessionRepository chatSessionRepo,
                               ChatMessageRepository chatMessageRepo) {
        this.articleRepo = articleRepo;
        this.categoryRepo = categoryRepo;
        this.tagRepo = tagRepo;
        this.groupingRepo = groupingRepo;
        this.promptRepo = promptRepo;
        this.versionRepo = versionRepo;
        this.feedbackRepo = feedbackRepo;
        this.chatSessionRepo = chatSessionRepo;
        this.chatMessageRepo = chatMessageRepo;
    }

    @Transactional(readOnly = true)
    public ExportData exportAll(String tenantId) {
        ExportData data = new ExportData();
        data.exportedAt = LocalDateTime.now().toString();
        data.tenantId = tenantId;

        // Categories
        data.categories = categoryRepo.findByTenantIdOrderByOrderIndexAsc(tenantId).stream()
                .map(c -> {
                    CategoryExport ce = new CategoryExport();
                    ce.id = c.getId();
                    ce.name = c.getName();
                    ce.description = c.getDescription();
                    ce.parentId = c.getParentId();
                    ce.orderIndex = c.getOrderIndex();
                    return ce;
                }).toList();

        // Tags
        data.tags = tagRepo.findByTenantIdOrderByNameAsc(tenantId).stream()
                .map(t -> {
                    TagExport te = new TagExport();
                    te.id = t.getId();
                    te.name = t.getName();
                    return te;
                }).toList();

        // Groupings
        data.groupings = groupingRepo.findByTenantIdOrderByNameAsc(tenantId).stream()
                .map(g -> {
                    GroupingExport ge = new GroupingExport();
                    ge.id = g.getId();
                    ge.name = g.getName();
                    ge.description = g.getDescription();
                    return ge;
                }).toList();

        // Prompts
        data.prompts = promptRepo.findByTenantIdOrderByNameAsc(tenantId).stream()
                .map(p -> {
                    PromptExport pe = new PromptExport();
                    pe.id = p.getId();
                    pe.name = p.getName();
                    pe.description = p.getDescription();
                    pe.promptText = p.getPromptText();
                    pe.promptType = p.getPromptType().name();
                    return pe;
                }).toList();

        // Articles (without lazy-loaded children/parent to avoid circular refs)
        List<KnowledgeArticle> articles = articleRepo.findByTenantIdAndParentArticleIdIsNullOrderBySortOrderAsc(tenantId);
        // Get all articles via pageable to include sub-articles too
        List<KnowledgeArticle> allArticles = articleRepo.findByTenantId(tenantId,
                org.springframework.data.domain.Pageable.unpaged()).getContent();

        data.articles = allArticles.stream().map(a -> {
            ArticleExport ae = new ArticleExport();
            ae.id = a.getId();
            ae.title = a.getTitle();
            ae.content = a.getContent();
            ae.summary = a.getSummary();
            ae.status = a.getStatus().name();
            ae.parentArticleId = a.getParentArticleId();
            ae.sortOrder = a.getSortOrder();
            ae.treePath = a.getTreePath();
            ae.depth = a.getDepth();
            ae.version = a.getVersion();
            ae.createdBy = a.getCreatedBy();
            ae.updatedBy = a.getUpdatedBy();
            ae.createdAt = a.getCreatedAt() != null ? a.getCreatedAt().toString() : null;
            ae.updatedAt = a.getUpdatedAt() != null ? a.getUpdatedAt().toString() : null;
            ae.categoryId = a.getCategory() != null ? a.getCategory().getId() : null;
            ae.groupingId = a.getGrouping() != null ? a.getGrouping().getId() : null;
            ae.tagIds = a.getTags().stream().map(KnowledgeTag::getId).toList();
            ae.viewCount = a.getViewCount();
            ae.ratingSum = a.getRatingSum();
            ae.ratingCount = a.getRatingCount();

            // Versions
            ae.versions = versionRepo.findByArticleIdOrderByVersionDesc(a.getId()).stream()
                    .map(v -> {
                        VersionExport ve = new VersionExport();
                        ve.version = v.getVersion();
                        ve.title = v.getTitle();
                        ve.content = v.getContent();
                        ve.summary = v.getSummary();
                        ve.changedBy = v.getChangedBy();
                        ve.changedAt = v.getChangedAt() != null ? v.getChangedAt().toString() : null;
                        ve.changeNote = v.getChangeNote();
                        return ve;
                    }).toList();

            return ae;
        }).toList();

        return data;
    }

    @Transactional
    public ImportResult importAll(String tenantId, ExportData data) {
        ImportResult result = new ImportResult();
        Map<String, String> categoryIdMap = new HashMap<>();
        Map<String, String> tagIdMap = new HashMap<>();
        Map<String, String> groupingIdMap = new HashMap<>();

        // 1. Import Categories
        if (data.categories != null) {
            for (CategoryExport ce : data.categories) {
                Optional<KnowledgeCategory> existing = categoryRepo.findByTenantIdAndName(tenantId, ce.name);
                if (existing.isPresent()) {
                    categoryIdMap.put(ce.id, existing.get().getId());
                    result.categoriesSkipped++;
                } else {
                    KnowledgeCategory cat = new KnowledgeCategory();
                    cat.setTenantId(tenantId);
                    cat.setName(ce.name);
                    cat.setDescription(ce.description);
                    cat.setOrderIndex(ce.orderIndex);
                    cat = categoryRepo.save(cat);
                    categoryIdMap.put(ce.id, cat.getId());
                    result.categoriesImported++;
                }
            }
            // Resolve parent IDs for categories
            if (data.categories != null) {
                for (CategoryExport ce : data.categories) {
                    if (ce.parentId != null && categoryIdMap.containsKey(ce.parentId)) {
                        String newId = categoryIdMap.get(ce.id);
                        categoryRepo.findById(newId).ifPresent(cat -> {
                            cat.setParentId(categoryIdMap.get(ce.parentId));
                            categoryRepo.save(cat);
                        });
                    }
                }
            }
        }

        // 2. Import Tags
        if (data.tags != null) {
            for (TagExport te : data.tags) {
                Optional<KnowledgeTag> existing = tagRepo.findByTenantIdAndName(tenantId, te.name);
                if (existing.isPresent()) {
                    tagIdMap.put(te.id, existing.get().getId());
                    result.tagsSkipped++;
                } else {
                    KnowledgeTag tag = new KnowledgeTag();
                    tag.setTenantId(tenantId);
                    tag.setName(te.name);
                    tag = tagRepo.save(tag);
                    tagIdMap.put(te.id, tag.getId());
                    result.tagsImported++;
                }
            }
        }

        // 3. Import Groupings
        if (data.groupings != null) {
            for (GroupingExport ge : data.groupings) {
                Optional<KnowledgeGrouping> existing = groupingRepo.findByTenantIdAndName(tenantId, ge.name);
                if (existing.isPresent()) {
                    groupingIdMap.put(ge.id, existing.get().getId());
                    result.groupingsSkipped++;
                } else {
                    KnowledgeGrouping grp = new KnowledgeGrouping();
                    grp.setTenantId(tenantId);
                    grp.setName(ge.name);
                    grp.setDescription(ge.description);
                    grp = groupingRepo.save(grp);
                    groupingIdMap.put(ge.id, grp.getId());
                    result.groupingsImported++;
                }
            }
        }

        // 4. Import Prompts
        if (data.prompts != null) {
            for (PromptExport pe : data.prompts) {
                PromptConfig prompt = new PromptConfig();
                prompt.setTenantId(tenantId);
                prompt.setName(pe.name);
                prompt.setDescription(pe.description);
                prompt.setPromptText(pe.promptText);
                prompt.setPromptType(PromptType.valueOf(pe.promptType));
                promptRepo.save(prompt);
                result.promptsImported++;
            }
        }

        // 5. Import Articles (sorted by depth so parents come first)
        Map<String, String> articleIdMap = new HashMap<>();
        if (data.articles != null) {
            List<ArticleExport> sorted = data.articles.stream()
                    .sorted(Comparator.comparingInt(a -> a.depth))
                    .toList();

            for (ArticleExport ae : sorted) {
                KnowledgeArticle article = new KnowledgeArticle();
                article.setTenantId(tenantId);
                article.setTitle(ae.title);
                article.setContent(ae.content);
                article.setSummary(ae.summary);
                article.setStatus(ArticleStatus.valueOf(ae.status));
                article.setSortOrder(ae.sortOrder);
                article.setDepth(ae.depth);
                article.setVersion(ae.version);
                article.setCreatedBy(ae.createdBy);
                article.setUpdatedBy(ae.updatedBy);
                if (ae.createdAt != null) article.setCreatedAt(LocalDateTime.parse(ae.createdAt));
                if (ae.updatedAt != null) article.setUpdatedAt(LocalDateTime.parse(ae.updatedAt));
                article.setViewCount(ae.viewCount);
                article.setRatingSum(ae.ratingSum);
                article.setRatingCount(ae.ratingCount);

                // Resolve parent
                if (ae.parentArticleId != null && articleIdMap.containsKey(ae.parentArticleId)) {
                    article.setParentArticleId(articleIdMap.get(ae.parentArticleId));
                }

                // Resolve category
                if (ae.categoryId != null && categoryIdMap.containsKey(ae.categoryId)) {
                    categoryRepo.findById(categoryIdMap.get(ae.categoryId))
                            .ifPresent(article::setCategory);
                }

                // Resolve grouping
                if (ae.groupingId != null && groupingIdMap.containsKey(ae.groupingId)) {
                    groupingRepo.findById(groupingIdMap.get(ae.groupingId))
                            .ifPresent(article::setGrouping);
                }

                // Resolve tags
                if (ae.tagIds != null) {
                    Set<KnowledgeTag> resolvedTags = ae.tagIds.stream()
                            .filter(tagIdMap::containsKey)
                            .map(tagIdMap::get)
                            .map(tagRepo::findById)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .collect(Collectors.toSet());
                    article.setTags(resolvedTags);
                }

                article = articleRepo.save(article);
                articleIdMap.put(ae.id, article.getId());

                // Update treePath
                if (article.getParentArticleId() != null) {
                    article.setTreePath(article.getParentArticleId() + "/" + article.getId());
                } else {
                    article.setTreePath(article.getId());
                }
                articleRepo.save(article);

                // Import versions
                if (ae.versions != null) {
                    for (VersionExport ve : ae.versions) {
                        ArticleVersion version = new ArticleVersion();
                        version.setArticleId(article.getId());
                        version.setVersion(ve.version);
                        version.setTitle(ve.title);
                        version.setContent(ve.content);
                        version.setSummary(ve.summary);
                        version.setChangedBy(ve.changedBy);
                        if (ve.changedAt != null) version.setChangedAt(LocalDateTime.parse(ve.changedAt));
                        version.setChangeNote(ve.changeNote);
                        versionRepo.save(version);
                    }
                }

                result.articlesImported++;
            }
        }

        log.info("Import abgeschlossen fuer Mandant {}: {} Artikel, {} Kategorien, {} Tags, {} Gruppierungen, {} Prompts",
                tenantId, result.articlesImported, result.categoriesImported, result.tagsImported,
                result.groupingsImported, result.promptsImported);

        return result;
    }

    // --- Export DTOs ---

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ExportData {
        public String exportedAt;
        public String tenantId;
        public List<CategoryExport> categories;
        public List<TagExport> tags;
        public List<GroupingExport> groupings;
        public List<PromptExport> prompts;
        public List<ArticleExport> articles;
    }

    public static class CategoryExport {
        public String id;
        public String name;
        public String description;
        public String parentId;
        public int orderIndex;
    }

    public static class TagExport {
        public String id;
        public String name;
    }

    public static class GroupingExport {
        public String id;
        public String name;
        public String description;
    }

    public static class PromptExport {
        public String id;
        public String name;
        public String description;
        public String promptText;
        public String promptType;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ArticleExport {
        public String id;
        public String title;
        public String content;
        public String summary;
        public String status;
        public String parentArticleId;
        public int sortOrder;
        public String treePath;
        public int depth;
        public int version;
        public String createdBy;
        public String updatedBy;
        public String createdAt;
        public String updatedAt;
        public String categoryId;
        public String groupingId;
        public List<String> tagIds;
        public int viewCount;
        public double ratingSum;
        public int ratingCount;
        public List<VersionExport> versions;
    }

    public static class VersionExport {
        public int version;
        public String title;
        public String content;
        public String summary;
        public String changedBy;
        public String changedAt;
        public String changeNote;
    }

    public static class ImportResult {
        public int articlesImported;
        public int categoriesImported;
        public int categoriesSkipped;
        public int tagsImported;
        public int tagsSkipped;
        public int groupingsImported;
        public int groupingsSkipped;
        public int promptsImported;
    }
}
