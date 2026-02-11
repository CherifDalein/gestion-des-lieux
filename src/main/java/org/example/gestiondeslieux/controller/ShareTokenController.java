package org.example.gestiondeslieux.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.example.gestiondeslieux.model.ShareToken;
import org.example.gestiondeslieux.service.ShareTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/tokens")
@SecurityRequirement(name = "bearerAuth")
public class ShareTokenController {

    private final ShareTokenService service;

    public ShareTokenController(ShareTokenService service) {
        this.service = service;
    }

    // Lister tous les tokens actifs pour l'utilisateur
    @GetMapping
    public List<ShareToken> listTokens(
            Authentication authentication,
            @RequestParam(required = false) String resourceType
    ) {
        Long userId = (Long) authentication.getPrincipal();
        if (resourceType != null) {
            return service.getTokensByUserAndType(userId, resourceType);
        }
        return service.getTokensByUser(userId);
    }

    // Révoquer un token
    @PostMapping("/{id}/revoke")
    public ResponseEntity<?> revokeToken(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        Optional<ShareToken> token = service.revokeToken(id);
        if (token.isPresent() && token.get().getUserId().equals(userId)) {
            return ResponseEntity.ok(Map.of("message", "Token révoqué"));
        } else {
            return ResponseEntity.status(403).body(Map.of("message", "Accès refusé ou token non trouvé"));
        }
    }

    // Modifier l'expiration d'un token
    @PostMapping("/{id}/expiration")
    public ResponseEntity<?> updateExpiration(
            @PathVariable UUID id,
            @RequestParam LocalDateTime newExpiration,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        Optional<ShareToken> token = service.updateExpiration(id, newExpiration);
        if (token.isPresent() && token.get().getUserId().equals(userId)) {
            return ResponseEntity.ok(Map.of("message", "Expiration mise à jour"));
        } else {
            return ResponseEntity.status(403).body(Map.of("message", "Accès refusé ou token non trouvé"));
        }
    }

    // Créer un token
    @PostMapping("/create")
    public ShareToken createToken(
            @RequestParam String resourceType,
            @RequestParam UUID resourceId,
            @RequestParam String permission,
            @RequestParam(required = false) LocalDateTime expiresAt,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return service.createToken(userId, resourceType, resourceId, permission, expiresAt);
    }
}
