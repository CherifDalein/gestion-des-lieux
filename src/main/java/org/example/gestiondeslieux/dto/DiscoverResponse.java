package org.example.gestiondeslieux.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
public class DiscoverResponse {
    @Schema(example = "http://localhost:8080")
    private String serverUrl;
    @Schema(example = "Partage Paris")
    private String tokenLabel;
    private List<CollectionDto> collections;
    private List<PlaceDto> places;
    @Schema(example = "false")
    private Boolean locationAvailable;
}
