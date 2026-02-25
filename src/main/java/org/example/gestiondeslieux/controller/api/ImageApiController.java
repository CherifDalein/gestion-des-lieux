package org.example.gestiondeslieux.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.gestiondeslieux.service.image.IPlaceImageService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/images")
@Tag(name = "Images", description = "Accès aux images uploadées")
@RequiredArgsConstructor
public class ImageApiController {

    private final IPlaceImageService placeImageService;

    @GetMapping("/{id}")
    @Operation(summary = "Télécharger une image")
    public ResponseEntity<byte[]> getImage(@PathVariable Long id) {
        byte[] bytes = placeImageService.getImageBytes(id);
        String contentType = placeImageService.getImageContentType(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(bytes);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une image")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id, Authentication auth) {
        placeImageService.deleteImage(id, (Long) auth.getPrincipal());
        return ResponseEntity.noContent().build();
    }
}
