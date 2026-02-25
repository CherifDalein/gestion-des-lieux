package org.example.gestiondeslieux.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ShareLocationResponse {
    private String token;
    private String url;
    private LocalDateTime expiresAt;
}
