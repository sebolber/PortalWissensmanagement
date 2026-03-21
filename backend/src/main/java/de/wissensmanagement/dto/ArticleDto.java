package de.wissensmanagement.dto;

import de.wissensmanagement.enums.ArticleStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ArticleDto {
    private String id;
    private String tenantId;
    private String title;
    private String content;
    private String summary;
    private ArticleStatus status;

    // Hierarchy
    private String parentArticleId;
    private int sortOrder;
    private String treePath;
    private int depth;
    private List<ArticleDto> children;
    private List<BreadcrumbItem> breadcrumb;
    private int childCount;

    private CategoryDto category;
    private List<TagDto> tags;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int version;
    private boolean publicWithinTenant;
    private String linkedTaskId;
    private int viewCount;
    private int usageCount;
    private double averageRating;
    private int ratingCount;
    private LocalDateTime lastUsedAt;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class BreadcrumbItem {
        private String id;
        private String title;
    }
}
