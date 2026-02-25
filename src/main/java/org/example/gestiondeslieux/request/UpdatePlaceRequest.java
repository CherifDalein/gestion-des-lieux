package org.example.gestiondeslieux.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class UpdatePlaceRequest {

    @Size(min = 1, max = 200)
    private String title;

    @Size(max = 2000)
    private String description;

    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private Double latitude;

    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private Double longitude;

    @Size(max = 500)
    private String imageUrl;

    private List<String> tags;
}
