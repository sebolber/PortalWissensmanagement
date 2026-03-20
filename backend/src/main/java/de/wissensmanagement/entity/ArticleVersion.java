package de.wissensmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "wm_article_versions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ArticleVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "article_id", nullable = false)
    private String articleId;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 2000)
    private String summary;

    @Column(name = "changed_by", nullable = false)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "change_note", length = 500)
    private String changeNote;

    @PrePersist
    void onCreate() {
        if (changedAt == null) changedAt = LocalDateTime.now();
    }
}
