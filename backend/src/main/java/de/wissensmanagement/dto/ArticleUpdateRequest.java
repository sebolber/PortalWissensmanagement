package de.wissensmanagement.dto;

import de.wissensmanagement.enums.ArticleType;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ArticleUpdateRequest {
    @NotBlank(message = "Titel ist erforderlich")
    private String title;

    @NotBlank(message = "Inhalt ist erforderlich")
    private String content;

    private String summary;
    private String categoryId;
    private String groupingId;
    private List<String> tagNames;
    private boolean publicWithinTenant = true;
    private String linkedTaskId;
    private String changeNote;
    private ArticleType articleType;
    private String productVersion;
    private String productVendor;
    private String productIconUrl;
    private String productDocumentationUrl;
}
