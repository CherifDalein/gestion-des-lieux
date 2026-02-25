package org.example.gestiondeslieux.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.example.gestiondeslieux.enums.Permission;
import org.example.gestiondeslieux.enums.ResourceType;

import java.time.LocalDateTime;

@Data
public class CreateTokenRequest {

    @NotNull(message = "Le type de ressource est obligatoire")
    private ResourceType resourceType;

    @NotNull(message = "L'identifiant de la ressource est obligatoire")
    private Long resourceId;

    @NotNull(message = "La permission est obligatoire")
    private Permission permission;

    private LocalDateTime expiresAt;

    private String label;
}
