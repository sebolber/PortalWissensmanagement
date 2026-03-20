package de.wissensmanagement.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StatistikDto {
    private long gesamt;
    private long veroeffentlicht;
    private long entwuerfe;
    private long kategorien;
}
