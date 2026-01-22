package org.example.gestiondeslieux.controller;


import jakarta.validation.Valid;
import org.example.gestiondeslieux.dto.CreatePlaceRequest;
import org.example.gestiondeslieux.model.Place;
import org.example.gestiondeslieux.service.PlaceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/places")
public class PlaceController {

    private final PlaceService placeService;

    public PlaceController(PlaceService placeService) {
        this.placeService = placeService;
    }

    @PostMapping
    public ResponseEntity<?> createPlace(
            @Valid @RequestBody CreatePlaceRequest request,
            @RequestHeader("Authorization") String token
    ) {
        // Ici : vérification du token (simplifiée)
        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Place place = placeService.createPlace(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                        "id", place.getId(),
                        "message", "Lieu créé avec succès"
                ));
    }

    @GetMapping
    public ResponseEntity<List<Place>> getAllPlaces(
            @RequestHeader(value ="Authorization", required = false) String token) {
        //verification du token
        //if (!isTokenValid(token)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Place> places = placeService.getAllPlaces();
        return ResponseEntity.ok(places);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Place> getPlaceById(@PathVariable UUID id,
                                              @RequestHeader(value = "Authorization", required = false)
                                              String token) {
        //if (!isTokenValid(token)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Optional<Place> place = placeService.getPlaceById(id);

        return place.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePlace(
            @PathVariable UUID id,
            @Valid @RequestBody CreatePlaceRequest request,
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        /*if (!isTokenValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }*/

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
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
       /* if (!isTokenValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }*/

        boolean deleted = placeService.deletePlace(id);
        if (deleted) {
            return ResponseEntity.noContent().build(); // 204 No Content
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Méthode utilitaire pour éviter de dupliquer le if(token...) partout
    /*private boolean isTokenValid(String token) {
        return token != null && token.startsWith("Bearer ");
    }*/

}
