package org.example.gestiondeslieux.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class CreatePlaceRequest {

    @NotBlank(message = "Le titre est obligatoire")
    @Size(min = 1, max = 200, message = "Le titre doit faire entre 1 et 200 caractères")
    private String title;

    @Size(max = 2000, message = "La description ne peut pas dépasser 2000 caractères")
    private String description;

    @NotNull(message = "La latitude est obligatoire")
    @DecimalMin(value = "-90.0", message = "Latitude invalide (min -90)")
    @DecimalMax(value = "90.0", message = "Latitude invalide (max 90)")
    private Double latitude;

    @NotNull(message = "La longitude est obligatoire")
    @DecimalMin(value = "-180.0", message = "Longitude invalide (min -180)")
    @DecimalMax(value = "180.0", message = "Longitude invalide (max 180)")
    private Double longitude;

    @Size(max = 500, message = "L'URL de l'image ne peut pas dépasser 500 caractères")
    private String imageUrl;

    private List<@NotBlank String> tags;
}
