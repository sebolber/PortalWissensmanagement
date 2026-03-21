package de.wissensmanagement.service;

import de.wissensmanagement.dto.ArticleDto;
import de.wissensmanagement.entity.KnowledgeArticle;
import de.wissensmanagement.enums.ArticleStatus;
import de.wissensmanagement.repository.KnowledgeArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HierarchyServiceTest {

    @Mock
    private KnowledgeArticleRepository articleRepo;

    @InjectMocks
    private HierarchyService hierarchyService;

    private static final String TENANT_ID = "tenant-1";

    private KnowledgeArticle createArticle(String id, String title, String parentId, int depth) {
        KnowledgeArticle article = KnowledgeArticle.builder()
                .tenantId(TENANT_ID)
                .title(title)
                .content("Content for " + title)
                .status(ArticleStatus.PUBLISHED)
                .parentArticleId(parentId)
                .depth(depth)
                .sortOrder(0)
                .treePath(parentId == null ? "/" + id + "/" : "/" + parentId + "/" + id + "/")
                .createdBy("user-1")
                .createdAt(LocalDateTime.now())
                .build();
        // Use reflection or setter since @Builder won't set id
        article.setId(id);
        return article;
    }

    @Test
    void getArticleTree_returnsHierarchicalStructure() {
        KnowledgeArticle root1 = createArticle("root-1", "Root 1", null, 0);
        KnowledgeArticle root2 = createArticle("root-2", "Root 2", null, 0);
        KnowledgeArticle child1 = createArticle("child-1", "Child 1", "root-1", 1);
        KnowledgeArticle grandchild1 = createArticle("gc-1", "Grandchild 1", "child-1", 2);

        List<KnowledgeArticle> all = List.of(root1, root2, child1, grandchild1);
        when(articleRepo.findByTenantId(eq(TENANT_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(all));

        List<HierarchyService.ArticleTreeNode> tree = hierarchyService.getArticleTree(TENANT_ID);

        assertEquals(2, tree.size(), "Should have 2 root nodes");
        assertEquals("Root 1", tree.get(0).title());
        assertEquals(1, tree.get(0).children().size(), "Root 1 should have 1 child");
        assertEquals("Child 1", tree.get(0).children().get(0).title());
        assertEquals(1, tree.get(0).children().get(0).children().size(), "Child 1 should have 1 grandchild");
    }

    @Test
    void getBreadcrumb_returnsPathFromRootToArticle() {
        KnowledgeArticle root = createArticle("root-1", "Root", null, 0);
        KnowledgeArticle child = createArticle("child-1", "Child", "root-1", 1);
        KnowledgeArticle grandchild = createArticle("gc-1", "Grandchild", "child-1", 2);

        when(articleRepo.findByIdAndTenantId("gc-1", TENANT_ID)).thenReturn(Optional.of(grandchild));
        when(articleRepo.findByIdAndTenantId("child-1", TENANT_ID)).thenReturn(Optional.of(child));
        when(articleRepo.findByIdAndTenantId("root-1", TENANT_ID)).thenReturn(Optional.of(root));

        List<ArticleDto.BreadcrumbItem> breadcrumb = hierarchyService.getBreadcrumb(TENANT_ID, "gc-1");

        assertEquals(3, breadcrumb.size());
        assertEquals("Root", breadcrumb.get(0).getTitle());
        assertEquals("Child", breadcrumb.get(1).getTitle());
        assertEquals("Grandchild", breadcrumb.get(2).getTitle());
    }

    @Test
    void getBreadcrumb_handlesRootArticle() {
        KnowledgeArticle root = createArticle("root-1", "Root", null, 0);
        when(articleRepo.findByIdAndTenantId("root-1", TENANT_ID)).thenReturn(Optional.of(root));

        List<ArticleDto.BreadcrumbItem> breadcrumb = hierarchyService.getBreadcrumb(TENANT_ID, "root-1");

        assertEquals(1, breadcrumb.size());
        assertEquals("Root", breadcrumb.get(0).getTitle());
    }

    @Test
    void getBreadcrumb_preventsCycles() {
        KnowledgeArticle a = createArticle("a", "A", "b", 1);
        KnowledgeArticle b = createArticle("b", "B", "a", 1); // cycle!

        when(articleRepo.findByIdAndTenantId("a", TENANT_ID)).thenReturn(Optional.of(a));
        when(articleRepo.findByIdAndTenantId("b", TENANT_ID)).thenReturn(Optional.of(b));

        List<ArticleDto.BreadcrumbItem> breadcrumb = hierarchyService.getBreadcrumb(TENANT_ID, "a");

        // Should terminate and not infinite loop
        assertTrue(breadcrumb.size() <= 2);
    }

    @Test
    void moveArticle_preventsMovingToSelf() {
        KnowledgeArticle article = createArticle("art-1", "Article", null, 0);
        when(articleRepo.findByIdAndTenantId("art-1", TENANT_ID)).thenReturn(Optional.of(article));

        assertThrows(IllegalArgumentException.class,
                () -> hierarchyService.moveArticle(TENANT_ID, "art-1", "art-1"));
    }

    @Test
    void moveArticle_preventsMovingToOwnDescendant() {
        KnowledgeArticle parent = createArticle("parent", "Parent", null, 0);
        parent.setTreePath("/parent/");
        KnowledgeArticle child = createArticle("child", "Child", "parent", 1);
        child.setTreePath("/parent/child/");

        when(articleRepo.findByIdAndTenantId("parent", TENANT_ID)).thenReturn(Optional.of(parent));
        when(articleRepo.findByIdAndTenantId("child", TENANT_ID)).thenReturn(Optional.of(child));

        assertThrows(IllegalArgumentException.class,
                () -> hierarchyService.moveArticle(TENANT_ID, "parent", "child"));
    }

    @Test
    void initializeHierarchy_setsCorrectPathForRoot() {
        KnowledgeArticle article = createArticle("art-1", "Article", null, 0);
        article.setTreePath(null);
        article.setDepth(-1);

        when(articleRepo.findMaxRootSortOrder(TENANT_ID)).thenReturn(5);
        when(articleRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        hierarchyService.initializeHierarchy(article);

        assertEquals(0, article.getDepth());
        assertEquals("/art-1/", article.getTreePath());
        assertEquals(6, article.getSortOrder());
    }

    @Test
    void initializeHierarchy_setsCorrectPathForChild() {
        KnowledgeArticle parent = createArticle("parent", "Parent", null, 0);
        parent.setTreePath("/parent/");
        parent.setDepth(0);

        KnowledgeArticle child = createArticle("child", "Child", "parent", 0);
        child.setTreePath(null);

        when(articleRepo.findByIdAndTenantId("parent", TENANT_ID)).thenReturn(Optional.of(parent));
        when(articleRepo.findMaxSortOrder(TENANT_ID, "parent")).thenReturn(2);
        when(articleRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        hierarchyService.initializeHierarchy(child);

        assertEquals(1, child.getDepth());
        assertEquals("/parent/child/", child.getTreePath());
        assertEquals(3, child.getSortOrder());
    }

    @Test
    void reorderArticles_updatesAllSortOrders() {
        KnowledgeArticle a = createArticle("a", "A", "parent", 1);
        KnowledgeArticle b = createArticle("b", "B", "parent", 1);
        KnowledgeArticle c = createArticle("c", "C", "parent", 1);

        when(articleRepo.findByIdAndTenantId(eq("a"), eq(TENANT_ID))).thenReturn(Optional.of(a));
        when(articleRepo.findByIdAndTenantId(eq("b"), eq(TENANT_ID))).thenReturn(Optional.of(b));
        when(articleRepo.findByIdAndTenantId(eq("c"), eq(TENANT_ID))).thenReturn(Optional.of(c));
        when(articleRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        hierarchyService.reorderArticles(TENANT_ID, "parent", List.of("c", "a", "b"));

        assertEquals(1, a.getSortOrder());
        assertEquals(2, b.getSortOrder());
        assertEquals(0, c.getSortOrder());
    }

    // --- Tenant isolation tests ---

    @Test
    void getArticleTree_onlyReturnsTenantArticles() {
        // Verifies the query is called with the correct tenantId
        when(articleRepo.findByTenantId(eq("tenant-A"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        hierarchyService.getArticleTree("tenant-A");

        verify(articleRepo).findByTenantId(eq("tenant-A"), any(Pageable.class));
        verify(articleRepo, never()).findByTenantId(eq("tenant-B"), any());
    }

    @Test
    void getBreadcrumb_respectsTenantBoundary() {
        when(articleRepo.findByIdAndTenantId("art-1", "tenant-A")).thenReturn(Optional.empty());

        List<ArticleDto.BreadcrumbItem> breadcrumb = hierarchyService.getBreadcrumb("tenant-A", "art-1");

        assertTrue(breadcrumb.isEmpty());
    }
}
