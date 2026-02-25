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
import org.example.gestiondeslieux.model.Collection;
import org.example.gestiondeslieux.model.Place;
import org.example.gestiondeslieux.request.CreateCollectionRequest;
import org.example.gestiondeslieux.request.CreateTokenRequest;
import org.example.gestiondeslieux.request.UpdateCollectionRequest;
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

    private Long userId(Authentication auth) { return (Long) auth.getPrincipal(); }

    private CollectionDto toDto(Collection c, Long userId) {
        CollectionDto dto = new CollectionDto();
        dto.setId(c.getId());
        dto.setName(c.getName());
        dto.setTagFilter(c.getTagFilter());
        dto.setIsShared(c.getIsShared());
        dto.setCreatedAt(c.getCreatedAt());
        dto.setUsername(c.getUser().getUsername());
        Page<Place> places = collectionService.getPlacesInCollection(c.getId(), userId,
                org.springframework.data.domain.Pageable.ofSize(1));
        dto.setPlaceCount(places.getTotalElements());
        return dto;
    }

    private PlaceDto placeToDto(Place p) {
        PlaceDto dto = new PlaceDto();
        dto.setId(p.getId());
        dto.setTitle(p.getTitle());
        dto.setDescription(p.getDescription());
        dto.setLatitude(p.getLatitude());
        dto.setLongitude(p.getLongitude());
        dto.setImageUrl(p.getImageUrl());
        dto.setTags(p.getTags());
        dto.setCreatedAt(p.getCreatedAt());
        dto.setUpdatedAt(p.getUpdatedAt());
        if (p.getUser() != null) dto.setUsername(p.getUser().getUsername());
        return dto;
    }

    @GetMapping
    @Operation(summary = "Lister les collections")
    public ResponseEntity<List<CollectionDto>> getCollections(Authentication auth) {
        Long uid = userId(auth);
        List<CollectionDto> list = collectionService.getCollectionsByUser(uid)
                .stream().map(c -> toDto(c, uid)).toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping
    @Operation(summary = "Créer une collection")
    public ResponseEntity<CollectionDto> createCollection(
            @Valid @RequestBody CreateCollectionRequest req, Authentication auth) {
        Long uid = userId(auth);
        Collection c = collectionService.createCollection(req, uid);
        return ResponseEntity.status(201).body(toDto(c, uid));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir une collection")
    public ResponseEntity<?> getCollection(@PathVariable Long id,
                                           @RequestParam(required = false) String token,
                                           Authentication auth,
                                           HttpServletRequest request) {
        Long uid = userId(auth);
        Collection c = collectionService.getCollectionById(id, uid);
        CollectionDto dto = toDto(c, uid);
        return HttpCacheUtils.buildCachedResponse(dto, c.getCreatedAt(), request,
                "private, no-cache");
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une collection")
    public ResponseEntity<CollectionDto> updateCollection(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCollectionRequest req,
            Authentication auth) {
        Long uid = userId(auth);
        Collection c = collectionService.updateCollection(id, req, uid);
        return ResponseEntity.ok(toDto(c, uid));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une collection")
    public ResponseEntity<Void> deleteCollection(@PathVariable Long id, Authentication auth) {
        collectionService.deleteCollection(id, userId(auth));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/places")
    @Operation(summary = "Lieux dans la collection")
    public ResponseEntity<Page<PlaceDto>> getPlaces(
            @PathVariable Long id,
            @RequestParam(required = false) String token,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication auth) {
        Page<PlaceDto> page = collectionService.getPlacesInCollection(id, userId(auth), pageable)
                .map(this::placeToDto);
        return ResponseEntity.ok(page);
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
        ExportFormat fmt = resolveFormat(format, accept);
        String content = placeService.exportCollection(id, fmt, userId(auth));
        String mediaType = toMediaType(fmt);
        String name = collectionService.getCollectionById(id, userId(auth)).getName();
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
    public ResponseEntity<ShareResponse> shareCollection(
            @PathVariable Long id,
            @RequestBody(required = false) CreateTokenRequest req,
            Authentication auth) {
        Long uid = userId(auth);
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
        return ResponseEntity.ok(sr);
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
