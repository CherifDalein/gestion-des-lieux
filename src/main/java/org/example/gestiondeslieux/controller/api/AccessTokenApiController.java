package org.example.gestiondeslieux.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gestiondeslieux.dto.AccessTokenDto;
import org.example.gestiondeslieux.dto.DiscoverResponse;
import org.example.gestiondeslieux.enums.ResourceType;
import org.example.gestiondeslieux.model.AccessToken;
import org.example.gestiondeslieux.model.Collection;
import org.example.gestiondeslieux.model.Place;
import org.example.gestiondeslieux.request.CreateTokenRequest;
import org.example.gestiondeslieux.response.ApiResponse;
import org.example.gestiondeslieux.security.AuthContextUtils;
import org.example.gestiondeslieux.service.collection.ICollectionService;
import org.example.gestiondeslieux.service.place.IPlaceService;
import org.example.gestiondeslieux.service.token.IAccessTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
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
    private final IPlaceService placeService;
    private final ICollectionService collectionService;

    @Value("${app.share.base-url:http://localhost:8080}")
    private String baseUrl;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Créer un token de partage")
    public ResponseEntity<ApiResponse<AccessTokenDto>> createToken(@Valid @RequestBody CreateTokenRequest req,
                                                                   Authentication auth) {
        AccessToken token = accessTokenService.createToken(req, AuthContextUtils.requireUserId(auth));
        return ResponseEntity.status(201)
                .body(new ApiResponse<>("Token créé avec succès", accessTokenService.convertToDto(token)));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lister mes tokens")
    public ResponseEntity<ApiResponse<List<AccessTokenDto>>> getTokens(Authentication auth) {
        List<AccessTokenDto> list = accessTokenService.getTokensByUser(AuthContextUtils.requireUserId(auth))
                .stream().map(accessTokenService::convertToDto).toList();
        return ResponseEntity.ok(new ApiResponse<>("Tokens récupérés avec succès", list));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Obtenir un token")
    public ResponseEntity<ApiResponse<AccessTokenDto>> getToken(@PathVariable Long id, Authentication auth) {
        AccessTokenDto dto = accessTokenService.convertToDto(
                accessTokenService.getTokenById(id, AuthContextUtils.requireUserId(auth)));
        return ResponseEntity.ok(new ApiResponse<>("Token récupéré avec succès", dto));
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Révoquer un token")
    public ResponseEntity<ApiResponse<Void>> revokeToken(@PathVariable Long id, Authentication auth) {
        accessTokenService.revokeToken(id, AuthContextUtils.requireUserId(auth));
        return ResponseEntity.ok(new ApiResponse<>("Token révoqué avec succès", null));
    }

    @PatchMapping(value = "/{id}/expiration",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modifier l'expiration d'un token")
    public ResponseEntity<ApiResponse<AccessTokenDto>> updateExpiration(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        AccessToken token = accessTokenService.getTokenById(id, AuthContextUtils.requireUserId(auth));
        if (body.containsKey("expiresAt")) {
            token.setExpiresAt(java.time.LocalDateTime.parse(body.get("expiresAt")));
        }
        return ResponseEntity.ok(new ApiResponse<>("Expiration mise à jour avec succès",
                accessTokenService.convertToDto(token)));
    }

    @GetMapping(value = "/discover", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Découvrir les ressources accessibles avec un token")
    public ResponseEntity<ApiResponse<DiscoverResponse>> discover(@RequestParam String token) {
        AccessToken at = accessTokenService.validateAndGet(token);
        LocalDateTime now = LocalDateTime.now();
        Long ownerId = at.getCreatedBy().getId();
        DiscoverResponse dr = new DiscoverResponse();
        dr.setServerUrl(baseUrl);
        dr.setTokenLabel(at.getLabel());
        dr.setResourceType(at.getResourceType());
        dr.setResourceId(at.getResourceId());
        dr.setPermission(at.getPermission());
        dr.setExpiresAt(at.getExpiresAt());
        dr.setExpired(at.getExpiresAt() != null && at.getExpiresAt().isBefore(now));
        dr.setRevoked(at.getRevokedAt() != null);
        dr.setCollections(List.of());
        dr.setPlaces(List.of());
        dr.setLocationAvailable(false);

        if (at.getResourceType() == ResourceType.PLACE) {
            Place place = placeService.getPlaceByIdWithToken(at.getResourceId(), null, at.getToken());
            dr.setPlaces(List.of(placeService.convertToDto(place)));
        } else if (at.getResourceType() == ResourceType.COLLECTION) {
            Collection collection = collectionService.getCollectionById(at.getResourceId(), ownerId);
            dr.setCollections(List.of(collectionService.convertToDto(collection)));
            dr.setPlaces(collectionService.getPlacesInCollection(at.getResourceId(), ownerId, Pageable.unpaged())
                    .map(placeService::convertToDto)
                    .toList());
        } else if (at.getResourceType() == ResourceType.CURRENT_LOCATION) {
            dr.setLocationAvailable(true);
        }

        return ResponseEntity.ok(new ApiResponse<>("Découverte effectuée avec succès", dr));
    }
}
