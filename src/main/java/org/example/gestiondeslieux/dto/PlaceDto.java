package org.example.gestiondeslieux.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class PlaceDto extends RepresentationModel<PlaceDto> {
    @Schema(example = "10")
    private Long id;
    @Schema(example = "Tour Eiffel")
    private String title;
    @Schema(example = "Monument emblématique de Paris")
    private String description;
    @Schema(example = "48.8584")
    private Double latitude;
    @Schema(example = "2.2945")
    private Double longitude;
    @Schema(example = "/api/images/25")
    private String imageUrl;
    @Schema(example = "[\"paris\",\"tourisme\"]")
    private List<String> tags;
    @Schema(example = "2026-02-25T22:30:00")
    private LocalDateTime createdAt;
    @Schema(example = "2026-02-25T22:45:00")
    private LocalDateTime updatedAt;
    @Schema(example = "alice@test.com")
    private String email;
    @Schema(example = "2")
    private int collectionCount;
}
