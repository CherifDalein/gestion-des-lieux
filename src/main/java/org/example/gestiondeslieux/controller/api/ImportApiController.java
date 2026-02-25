package org.example.gestiondeslieux.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.gestiondeslieux.dto.ImportResultDto;
import org.example.gestiondeslieux.enums.ExportFormat;
import org.example.gestiondeslieux.exceptions.InvalidFormatException;
import org.example.gestiondeslieux.model.Place;
import org.example.gestiondeslieux.service.export.IExportService;
import org.example.gestiondeslieux.response.ApiResponse;
import org.example.gestiondeslieux.security.AuthContextUtils;
import org.example.gestiondeslieux.service.place.IPlaceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/import")
@Tag(name = "Import", description = "Import GPX/KML/GeoJSON")
@RequiredArgsConstructor
public class ImportApiController {

    private final IPlaceService placeService;
    private final IExportService exportService;

    @PostMapping(consumes = "multipart/form-data")
    @Operation(summary = "Importer des lieux depuis un fichier")
    public ResponseEntity<ApiResponse<ImportResultDto>> importFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String format,
            Authentication auth) throws IOException {

        Long userId = AuthContextUtils.requireUserId(auth);
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        ExportFormat fmt = resolveFormat(format, content);

        List<Place> imported = placeService.importPlaces(content, fmt, userId);
        ImportResultDto result = new ImportResultDto();
        result.setImported(imported.size());
        result.setSkipped(0);
        result.setErrors(List.of());
        result.setPlaces(imported.stream().map(placeService::convertToDto).toList());
        return ResponseEntity.ok(new ApiResponse<>("Import fichier terminé avec succès", result));
    }

    @PostMapping(consumes = "application/json")
    @Operation(summary = "Importer des lieux depuis JSON avec contenu brut")
    public ResponseEntity<ApiResponse<ImportResultDto>> importJson(
            @RequestBody org.example.gestiondeslieux.request.ImportRequest req,
            Authentication auth) {

        Long userId = AuthContextUtils.requireUserId(auth);
        String content = req.getContent();
        ExportFormat fmt = req.getFormat() != null
                ? req.getFormat()
                : exportService.detectFormat(content);

        List<Place> imported = placeService.importPlaces(content, fmt, userId);

        ImportResultDto result = new ImportResultDto();
        result.setImported(imported.size());
        result.setSkipped(0);
        result.setErrors(List.of());
        result.setPlaces(imported.stream().map(placeService::convertToDto).toList());
        return ResponseEntity.ok(new ApiResponse<>("Import JSON terminé avec succès", result));
    }

    private ExportFormat resolveFormat(String format, String content) {
        if (format == null || format.isBlank()) {
            return exportService.detectFormat(content);
        }
        try {
            return ExportFormat.valueOf(format.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidFormatException(format, "format non supporte");
        }
    }
}
