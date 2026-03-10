package org.example.gestiondeslieux.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
public class CollectionDto extends RepresentationModel<CollectionDto> {
    @Schema(example = "3")
    private Long id;
    @Schema(example = "Lieux favoris")
    private String name;
    @Schema(example = "paris")
    private String tagFilter;
    @Schema(example = "false")
    private Boolean isShared;
    @Schema(example = "2026-02-25T22:30:00")
    private LocalDateTime createdAt;
    @Schema(example = "12")
    private long placeCount;
    @Schema(example = "alice@test.com")
    private String email;
}
