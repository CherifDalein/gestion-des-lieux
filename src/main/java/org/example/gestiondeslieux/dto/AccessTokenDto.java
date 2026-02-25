package org.example.gestiondeslieux.dto;

import lombok.Data;
import org.example.gestiondeslieux.enums.Permission;
import org.example.gestiondeslieux.enums.ResourceType;

import java.time.LocalDateTime;

@Data
public class AccessTokenDto {
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
