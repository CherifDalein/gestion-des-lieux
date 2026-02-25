package org.example.gestiondeslieux.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PlaceWithDistanceDto extends PlaceDto {
    private Double distanceKm;
}
