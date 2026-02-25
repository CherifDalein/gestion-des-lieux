package org.example.gestiondeslieux.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.gestiondeslieux.dto.ImportResultDto;
import org.example.gestiondeslieux.dto.PlaceDto;
import org.example.gestiondeslieux.enums.ExportFormat;
import org.example.gestiondeslieux.model.Place;
import org.example.gestiondeslieux.service.export.IExportService;
import org.example.gestiondeslieux.service.place.IPlaceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/import")
@Tag(name = "Import", description = "Import GPX/KML/GeoJSON")
@RequiredArgsConstructor
public class ImportApiController {

    private final IPlaceService placeService;
    private final IExportService exportService;

    private Long userId(Authentication auth) { return (Long) auth.getPrincipal(); }

    private PlaceDto toDto(Place p) {
        PlaceDto dto = new PlaceDto();
        dto.setId(p.getId());
        dto.setTitle(p.getTitle());
        dto.setLatitude(p.getLatitude());
        dto.setLongitude(p.getLongitude());
        dto.setTags(p.getTags());
        return dto;
    }

    @PostMapping(consumes = "multipart/form-data")
    @Operation(summary = "Importer des lieux depuis un fichier")
    public ResponseEntity<ImportResultDto> importFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String format,
            Authentication auth) throws IOException {

        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        ExportFormat fmt = format != null
                ? ExportFormat.valueOf(format.toUpperCase())
                : exportService.detectFormat(content);

        List<Place> imported = placeService.importPlaces(content, fmt, userId(auth));
        ImportResultDto result = new ImportResultDto();
        result.setImported(imported.size());
        result.setSkipped(0);
        result.setErrors(List.of());
        result.setPlaces(imported.stream().map(this::toDto).toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping(consumes = "application/json")
    @Operation(summary = "Importer des lieux depuis JSON avec contenu brut")
    public ResponseEntity<ImportResultDto> importJson(
            @RequestBody org.example.gestiondeslieux.request.ImportRequest req,
            Authentication auth) {

        String content = req.getContent();
        ExportFormat fmt = req.getFormat() != null
                ? req.getFormat()
                : exportService.detectFormat(content);

        List<String> errors = new ArrayList<>();
        List<Place> imported;
        try {
            imported = placeService.importPlaces(content, fmt, userId(auth));
        } catch (Exception e) {
            errors.add(e.getMessage());
            imported = List.of();
        }

        ImportResultDto result = new ImportResultDto();
        result.setImported(imported.size());
        result.setSkipped(0);
        result.setErrors(errors);
        result.setPlaces(imported.stream().map(this::toDto).toList());
        return ResponseEntity.ok(result);
    }
}
