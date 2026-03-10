package org.example.gestiondeslieux.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.example.gestiondeslieux.enums.Permission;
import org.example.gestiondeslieux.enums.ResourceType;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DiscoverResponse {
    @Schema(example = "http://localhost:8080")
    private String serverUrl;
    @Schema(example = "Partage Paris")
    private String tokenLabel;
    @Schema(example = "PLACE")
    private ResourceType resourceType;
    @Schema(example = "42")
    private Long resourceId;
    @Schema(example = "READ")
    private Permission permission;
    @Schema(example = "2026-02-26T12:30:00")
    private LocalDateTime expiresAt;
    @Schema(example = "false")
    private Boolean expired;
    @Schema(example = "false")
    private Boolean revoked;
    private List<CollectionDto> collections;
    private List<PlaceDto> places;
    @Schema(example = "false")
    private Boolean locationAvailable;
}
