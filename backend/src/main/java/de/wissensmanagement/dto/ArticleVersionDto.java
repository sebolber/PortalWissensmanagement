package de.wissensmanagement.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ArticleVersionDto {
    private String id;
    private int version;
    private String title;
    private String content;
    private String summary;
    private String changedBy;
    private LocalDateTime changedAt;
    private String changeNote;
}
