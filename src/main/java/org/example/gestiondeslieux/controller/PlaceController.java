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

import java.util.Map;

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
}