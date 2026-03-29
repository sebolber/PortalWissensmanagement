package de.wissensmanagement.service;

import de.wissensmanagement.dto.ArticleDto;
import de.wissensmanagement.entity.KnowledgeArticle;
import de.wissensmanagement.repository.KnowledgeArticleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class HierarchyService {

    private final KnowledgeArticleRepository articleRepo;

    public HierarchyService(KnowledgeArticleRepository articleRepo) {
        this.articleRepo = articleRepo;
    }

    /**
     * Returns the full article tree for a tenant (root articles with nested children).
     */
    public List<ArticleTreeNode> getArticleTree(String tenantId) {
        List<KnowledgeArticle> allArticles = articleRepo.findByTenantId(tenantId,
                org.springframework.data.domain.Pageable.unpaged()).getContent();

        Map<String, List<KnowledgeArticle>> childrenMap = allArticles.stream()
                .filter(a -> a.getParentArticleId() != null)
                .collect(Collectors.groupingBy(KnowledgeArticle::getParentArticleId));

        List<KnowledgeArticle> roots = allArticles.stream()
                .filter(KnowledgeArticle::isRoot)
                .sorted(Comparator.comparingInt(KnowledgeArticle::getSortOrder))
                .toList();

        return roots.stream()
                .map(root -> buildTreeNode(root, childrenMap))
                .toList();
    }

    private ArticleTreeNode buildTreeNode(KnowledgeArticle article, Map<String, List<KnowledgeArticle>> childrenMap) {
        List<KnowledgeArticle> children = childrenMap.getOrDefault(article.getId(), List.of());
        List<ArticleTreeNode> childNodes = children.stream()
                .sorted(Comparator.comparingInt(KnowledgeArticle::getSortOrder))
                .map(child -> buildTreeNode(child, childrenMap))
                .toList();

        return new ArticleTreeNode(
                article.getId(),
                article.getTitle(),
                article.getStatus().name(),
                article.getParentArticleId(),
                article.getSortOrder(),
                article.getDepth(),
                childNodes.size() + childNodes.stream().mapToInt(ArticleTreeNode::totalDescendants).sum(),
                childNodes
        );
    }

    /**
     * Returns the direct children of an article.
     */
    public List<KnowledgeArticle> getChildren(String tenantId, String parentArticleId) {
        return articleRepo.findByTenantIdAndParentArticleIdOrderBySortOrderAsc(tenantId, parentArticleId);
    }

    /**
     * Returns the root articles of a tenant.
     */
    public List<KnowledgeArticle> getRootArticles(String tenantId) {
        return articleRepo.findByTenantIdAndParentArticleIdIsNullOrderBySortOrderAsc(tenantId);
    }

    /**
     * Returns the breadcrumb path from root to the given article.
     */
    public List<ArticleDto.BreadcrumbItem> getBreadcrumb(String tenantId, String articleId) {
        List<ArticleDto.BreadcrumbItem> breadcrumb = new ArrayList<>();
        String currentId = articleId;
        Set<String> visited = new HashSet<>();

        while (currentId != null) {
            if (visited.contains(currentId)) break; // cycle protection
            visited.add(currentId);

            KnowledgeArticle article = articleRepo.findByIdAndTenantId(currentId, tenantId)
                    .orElse(null);
            if (article == null) break;

            breadcrumb.add(0, ArticleDto.BreadcrumbItem.builder()
                    .id(article.getId())
                    .title(article.getTitle())
                    .build());

            currentId = article.getParentArticleId();
        }
        return breadcrumb;
    }

    /**
     * Moves an article to a new parent (or to root if newParentId is null).
     * Recalculates treePath and depth for moved article and all descendants.
     */
    @Transactional
    public void moveArticle(String tenantId, String articleId, String newParentId) {
        KnowledgeArticle article = articleRepo.findByIdAndTenantId(articleId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Artikel nicht gefunden: " + articleId));

        // Prevent moving to self or to own descendant
        if (articleId.equals(newParentId)) {
            throw new IllegalArgumentException("Artikel kann nicht in sich selbst verschoben werden.");
        }
        if (newParentId != null) {
            KnowledgeArticle newParent = articleRepo.findByIdAndTenantId(newParentId, tenantId)
                    .orElseThrow(() -> new EntityNotFoundException("Zielartikel nicht gefunden: " + newParentId));
            if (newParent.getTreePath() != null && newParent.getTreePath().contains("/" + articleId + "/")) {
                throw new IllegalArgumentException("Artikel kann nicht in einen eigenen Unterartikel verschoben werden.");
            }
        }

        // Calculate new sort order (append at end)
        int newSortOrder;
        if (newParentId == null) {
            Integer maxSort = articleRepo.findMaxRootSortOrder(tenantId);
            newSortOrder = (maxSort != null ? maxSort : 0) + 1;
        } else {
            Integer maxSort = articleRepo.findMaxSortOrder(tenantId, newParentId);
            newSortOrder = (maxSort != null ? maxSort : 0) + 1;
        }

        article.setParentArticleId(newParentId);
        article.setSortOrder(newSortOrder);
        articleRepo.save(article);

        // Recalculate paths for moved subtree
        recalculatePaths(tenantId, article);
    }

    /**
     * Reorders articles within the same parent.
     */
    @Transactional
    public void reorderArticles(String tenantId, String parentArticleId, List<String> orderedIds) {
        for (int i = 0; i < orderedIds.size(); i++) {
            KnowledgeArticle article = articleRepo.findByIdAndTenantId(orderedIds.get(i), tenantId)
                    .orElseThrow(() -> new EntityNotFoundException("Artikel nicht gefunden"));
            article.setSortOrder(i);
            articleRepo.save(article);
        }
    }

    /**
     * Sets up hierarchy fields for a newly created article.
     */
    @Transactional
    public void initializeHierarchy(KnowledgeArticle article) {
        String tenantId = article.getTenantId();

        if (article.getParentArticleId() != null) {
            KnowledgeArticle parent = articleRepo.findByIdAndTenantId(article.getParentArticleId(), tenantId)
                    .orElseThrow(() -> new EntityNotFoundException("Parent-Artikel nicht gefunden"));

            article.setDepth(parent.getDepth() + 1);
            article.setTreePath(parent.getTreePath() + article.getId() + "/");

            Integer maxSort = articleRepo.findMaxSortOrder(tenantId, article.getParentArticleId());
            article.setSortOrder(maxSort != null ? maxSort + 1 : 0);
        } else {
            article.setDepth(0);
            article.setTreePath("/" + article.getId() + "/");

            Integer maxSort = articleRepo.findMaxRootSortOrder(tenantId);
            article.setSortOrder(maxSort != null ? maxSort + 1 : 0);
        }

        articleRepo.save(article);
    }

    /**
     * Recalculates treePath and depth for an article and all its descendants.
     */
    private void recalculatePaths(String tenantId, KnowledgeArticle article) {
        if (article.getParentArticleId() != null) {
            KnowledgeArticle parent = articleRepo.findByIdAndTenantId(article.getParentArticleId(), tenantId).orElse(null);
            if (parent != null) {
                article.setDepth(parent.getDepth() + 1);
                article.setTreePath(parent.getTreePath() + article.getId() + "/");
            }
        } else {
            article.setDepth(0);
            article.setTreePath("/" + article.getId() + "/");
        }
        articleRepo.save(article);

        // Recursively update children
        List<KnowledgeArticle> children = articleRepo.findByTenantIdAndParentArticleIdOrderBySortOrderAsc(tenantId, article.getId());
        for (KnowledgeArticle child : children) {
            recalculatePaths(tenantId, child);
        }
    }

    public record ArticleTreeNode(
            String id,
            String title,
            String status,
            String parentArticleId,
            int sortOrder,
            int depth,
            int totalDescendants,
            List<ArticleTreeNode> children
    ) {}
}
