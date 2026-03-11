package org.example.gestiondeslieux.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.example.gestiondeslieux.dto.CreatePlaceRequest;
import org.example.gestiondeslieux.model.Place;
import org.example.gestiondeslieux.service.PlaceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/places")
public class PlaceController {

    private final PlaceService placeService;

    public PlaceController(PlaceService placeService) {
        this.placeService = placeService;
    }

    @PostMapping
    public ResponseEntity<?> createPlace(
            @Valid @RequestBody CreatePlaceRequest request,
            Authentication authentication // CORRECTION : Récupération auto via le Filter
    ) {
        // Le principal contient le userId (défini dans JwtAuthenticationFilter)
        Long userId = (Long) authentication.getPrincipal();

        Place place = placeService.createPlace(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", place.getId(),
                "message", "Lieu créé avec succès"
        ));
    }

    @GetMapping
    public ResponseEntity<List<Place>> getAllPlaces(
            Authentication authentication) {
          if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<Place> places = placeService.getAllPlaces();
        return ResponseEntity.ok(places);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Place> getPlaceById(@PathVariable UUID id,
                                              Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Optional<Place> place = placeService.getPlaceById(id);

        return place.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePlace(
            @PathVariable UUID id,
            @Valid @RequestBody CreatePlaceRequest request,
            Authentication authentication
    ) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Place updatedPlace = placeService.updatePlace(id, request);
            return ResponseEntity.ok(updatedPlace);
        } catch (RuntimeException e) {
            // Si le service ne trouve pas le lieu
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePlace(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        boolean deleted = placeService.deletePlace(id);
        if (deleted) {
            return ResponseEntity.noContent().build(); // 204 No Content
        } else {
            return ResponseEntity.notFound().build();
        }
    }



}
