package org.example.gestiondeslieux.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCollectionRequest {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 200)
    @Schema(example = "Mes lieux favoris")
    private String name;

    @Schema(example = "paris")
    private String tagFilter;
}
