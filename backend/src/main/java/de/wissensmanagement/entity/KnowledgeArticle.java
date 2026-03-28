package de.wissensmanagement.entity;

import de.wissensmanagement.enums.ArticleStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "wm_articles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KnowledgeArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 2000)
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ArticleStatus status = ArticleStatus.DRAFT;

    // --- Hierarchy fields ---
    @Column(name = "parent_article_id")
    private String parentArticleId;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    @Column(name = "tree_path", length = 4000)
    private String treePath;

    @Column(name = "depth", nullable = false)
    @Builder.Default
    private int depth = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_article_id", insertable = false, updatable = false)
    private KnowledgeArticle parentArticle;

    @OneToMany(mappedBy = "parentArticle", fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<KnowledgeArticle> children = new ArrayList<>();

    // --- Category and Tags ---
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private KnowledgeCategory category;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "grouping_id")
    private KnowledgeGrouping grouping;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "wm_article_tags",
        joinColumns = @JoinColumn(name = "article_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private Set<KnowledgeTag> tags = new HashSet<>();

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @Builder.Default
    private int version = 1;

    @Column(name = "is_public_within_tenant", nullable = false)
    @Builder.Default
    private boolean publicWithinTenant = true;

    @Column(name = "organization_unit_id")
    private String organizationUnitId;

    @Column(name = "linked_task_id")
    private String linkedTaskId;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private int viewCount = 0;

    @Column(name = "usage_count", nullable = false)
    @Builder.Default
    private int usageCount = 0;

    @Column(name = "rating_sum", nullable = false)
    @Builder.Default
    private double ratingSum = 0;

    @Column(name = "rating_count", nullable = false)
    @Builder.Default
    private int ratingCount = 0;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    public double getAverageRating() {
        return ratingCount > 0 ? ratingSum / ratingCount : 0;
    }

    public boolean isRoot() {
        return parentArticleId == null;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
