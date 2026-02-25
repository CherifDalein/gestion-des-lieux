package org.example.gestiondeslieux.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
public class CollectionDto extends RepresentationModel<CollectionDto> {
    private Long id;
    private String name;
    private String tagFilter;
    private Boolean isShared;
    private LocalDateTime createdAt;
    private long placeCount;
    private String username;
}
