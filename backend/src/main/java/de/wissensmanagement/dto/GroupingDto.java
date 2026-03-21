package de.wissensmanagement.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GroupingDto {
    private String id;
    private String name;
    private String description;
}
