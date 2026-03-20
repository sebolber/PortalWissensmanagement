package de.wissensmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "wm_suggestions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KnowledgeSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "context_type", nullable = false, length = 50)
    private String contextType;

    @Column(name = "context_ref_id", nullable = false)
    private String contextRefId;

    @Column(name = "article_id", nullable = false)
    private String articleId;

    private Double confidence;

    @Column(length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
