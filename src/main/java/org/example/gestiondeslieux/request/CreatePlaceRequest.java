package org.example.gestiondeslieux.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class CreatePlaceRequest {

    @NotBlank(message = "Le titre est obligatoire")
    @Size(min = 1, max = 200, message = "Le titre doit faire entre 1 et 200 caractères")
    @Schema(example = "Tour Eiffel")
    private String title;

    @Size(max = 2000, message = "La description ne peut pas dépasser 2000 caractères")
    @Schema(example = "Monument emblématique de Paris")
    private String description;

    @NotNull(message = "La latitude est obligatoire")
    @DecimalMin(value = "-90.0", message = "Latitude invalide (min -90)")
    @DecimalMax(value = "90.0", message = "Latitude invalide (max 90)")
    @Schema(example = "48.8584")
    private Double latitude;

    @NotNull(message = "La longitude est obligatoire")
    @DecimalMin(value = "-180.0", message = "Longitude invalide (min -180)")
    @DecimalMax(value = "180.0", message = "Longitude invalide (max 180)")
    @Schema(example = "2.2945")
    private Double longitude;

    @Size(max = 500, message = "L'URL de l'image ne peut pas dépasser 500 caractères")
    @Schema(example = "https://example.com/images/tour-eiffel.jpg")
    private String imageUrl;

    @Schema(example = "[\"paris\",\"monument\",\"tourisme\"]")
    private List<@NotBlank String> tags;
}
