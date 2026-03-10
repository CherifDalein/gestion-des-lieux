package org.example.gestiondeslieux.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class UpdatePlaceRequest {

    @Size(min = 1, max = 200)
    @Schema(example = "Tour Eiffel de nuit")
    private String title;

    @Size(max = 2000)
    @Schema(example = "Vue magnifique le soir")
    private String description;

    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    @Schema(example = "48.8585")
    private Double latitude;

    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    @Schema(example = "2.2946")
    private Double longitude;

    @Size(max = 500)
    @Schema(example = "https://example.com/images/tour-eiffel-night.jpg")
    private String imageUrl;

    @Schema(example = "[\"paris\",\"nuit\"]")
    private List<String> tags;
}
