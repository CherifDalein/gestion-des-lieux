package org.example.gestiondeslieux.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CollectionDto {
    private Long id;
    private String name;
    private String tagFilter;
    private Boolean isShared;
    private LocalDateTime createdAt;
    private long placeCount;
    private String username;
}
