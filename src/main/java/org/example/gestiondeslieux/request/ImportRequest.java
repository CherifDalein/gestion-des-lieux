package org.example.gestiondeslieux.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.example.gestiondeslieux.enums.ExportFormat;

import java.util.List;

@Data
public class ImportRequest {
    @Schema(example = "{\"type\":\"FeatureCollection\",\"features\":[]}")
    private String content;
    @Schema(example = "GEOJSON")
    private ExportFormat format;
    @Schema(example = "[\"import\",\"geojson\"]")
    private List<String> defaultTags;
    @Schema(example = "true")
    private boolean skipDuplicates = true;
}
