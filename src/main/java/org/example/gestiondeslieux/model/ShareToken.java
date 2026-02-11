package org.example.gestiondeslieux.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
public class ShareToken {

    @Id
    @GeneratedValue
    private UUID id;

    private Long userId;              // Propriétaire du token
    private String resourceType;      // COLLECTION, PLACE, POSITION
    private UUID resourceId;          // L'ID de la ressource partagée
    private String permission;        // READ, WRITE
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private int accessCount;
    private String token;             // Token utilisé pour l'accès distant
    private boolean revoked = false;  // Pour indiquer si le token a été révoqué

}
