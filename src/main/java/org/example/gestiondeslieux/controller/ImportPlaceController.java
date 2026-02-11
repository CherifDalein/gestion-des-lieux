package org.example.gestiondeslieux.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.example.gestiondeslieux.model.Place;
import org.example.gestiondeslieux.service.ImportPlaceService;
import org.example.gestiondeslieux.service.PlaceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/import")
@SecurityRequirement(name = "bearerAuth")
public class ImportPlaceController {

    private final ImportPlaceService importPlaceService; // Pour parser les fichiers
    private final PlaceService placeService;             // Pour CRUD et collections

    public ImportPlaceController(ImportPlaceService importPlaceService, PlaceService placeService) {
        this.importPlaceService = importPlaceService;
        this.placeService = placeService;
    }

    /** Endpoint pour prévisualiser l'import */
    @PostMapping("/preview")
    public ResponseEntity<?> previewImport(
            @RequestPart("file") MultipartFile file,  // <--- corrigé ici
            Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<Place> parsedPlaces = importPlaceService.parsePlacesFile(file);

            List<Map<String, Object>> conflicts = new ArrayList<>();
            for (Place p : parsedPlaces) {
                boolean exists = importPlaceService.existsByCoordinates(p.getLatitude(), p.getLongitude());
                if (exists) {
                    conflicts.add(Map.of(
                            "title", p.getTitle(),
                            "latitude", p.getLatitude(),
                            "longitude", p.getLongitude()
                    ));
                }
            }

            Map<String, Object> preview = new HashMap<>();
            preview.put("totalDetected", parsedPlaces.size());
            preview.put("conflicts", conflicts);
            preview.put("sample", parsedPlaces.stream().limit(5));

            return ResponseEntity.ok(preview);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Fichier invalide ou erreur lors du parsing",
                    "details", e.getMessage()
            ));
        }
    }

    /** Endpoint pour confirmer l'import et enregistrer les lieux */
    @PostMapping(value = "/confirm", consumes = "multipart/form-data")
    public ResponseEntity<?> confirmImport(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) Set<String> tags,
            @RequestParam(defaultValue = "ignore") String conflictStrategy,
            Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Long userId = (Long) authentication.getPrincipal();

            // Parse le fichier via ImportPlaceService
            List<Place> parsedPlaces = importPlaceService.parsePlacesFile(file);

            // Applique les tags optionnels
            if (tags != null && !tags.isEmpty()) {
                for (Place p : parsedPlaces) {
                    p.getTags().addAll(tags);
                }
            }

            // Enregistre les lieux en gérant les doublons selon la stratégie
            List<Place> savedPlaces = new ArrayList<>();
            for (Place p : parsedPlaces) {
                Optional<Place> existing = importPlaceService.findByCoordinates(p.getLatitude(), p.getLongitude());
                if (existing.isPresent()) {
                    switch (conflictStrategy.toLowerCase()) {
                        case "replace":
                            p.setId(existing.get().getId());
                            savedPlaces.add(importPlaceService.savePlace(p, userId));
                            break;
                        case "duplicate":
                            savedPlaces.add(importPlaceService.savePlace(p, userId));
                            break;
                        default: // ignore
                            break;
                    }
                } else {
                    savedPlaces.add(importPlaceService.savePlace(p, userId));
                }
            }

            // Mise à jour automatique des collections
            importPlaceService.updateCollections();

            return ResponseEntity.ok(Map.of(
                    "message", "Import terminé",
                    "importedCount", savedPlaces.size()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Erreur lors de l'import",
                    "details", e.getMessage()
            ));
        }
    }
}
