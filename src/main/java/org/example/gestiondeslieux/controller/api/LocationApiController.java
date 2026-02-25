package org.example.gestiondeslieux.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gestiondeslieux.dto.CurrentLocationDto;
import org.example.gestiondeslieux.dto.PublicLocationDto;
import org.example.gestiondeslieux.dto.ShareLocationResponse;
import org.example.gestiondeslieux.model.CurrentLocation;
import org.example.gestiondeslieux.request.UpdateLocationRequest;
import org.example.gestiondeslieux.service.location.ICurrentLocationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/api/location")
@Tag(name = "Location", description = "Position courante")
@RequiredArgsConstructor
public class LocationApiController {

    private final ICurrentLocationService locationService;

    @Value("${app.share.base-url:http://localhost:8080}")
    private String baseUrl;

    private Long userId(Authentication auth) { return (Long) auth.getPrincipal(); }

    private CurrentLocationDto toDto(CurrentLocation loc) {
        CurrentLocationDto dto = new CurrentLocationDto();
        dto.setId(loc.getId());
        dto.setLatitude(loc.getLatitude());
        dto.setLongitude(loc.getLongitude());
        dto.setAccuracy(loc.getAccuracy());
        dto.setTimestamp(loc.getTimestamp());
        dto.setIsShared(loc.getIsShared());
        dto.setUpdatedAt(loc.getUpdatedAt());
        return dto;
    }

    @PostMapping("/update")
    @Operation(summary = "Mettre à jour la position courante")
    public ResponseEntity<CurrentLocationDto> update(@Valid @RequestBody UpdateLocationRequest req,
                                                     Authentication auth) {
        CurrentLocation loc = locationService.updateLocation(req, userId(auth));
        return ResponseEntity.ok(toDto(loc));
    }

    @GetMapping("/current")
    @Operation(summary = "Obtenir la position courante")
    public ResponseEntity<CurrentLocationDto> getCurrent(Authentication auth) {
        return ResponseEntity.ok(toDto(locationService.getCurrentLocation(userId(auth))));
    }

    @DeleteMapping("/current")
    @Operation(summary = "Supprimer la position courante")
    public ResponseEntity<Void> deleteCurrent(Authentication auth) {
        locationService.deleteLocation(userId(auth));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/share")
    @Operation(summary = "Démarrer le partage de position")
    public ResponseEntity<ShareLocationResponse> startSharing(Authentication auth) {
        CurrentLocation loc = locationService.startSharing(userId(auth));
        ShareLocationResponse res = new ShareLocationResponse();
        res.setToken(loc.getShareToken());
        res.setUrl(baseUrl + "/api/location/public/" + loc.getShareToken());
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/share")
    @Operation(summary = "Arrêter le partage de position")
    public ResponseEntity<Void> stopSharing(Authentication auth) {
        locationService.stopSharing(userId(auth));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/public/{shareToken}")
    @Operation(summary = "Accès public à une position partagée")
    public ResponseEntity<PublicLocationDto> getPublic(@PathVariable String shareToken) {
        CurrentLocation loc = locationService.getSharedLocation(shareToken);
        PublicLocationDto dto = new PublicLocationDto();
        dto.setLatitude(loc.getLatitude());
        dto.setLongitude(loc.getLongitude());
        dto.setAccuracy(loc.getAccuracy());
        dto.setTimestamp(loc.getTimestamp());
        dto.setUpdatedAt(loc.getUpdatedAt());
        dto.setAgeSeconds(loc.getUpdatedAt() != null
                ? ChronoUnit.SECONDS.between(loc.getUpdatedAt(), LocalDateTime.now()) : null);
        return ResponseEntity.ok()
                .header("Cache-Control", "public, max-age=15")
                .body(dto);
    }
}
