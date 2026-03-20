package de.wissensmanagement.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FeedbackRequest {
    @Min(1) @Max(5)
    private int rating;
    private String comment;
}
