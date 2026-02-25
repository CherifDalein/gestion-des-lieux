package org.example.gestiondeslieux.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gestiondeslieux.dto.PlaceDto;
import org.example.gestiondeslieux.dto.PlaceWithDistanceDto;
import org.example.gestiondeslieux.model.Place;
import org.example.gestiondeslieux.request.CreatePlaceRequest;
import org.example.gestiondeslieux.request.UpdatePlaceRequest;
import org.example.gestiondeslieux.service.image.IPlaceImageService;
import org.example.gestiondeslieux.service.place.IPlaceService;
import org.example.gestiondeslieux.util.HttpCacheUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/places")
@Tag(name = "Places", description = "Gestion des lieux")
@RequiredArgsConstructor
public class PlaceApiController {

    private final IPlaceService placeService;
    private final IPlaceImageService placeImageService;

    private Long userId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }

    private PlaceDto toDto(Place p) {
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
    @Operation(summary = "Lister les lieux de l'utilisateur")
    public ResponseEntity<?> getPlaces(Authentication auth,
                                       @PageableDefault(size = 20) Pageable pageable,
                                       HttpServletRequest request) {
        Page<PlaceDto> page = placeService.getPlacesByUser(userId(auth), pageable).map(this::toDto);
        Optional<LocalDateTime> lastMod = placeService.getAllTagsByUser(userId(auth)).isEmpty()
                ? Optional.empty()
                : Optional.ofNullable(page.getContent().isEmpty() ? null
                    : page.getContent().stream()
                          .map(PlaceDto::getUpdatedAt)
                          .max(LocalDateTime::compareTo)
                          .orElse(null));
        return HttpCacheUtils.buildCachedResponse(page, lastMod.orElse(null), request,
                "private, no-cache");
    }

    @PostMapping
    @Operation(summary = "Créer un lieu")
    public ResponseEntity<PlaceDto> createPlace(@Valid @RequestBody CreatePlaceRequest req,
                                                Authentication auth) {
        Place p = placeService.createPlace(req, userId(auth));
        return ResponseEntity.status(201).body(toDto(p));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir un lieu")
    public ResponseEntity<?> getPlace(@PathVariable Long id,
                                      @RequestParam(required = false) String token,
                                      Authentication auth,
                                      HttpServletRequest request) {
        Long uid = auth != null ? (Long) auth.getPrincipal() : null;
        Place p = placeService.getPlaceByIdWithToken(id, uid, token);
        PlaceDto dto = toDto(p);
        return HttpCacheUtils.buildCachedResponse(dto, p.getUpdatedAt(), request,
                "private, max-age=60, must-revalidate");
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un lieu")
    public ResponseEntity<PlaceDto> updatePlace(@PathVariable Long id,
                                                @Valid @RequestBody UpdatePlaceRequest req,
                                                Authentication auth) {
        Place p = placeService.updatePlace(id, req, userId(auth));
        return ResponseEntity.ok(toDto(p));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un lieu")
    public ResponseEntity<Void> deletePlace(@PathVariable Long id, Authentication auth) {
        placeService.deletePlace(id, userId(auth));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @Operation(summary = "Rechercher des lieux")
    public ResponseEntity<Page<PlaceDto>> searchPlaces(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(required = false, defaultValue = "5.0") Double radius,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication auth) {
        org.example.gestiondeslieux.request.PlaceSearchRequest req =
                new org.example.gestiondeslieux.request.PlaceSearchRequest();
        req.setQ(q); req.setTag(tag); req.setLat(lat); req.setLon(lon); req.setRadiusKm(radius);
        Page<PlaceDto> page = placeService.searchPlaces(req, userId(auth), pageable).map(this::toDto);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/nearby")
    @Operation(summary = "Lieux à proximité (Haversine)")
    public ResponseEntity<Page<PlaceWithDistanceDto>> getNearby(
            @RequestParam Double lat,
            @RequestParam Double lon,
            @RequestParam Double radius,
            @RequestParam(required = false) String tag,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication auth) {
        Page<Object[]> raw = placeService.searchNearby(lat, lon, radius, userId(auth), pageable);
        Page<PlaceWithDistanceDto> page = raw.map(row -> {
            PlaceWithDistanceDto dto = new PlaceWithDistanceDto();
            // columns from native query: id, title, description, latitude, longitude,
            // image_url, created_at, updated_at, user_id, version, distance_km
            dto.setLatitude(((Number) row[3]).doubleValue());
            dto.setLongitude(((Number) row[4]).doubleValue());
            dto.setDistanceKm(row[row.length - 1] != null ? ((Number) row[row.length - 1]).doubleValue() : null);
            return dto;
        });
        return ResponseEntity.ok(page);
    }

    @GetMapping("/tags")
    @Operation(summary = "Tous les tags de l'utilisateur")
    public ResponseEntity<List<String>> getTags(Authentication auth) {
        return ResponseEntity.ok(placeService.getAllTagsByUser(userId(auth)));
    }

    @PostMapping("/{id}/tags/{tag}")
    @Operation(summary = "Ajouter un tag")
    public ResponseEntity<PlaceDto> addTag(@PathVariable Long id,
                                           @PathVariable String tag,
                                           Authentication auth) {
        placeService.addTagToPlace(id, tag, userId(auth));
        return ResponseEntity.ok(toDto(placeService.getPlaceById(id, userId(auth))));
    }

    @DeleteMapping("/{id}/tags/{tag}")
    @Operation(summary = "Retirer un tag")
    public ResponseEntity<PlaceDto> removeTag(@PathVariable Long id,
                                              @PathVariable String tag,
                                              Authentication auth) {
        placeService.removeTagFromPlace(id, tag, userId(auth));
        return ResponseEntity.ok(toDto(placeService.getPlaceById(id, userId(auth))));
    }

    @PostMapping("/{id}/image")
    @Operation(summary = "Uploader une image")
    public ResponseEntity<PlaceDto> uploadImage(@PathVariable Long id,
                                                @RequestParam("file") MultipartFile file,
                                                Authentication auth) {
        placeImageService.uploadImage(file, id, userId(auth));
        return ResponseEntity.ok(toDto(placeService.getPlaceById(id, userId(auth))));
    }

    @DeleteMapping("/{id}/image")
    @Operation(summary = "Supprimer l'image principale")
    public ResponseEntity<PlaceDto> deleteImage(@PathVariable Long id, Authentication auth) {
        Place p = placeService.getPlaceById(id, userId(auth));
        p.setImageUrl(null);
        return ResponseEntity.ok(toDto(p));
    }
}
