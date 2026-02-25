package org.example.gestiondeslieux.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.gestiondeslieux.enums.Permission;
import org.example.gestiondeslieux.enums.ResourceType;
import org.springframework.hateoas.RepresentationModel;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
public class AccessTokenDto extends RepresentationModel<AccessTokenDto> {
    private Long id;
    private String token;
    private ResourceType resourceType;
    private Long resourceId;
    private Permission permission;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    private LocalDateTime createdAt;
    private String label;
    private Long accessCount;
    private Boolean isValid;
    private String resourceName;
}
