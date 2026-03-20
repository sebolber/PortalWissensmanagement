package de.wissensmanagement.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CategoryDto {
    private String id;
    private String name;
    private String description;
    private String parentId;
    private int orderIndex;
}
