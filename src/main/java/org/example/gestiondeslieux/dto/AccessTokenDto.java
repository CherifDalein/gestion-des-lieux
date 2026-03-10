package org.example.gestiondeslieux.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.gestiondeslieux.enums.Permission;
import org.example.gestiondeslieux.enums.ResourceType;
import org.springframework.hateoas.RepresentationModel;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
public class AccessTokenDto extends RepresentationModel<AccessTokenDto> {
    @Schema(example = "12")
    private Long id;
    @Schema(example = "tok_abcd1234xyz")
    private String token;
    @Schema(example = "PLACE")
    private ResourceType resourceType;
    @Schema(example = "42")
    private Long resourceId;
    @Schema(example = "READ")
    private Permission permission;
    @Schema(example = "2026-12-31T23:59:59")
    private LocalDateTime expiresAt;
    @Schema(example = "2026-03-01T10:00:00")
    private LocalDateTime revokedAt;
    @Schema(example = "2026-02-25T22:30:00")
    private LocalDateTime createdAt;
    @Schema(example = "Partage temporaire")
    private String label;
    @Schema(example = "3")
    private Long accessCount;
    @Schema(example = "true")
    private Boolean isValid;
    @Schema(example = "Tour Eiffel")
    private String resourceName;
}
