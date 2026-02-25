package org.example.gestiondeslieux.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gestiondeslieux.dto.CollectionDto;
import org.example.gestiondeslieux.dto.PlaceDto;
import org.example.gestiondeslieux.dto.ShareResponse;
import org.example.gestiondeslieux.enums.ExportFormat;
import org.example.gestiondeslieux.enums.Permission;
import org.example.gestiondeslieux.enums.ResourceType;
import org.example.gestiondeslieux.exceptions.ResourceNotFoundException;
import org.example.gestiondeslieux.model.Collection;
import org.example.gestiondeslieux.request.CreateCollectionRequest;
import org.example.gestiondeslieux.request.CreateTokenRequest;
import org.example.gestiondeslieux.request.UpdateCollectionRequest;
import org.example.gestiondeslieux.response.ApiResponse;
import org.example.gestiondeslieux.security.AuthContextUtils;
import org.example.gestiondeslieux.service.collection.ICollectionService;
import org.example.gestiondeslieux.service.place.IPlaceService;
import org.example.gestiondeslieux.service.token.IAccessTokenService;
import org.example.gestiondeslieux.util.GeoMediaTypes;
import org.example.gestiondeslieux.util.HttpCacheUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/collections")
@Tag(name = "Collections", description = "Gestion des collections")
@RequiredArgsConstructor
public class CollectionApiController {

    private final ICollectionService collectionService;
    private final IPlaceService placeService;
    private final IAccessTokenService accessTokenService;

    @Value("${app.share.base-url:http://localhost:8080}")
    private String baseUrl;

    private Long resolveCollectionOwnerId(Long collectionId, Authentication auth, String tokenParam) {
        Long uid = AuthContextUtils.userIdOrNull(auth);
        if (uid != null) return uid;

        String token = AuthContextUtils.resolveToken(auth, tokenParam);
        if (token == null || !accessTokenService.hasPermission(token, ResourceType.COLLECTION, collectionId, Permission.READ)) {
            throw new ResourceNotFoundException("Collection", "id", collectionId);
        }
        return accessTokenService.validateAndGet(token).getCreatedBy().getId();
    }

    @GetMapping
    @Operation(summary = "Lister les collections")
    public ResponseEntity<ApiResponse<List<CollectionDto>>> getCollections(Authentication auth) {
        List<CollectionDto> list = collectionService.getCollectionsByUser(AuthContextUtils.requireUserId(auth))
                .stream().map(collectionService::convertToDto).toList();
        return ResponseEntity.ok(new ApiResponse<>("Collections récupérées avec succès", list));
    }

