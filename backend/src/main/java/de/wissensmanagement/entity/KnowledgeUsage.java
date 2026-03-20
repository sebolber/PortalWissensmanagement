package de.wissensmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "wm_usage")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KnowledgeUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "article_id", nullable = false)
    private String articleId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "context_type", nullable = false, length = 50)
    private String contextType;

    @Column(name = "context_reference_id")
    private String contextReferenceId;

    @Column(name = "used_by", nullable = false)
    private String usedBy;

    @Column(name = "used_at", nullable = false)
    private LocalDateTime usedAt;

    @PrePersist
    void onCreate() {
        if (usedAt == null) usedAt = LocalDateTime.now();
    }
}
