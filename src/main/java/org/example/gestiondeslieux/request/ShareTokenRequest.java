package org.example.gestiondeslieux.request;

import lombok.Data;
import org.example.gestiondeslieux.enums.Permission;

import java.time.LocalDateTime;

@Data
public class ShareTokenRequest {
    private Permission permission;
    private LocalDateTime expiresAt;
    private String label;
}
