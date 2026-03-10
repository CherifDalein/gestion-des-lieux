package org.example.gestiondeslieux.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
public class ImportResultDto {
    @Schema(example = "5")
    private int imported;
    @Schema(example = "2")
    private int skipped;
    @Schema(example = "[\"Ligne 12 invalide\"]")
    private List<String> errors;
    private List<PlaceDto> places;
}
