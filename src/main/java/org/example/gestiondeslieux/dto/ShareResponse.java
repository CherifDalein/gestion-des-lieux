package org.example.gestiondeslieux.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.example.gestiondeslieux.enums.Permission;
import org.example.gestiondeslieux.enums.ResourceType;

import java.time.LocalDateTime;

@Data
public class ShareResponse {
    @Schema(example = "tok_share_abc123")
    private String token;
    @Schema(example = "http://localhost:8080/api/places/42?token=tok_share_abc123")
    private String url;
    @Schema(example = "READ")
    private Permission permission;
    @Schema(example = "PLACE")
    private ResourceType resourceType;
    @Schema(example = "42")
    private Long resourceId;
    @Schema(example = "2026-12-31T23:59:59")
    private LocalDateTime expiresAt;
}
