package org.example.gestiondeslieux.controller;


import jakarta.validation.Valid;
import org.example.gestiondeslieux.dto.CreatePlaceRequest;
import org.example.gestiondeslieux.model.Place;
import org.example.gestiondeslieux.service.PlaceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
}
