package org.example.gestiondeslieux.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PlaceSearchRequest {
    @Schema(example = "cafe")
    private String q;
    @Schema(example = "paris")
    private String tag;
    @Schema(example = "48.8566")
    private Double lat;
    @Schema(example = "2.3522")
    private Double lon;
    @Schema(example = "5.0")
    private Double radiusKm = 5.0;
}
