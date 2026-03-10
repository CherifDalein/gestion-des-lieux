package org.example.gestiondeslieux.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.example.gestiondeslieux.enums.Permission;

import java.time.LocalDateTime;

@Data
public class ShareTokenRequest {
    @Schema(example = "READ")
    private Permission permission;
    @Schema(example = "2026-12-31T23:59:59")
    private LocalDateTime expiresAt;
    @Schema(example = "Partage temporaire")
    private String label;
}
