package org.example.gestiondeslieux.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gestiondeslieux.dto.PlaceDto;
import org.example.gestiondeslieux.dto.PlaceWithDistanceDto;
import org.example.gestiondeslieux.dto.ShareResponse;
import org.example.gestiondeslieux.enums.Permission;
import org.example.gestiondeslieux.enums.ResourceType;
import org.example.gestiondeslieux.model.Place;
import org.example.gestiondeslieux.request.CreateTokenRequest;
import org.example.gestiondeslieux.request.CreatePlaceRequest;
import org.example.gestiondeslieux.request.ShareTokenRequest;
import org.example.gestiondeslieux.request.UpdatePlaceRequest;
import org.example.gestiondeslieux.response.ApiResponse;
import org.example.gestiondeslieux.security.AuthContextUtils;
import org.example.gestiondeslieux.service.image.IPlaceImageService;
import org.example.gestiondeslieux.service.place.IPlaceService;
import org.example.gestiondeslieux.service.token.IAccessTokenService;
import org.example.gestiondeslieux.util.HttpCacheUtils;
import org.springframework.beans.factory.annotation.Value;
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
    private final IAccessTokenService accessTokenService;

    @Value("${app.share.base-url:http://localhost:8080}")
    private String baseUrl;

    @GetMapping
    @Operation(summary = "Lister les lieux de l'utilisateur")
    public ResponseEntity<ApiResponse<Page<PlaceDto>>> getPlaces(Authentication auth,
                                                                 @PageableDefault(size = 20) Pageable pageable,
                                                                 HttpServletRequest request) {
        Long userId = AuthContextUtils.requireUserId(auth);
        Page<PlaceDto> page = placeService.getPlacesByUser(userId, pageable)
                .map(placeService::convertToDto);
        Optional<LocalDateTime> lastMod = placeService.getAllTagsByUser(userId).isEmpty()
                ? Optional.empty()
                : Optional.ofNullable(page.getContent().isEmpty() ? null
                    : page.getContent().stream()
                          .map(PlaceDto::getUpdatedAt)
                          .max(LocalDateTime::compareTo)
                          .orElse(null));
        ApiResponse<Page<PlaceDto>> body = new ApiResponse<>("Lieux récupérés avec succès", page);
        return HttpCacheUtils.buildCachedResponse(body, lastMod.orElse(null), request,
                "private, no-cache");
    }

    @PostMapping
    @Operation(summary = "Créer un lieu")
    public ResponseEntity<ApiResponse<PlaceDto>> createPlace(@Valid @RequestBody CreatePlaceRequest req,
                                                             Authentication auth) {
        Place p = placeService.createPlace(req, AuthContextUtils.requireUserId(auth));
        return ResponseEntity.status(201)
                .body(new ApiResponse<>("Lieu créé avec succès", placeService.convertToDto(p)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir un lieu")
    public ResponseEntity<ApiResponse<PlaceDto>> getPlace(@PathVariable Long id,
                                                          @RequestParam(required = false) String token,
                                                          Authentication auth,
                                                          HttpServletRequest request) {
        Long uid = AuthContextUtils.userIdOrNull(auth);
        Place p = placeService.getPlaceByIdWithToken(id, uid, AuthContextUtils.resolveToken(auth, token));
        PlaceDto dto = placeService.convertToDto(p);
        ApiResponse<PlaceDto> body = new ApiResponse<>("Lieu récupéré avec succès", dto);
        return HttpCacheUtils.buildCachedResponse(body, p.getUpdatedAt(), request,
                "private, max-age=60, must-revalidate");
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un lieu")
    public ResponseEntity<ApiResponse<PlaceDto>> updatePlace(@PathVariable Long id,
                                                             @Valid @RequestBody UpdatePlaceRequest req,
                                                             Authentication auth) {
        Place p = placeService.updatePlace(id, req, AuthContextUtils.requireUserId(auth));
        return ResponseEntity.ok(new ApiResponse<>("Lieu mis à jour avec succès", placeService.convertToDto(p)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un lieu")
    public ResponseEntity<Void> deletePlace(@PathVariable Long id, Authentication auth) {
        placeService.deletePlace(id, AuthContextUtils.requireUserId(auth));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/share")
    @Operation(summary = "Partager un lieu via token")
    public ResponseEntity<ApiResponse<ShareResponse>> sharePlace(
            @PathVariable Long id,
            @RequestBody(required = false) ShareTokenRequest req,
            Authentication auth) {
        Long userId = AuthContextUtils.requireUserId(auth);
        CreateTokenRequest createReq = new CreateTokenRequest();
        createReq.setResourceType(ResourceType.PLACE);
        createReq.setResourceId(id);
        createReq.setPermission(req != null && req.getPermission() != null ? req.getPermission() : Permission.READ);
        createReq.setExpiresAt(req != null ? req.getExpiresAt() : null);
        createReq.setLabel(req != null ? req.getLabel() : null);
        var token = accessTokenService.createToken(createReq, userId);
        ShareResponse sr = new ShareResponse();
        sr.setToken(token.getToken());
        sr.setUrl(baseUrl + "/api/places/" + id + "?token=" + token.getToken());
        sr.setPermission(token.getPermission());
        sr.setResourceType(token.getResourceType());
        sr.setResourceId(token.getResourceId());
        sr.setExpiresAt(token.getExpiresAt());
        return ResponseEntity.ok(new ApiResponse<>("Lieu partagé avec succès", sr));
    }

    @GetMapping("/search")
    @Operation(summary = "Rechercher des lieux")
    public ResponseEntity<ApiResponse<Page<PlaceDto>>> searchPlaces(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(required = false, defaultValue = "5.0") Double radius,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication auth) {
        Long userId = AuthContextUtils.requireUserId(auth);
        org.example.gestiondeslieux.request.PlaceSearchRequest req =
                new org.example.gestiondeslieux.request.PlaceSearchRequest();
        req.setQ(q); req.setTag(tag); req.setLat(lat); req.setLon(lon); req.setRadiusKm(radius);
        Page<PlaceDto> page = placeService.searchPlaces(req, userId, pageable)
                .map(placeService::convertToDto);
        return ResponseEntity.ok(new ApiResponse<>("Recherche effectuée avec succès", page));
    }

    @GetMapping("/nearby")
    @Operation(summary = "Lieux à proximité (Haversine)")
    public ResponseEntity<ApiResponse<Page<PlaceWithDistanceDto>>> getNearby(
            @RequestParam Double lat,
            @RequestParam Double lon,
            @RequestParam Double radius,
            @RequestParam(required = false) String tag,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication auth) {
        Page<Object[]> raw = placeService.searchNearby(lat, lon, radius, AuthContextUtils.requireUserId(auth), pageable);
        Page<PlaceWithDistanceDto> page = raw.map(placeService::convertNearbyToDto);
        return ResponseEntity.ok(new ApiResponse<>("Lieux proches récupérés avec succès", page));
    }

    @GetMapping("/tags")
    @Operation(summary = "Tous les tags de l'utilisateur")
    public ResponseEntity<ApiResponse<List<String>>> getTags(Authentication auth) {
        return ResponseEntity.ok(new ApiResponse<>("Tags récupérés avec succès",
                placeService.getAllTagsByUser(AuthContextUtils.requireUserId(auth))));
    }

    @PostMapping("/{id}/tags/{tag}")
    @Operation(summary = "Ajouter un tag")
    public ResponseEntity<ApiResponse<PlaceDto>> addTag(@PathVariable Long id,
                                                        @PathVariable String tag,
                                                        Authentication auth) {
        Long userId = AuthContextUtils.requireUserId(auth);
        placeService.addTagToPlace(id, tag, userId);
        return ResponseEntity.ok(new ApiResponse<>("Tag ajouté avec succès",
                placeService.convertToDto(placeService.getPlaceById(id, userId))));
    }

    @DeleteMapping("/{id}/tags/{tag}")
    @Operation(summary = "Retirer un tag")
    public ResponseEntity<ApiResponse<PlaceDto>> removeTag(@PathVariable Long id,
                                                           @PathVariable String tag,
                                                           Authentication auth) {
        Long userId = AuthContextUtils.requireUserId(auth);
        placeService.removeTagFromPlace(id, tag, userId);
        return ResponseEntity.ok(new ApiResponse<>("Tag retiré avec succès",
                placeService.convertToDto(placeService.getPlaceById(id, userId))));
    }

    @PostMapping("/{id}/image")
    @Operation(summary = "Uploader une image")
    public ResponseEntity<ApiResponse<PlaceDto>> uploadImage(@PathVariable Long id,
                                                             @RequestParam("file") MultipartFile file,
                                                             Authentication auth) {
        Long userId = AuthContextUtils.requireUserId(auth);
        placeImageService.uploadImage(file, id, userId);
        return ResponseEntity.ok(new ApiResponse<>("Image uploadée avec succès",
                placeService.convertToDto(placeService.getPlaceById(id, userId))));
    }

    @DeleteMapping("/{id}/image")
    @Operation(summary = "Supprimer l'image principale")
    public ResponseEntity<ApiResponse<PlaceDto>> deleteImage(@PathVariable Long id, Authentication auth) {
        Place p = placeImageService.deleteMainImage(id, AuthContextUtils.requireUserId(auth));
        return ResponseEntity.ok(new ApiResponse<>("Image supprimée avec succès", placeService.convertToDto(p)));
    }
}
