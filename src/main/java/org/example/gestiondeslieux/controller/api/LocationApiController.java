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
import org.example.gestiondeslieux.response.ApiResponse;
import org.example.gestiondeslieux.security.AuthContextUtils;
import org.example.gestiondeslieux.service.location.ICurrentLocationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/location")
@Tag(name = "Location", description = "Position courante")
@RequiredArgsConstructor
public class LocationApiController {

    private final ICurrentLocationService locationService;

    @Value("${app.share.base-url:http://localhost:8080}")
    private String baseUrl;

    @PostMapping("/update")
    @Operation(summary = "Mettre à jour la position courante")
    public ResponseEntity<ApiResponse<CurrentLocationDto>> update(@Valid @RequestBody UpdateLocationRequest req,
                                                                  Authentication auth) {
        CurrentLocation loc = locationService.updateLocation(req, AuthContextUtils.requireUserId(auth));
        return ResponseEntity.ok(new ApiResponse<>("Position mise à jour avec succès",
                locationService.convertToDto(loc)));
    }

    @GetMapping("/current")
    @Operation(summary = "Obtenir la position courante")
    public ResponseEntity<ApiResponse<CurrentLocationDto>> getCurrent(Authentication auth) {
        CurrentLocationDto dto = locationService.convertToDto(
                locationService.getCurrentLocation(AuthContextUtils.requireUserId(auth)));
        return ResponseEntity.ok(new ApiResponse<>("Position courante récupérée avec succès", dto));
    }

    @DeleteMapping("/current")
    @Operation(summary = "Supprimer la position courante")
    public ResponseEntity<Void> deleteCurrent(Authentication auth) {
        locationService.deleteLocation(AuthContextUtils.requireUserId(auth));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/share")
    @Operation(summary = "Démarrer le partage de position")
    public ResponseEntity<ApiResponse<ShareLocationResponse>> startSharing(Authentication auth) {
        CurrentLocation loc = locationService.startSharing(AuthContextUtils.requireUserId(auth));
        ShareLocationResponse res = new ShareLocationResponse();
        res.setToken(loc.getShareToken());
        res.setUrl(baseUrl + "/api/location/public/" + loc.getShareToken());
        return ResponseEntity.ok(new ApiResponse<>("Partage de position activé", res));
    }

    @DeleteMapping("/share")
    @Operation(summary = "Arrêter le partage de position")
    public ResponseEntity<Void> stopSharing(Authentication auth) {
        locationService.stopSharing(AuthContextUtils.requireUserId(auth));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/public/{shareToken}")
    @Operation(summary = "Accès public à une position partagée")
    public ResponseEntity<ApiResponse<PublicLocationDto>> getPublic(@PathVariable String shareToken) {
        CurrentLocation loc = locationService.getSharedLocation(shareToken);
        PublicLocationDto dto = locationService.convertToPublicDto(loc);
        return ResponseEntity.ok()
                .header("Cache-Control", "public, max-age=15")
                .body(new ApiResponse<>("Position publique récupérée avec succès", dto));
    }
}
