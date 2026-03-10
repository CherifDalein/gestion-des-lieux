package org.example.gestiondeslieux.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ShareLocationResponse {
    @Schema(example = "loc_share_abc123")
    private String token;
    @Schema(example = "http://localhost:8080/api/location/public/loc_share_abc123")
    private String url;
    @Schema(example = "2026-12-31T23:59:59")
    private LocalDateTime expiresAt;
}
