package org.example.gestiondeslieux.dto;

import lombok.Data;
import org.example.gestiondeslieux.enums.Permission;
import org.example.gestiondeslieux.enums.ResourceType;

import java.time.LocalDateTime;

@Data
public class ShareResponse {
    private String token;
    private String url;
    private Permission permission;
    private ResourceType resourceType;
    private Long resourceId;
    private LocalDateTime expiresAt;
}