    @PostMapping
    @Operation(summary = "Créer une collection")
    public ResponseEntity<ApiResponse<CollectionDto>> createCollection(
            @Valid @RequestBody CreateCollectionRequest req, Authentication auth) {
        Collection c = collectionService.createCollection(req, AuthContextUtils.requireUserId(auth));
        return ResponseEntity.status(201)
                .body(new ApiResponse<>("Collection créée avec succès", collectionService.convertToDto(c)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir une collection")
    public ResponseEntity<ApiResponse<CollectionDto>> getCollection(@PathVariable Long id,
                                                                    @RequestParam(required = false) String token,
                                                                    Authentication auth,
                                                                    HttpServletRequest request) {
        Long uid = resolveCollectionOwnerId(id, auth, token);
        Collection c = collectionService.getCollectionById(id, uid);
        CollectionDto dto = collectionService.convertToDto(c);
        ApiResponse<CollectionDto> body = new ApiResponse<>("Collection récupérée avec succès", dto);
        return HttpCacheUtils.buildCachedResponse(body, c.getCreatedAt(), request,
                "private, no-cache");
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une collection")
    public ResponseEntity<ApiResponse<CollectionDto>> updateCollection(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCollectionRequest req,
            Authentication auth) {
        Collection c = collectionService.updateCollection(id, req, AuthContextUtils.requireUserId(auth));
        return ResponseEntity.ok(new ApiResponse<>("Collection mise à jour avec succès",
                collectionService.convertToDto(c)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une collection")
    public ResponseEntity<Void> deleteCollection(@PathVariable Long id, Authentication auth) {
        collectionService.deleteCollection(id, AuthContextUtils.requireUserId(auth));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/places")
    @Operation(summary = "Lieux dans la collection")
    public ResponseEntity<ApiResponse<Page<PlaceDto>>> getPlaces(
            @PathVariable Long id,
            @RequestParam(required = false) String token,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication auth) {
        Long uid = resolveCollectionOwnerId(id, auth, token);
        Page<PlaceDto> page = collectionService.getPlacesInCollection(id, uid, pageable)
                .map(placeService::convertToDto);
        return ResponseEntity.ok(new ApiResponse<>("Lieux de la collection récupérés avec succès", page));
    }

    @GetMapping(value = "/{id}/export",
        produces = {MediaType.APPLICATION_JSON_VALUE,
                    GeoMediaTypes.GPX_VALUE,
                    GeoMediaTypes.KML_VALUE,
                    GeoMediaTypes.GEOJSON_VALUE})
    @Operation(summary = "Exporter une collection (GPX/KML/GeoJSON)")
    public ResponseEntity<String> exportCollection(
            @PathVariable Long id,
            @RequestParam(required = false) String format,
            @RequestHeader(value = "Accept", defaultValue = GeoMediaTypes.GEOJSON_VALUE) String accept,
            Authentication auth) {
        Long userId = AuthContextUtils.requireUserId(auth);
        ExportFormat fmt = resolveFormat(format, accept);
        String content = placeService.exportCollection(id, fmt, userId);
        String mediaType = toMediaType(fmt);
        String name = collectionService.getCollectionById(id, userId).getName();
        String ext = fmt.name().toLowerCase();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + name + "." + ext + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=300")
                .body(content);
    }

    @PostMapping("/{id}/share")
    @Operation(summary = "Partager une collection via token")
    public ResponseEntity<ApiResponse<ShareResponse>> shareCollection(
            @PathVariable Long id,
            @RequestBody(required = false) CreateTokenRequest req,
            Authentication auth) {
        Long uid = AuthContextUtils.requireUserId(auth);
        if (req == null) req = new CreateTokenRequest();
        req.setResourceType(ResourceType.COLLECTION);
        req.setResourceId(id);
        if (req.getPermission() == null) req.setPermission(Permission.READ);
        var token = accessTokenService.createToken(req, uid);
        ShareResponse sr = new ShareResponse();
        sr.setToken(token.getToken());
        sr.setUrl(baseUrl + "/api/collections/" + id + "?token=" + token.getToken());
        sr.setPermission(token.getPermission());
        sr.setResourceType(token.getResourceType());
        sr.setResourceId(token.getResourceId());
        sr.setExpiresAt(token.getExpiresAt());
        return ResponseEntity.ok(new ApiResponse<>("Collection partagée avec succès", sr));
    }

    private ExportFormat resolveFormat(String param, String accept) {
        if (param != null) {
            return switch (param.toLowerCase()) {
                case "gpx" -> ExportFormat.GPX;
                case "kml" -> ExportFormat.KML;
                default    -> ExportFormat.GEOJSON;
            };
        }
        if (accept.contains("gpx")) return ExportFormat.GPX;
        if (accept.contains("kml")) return ExportFormat.KML;
        return ExportFormat.GEOJSON;
    }

    private String toMediaType(ExportFormat fmt) {
        return switch (fmt) {
            case GPX     -> GeoMediaTypes.GPX_VALUE;
            case KML     -> GeoMediaTypes.KML_VALUE;
            case GEOJSON -> GeoMediaTypes.GEOJSON_VALUE;
        };
    }
}
