package org.example.gestiondeslieux.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PlaceWithDistanceDto extends PlaceDto {
    @Schema(example = "1.4")
    private Double distanceKm;
}
