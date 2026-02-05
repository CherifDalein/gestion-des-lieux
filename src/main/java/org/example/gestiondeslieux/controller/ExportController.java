package org.example.gestiondeslieux.controller;

import org.example.gestiondeslieux.dto.GeoJsonFeatureCollection;
import org.example.gestiondeslieux.service.ExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exports")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    // Export de tous les lieux
    @GetMapping("/places/geojson")
    public ResponseEntity<GeoJsonFeatureCollection> exportAllPlaces() {
        GeoJsonFeatureCollection collection = exportService.exportAllPlacesToGeoJson();
        return ResponseEntity.ok(collection);
    }

    // Export par tag
    @GetMapping("/collections/{tag}/geojson")
    public ResponseEntity<GeoJsonFeatureCollection> exportByTag(@PathVariable String tag) {
        GeoJsonFeatureCollection collection = exportService.exportPlacesByTagToGeoJson(tag);
        return ResponseEntity.ok(collection);
    }

    // ---------------- GPX ----------------
    @GetMapping("/places/gpx")
    public ResponseEntity<String> exportAllPlacesGpx() throws Exception {
        String gpx = exportService.exportAllPlacesToGpx();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/gpx+xml")
                .body(gpx);
    }

    @GetMapping("/collections/{tag}/gpx")
    public ResponseEntity<String> exportByTagGpx(@PathVariable String tag) throws Exception {
        String gpx = exportService.exportPlacesByTagToGpx(tag);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/gpx+xml")
                .body(gpx);
    }
}
