package org.example.gestiondeslieux.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.example.gestiondeslieux.enums.Permission;
import org.example.gestiondeslieux.enums.ResourceType;

import java.time.LocalDateTime;

@Data
public class CreateTokenRequest {

    @NotNull(message = "Le type de ressource est obligatoire")
    @Schema(example = "PLACE")
    private ResourceType resourceType;

    @NotNull(message = "L'identifiant de la ressource est obligatoire")
    @Schema(example = "42")
    private Long resourceId;

    @NotNull(message = "La permission est obligatoire")
    @Schema(example = "READ")
    private Permission permission;

    @Schema(example = "2026-12-31T23:59:59")
    private LocalDateTime expiresAt;

    @Schema(example = "Partage lecture place")
    private String label;
}
