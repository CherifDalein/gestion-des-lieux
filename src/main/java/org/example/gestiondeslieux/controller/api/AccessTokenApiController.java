package org.example.gestiondeslieux.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gestiondeslieux.dto.AccessTokenDto;
import org.example.gestiondeslieux.dto.DiscoverResponse;
import org.example.gestiondeslieux.model.AccessToken;
import org.example.gestiondeslieux.request.CreateTokenRequest;
import org.example.gestiondeslieux.service.token.IAccessTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tokens")
@Tag(name = "Tokens", description = "Gestion des tokens de partage ACL")
@RequiredArgsConstructor
public class AccessTokenApiController {

    private final IAccessTokenService accessTokenService;

    @Value("${app.share.base-url:http://localhost:8080}")
    private String baseUrl;

    private Long userId(Authentication auth) { return (Long) auth.getPrincipal(); }

    private AccessTokenDto toDto(AccessToken t) {
        AccessTokenDto dto = new AccessTokenDto();
        dto.setId(t.getId());
        dto.setToken(t.getToken());
        dto.setResourceType(t.getResourceType());
        dto.setResourceId(t.getResourceId());
        dto.setPermission(t.getPermission());
        dto.setExpiresAt(t.getExpiresAt());
        dto.setRevokedAt(t.getRevokedAt());
        dto.setCreatedAt(t.getCreatedAt());
        dto.setLabel(t.getLabel());
        dto.setAccessCount(t.getAccessCount());
        boolean valid = t.getRevokedAt() == null &&
                (t.getExpiresAt() == null || t.getExpiresAt().isAfter(LocalDateTime.now()));
        dto.setIsValid(valid);
        return dto;
    }

    @PostMapping
    @Operation(summary = "Créer un token de partage")
    public ResponseEntity<AccessTokenDto> createToken(@Valid @RequestBody CreateTokenRequest req,
                                                      Authentication auth) {
        AccessToken token = accessTokenService.createToken(req, userId(auth));
        return ResponseEntity.status(201).body(toDto(token));
    }

    @GetMapping
    @Operation(summary = "Lister mes tokens")
    public ResponseEntity<List<AccessTokenDto>> getTokens(Authentication auth) {
        List<AccessTokenDto> list = accessTokenService.getTokensByUser(userId(auth))
                .stream().map(this::toDto).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir un token")
    public ResponseEntity<AccessTokenDto> getToken(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(toDto(accessTokenService.getTokenById(id, userId(auth))));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Révoquer un token")
    public ResponseEntity<Void> revokeToken(@PathVariable Long id, Authentication auth) {
        accessTokenService.revokeToken(id, userId(auth));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/expiration")
    @Operation(summary = "Modifier l'expiration d'un token")
    public ResponseEntity<AccessTokenDto> updateExpiration(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        AccessToken token = accessTokenService.getTokenById(id, userId(auth));
        if (body.containsKey("expiresAt")) {
            token.setExpiresAt(LocalDateTime.parse(body.get("expiresAt")));
        }
        return ResponseEntity.ok(toDto(token));
    }

    @GetMapping("/discover")
    @Operation(summary = "Découvrir les ressources accessibles avec un token")
    public ResponseEntity<DiscoverResponse> discover(@RequestParam String token) {
        AccessToken at = accessTokenService.validateAndGet(token);
        DiscoverResponse dr = new DiscoverResponse();
        dr.setServerUrl(baseUrl);
        dr.setTokenLabel(at.getLabel());
        dr.setCollections(List.of());
        dr.setPlaces(List.of());
        dr.setLocationAvailable(false);
        return ResponseEntity.ok(dr);
    }
}
