package org.example.gestiondeslieux.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CurrentLocationDto {
    private Long id;
    private Double latitude;
    private Double longitude;
    private Double accuracy;
    private LocalDateTime timestamp;
    private Boolean isShared;
    private LocalDateTime updatedAt;
}
