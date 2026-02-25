package org.example.gestiondeslieux.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCollectionRequest {

    @Size(max = 200)
    @Schema(example = "Lieux à visiter")
    private String name;

    @Schema(example = "voyage")
    private String tagFilter;
}
