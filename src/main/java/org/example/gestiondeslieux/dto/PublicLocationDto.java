package org.example.gestiondeslieux.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PublicLocationDto {
    private Double latitude;
    private Double longitude;
    private Double accuracy;
    private LocalDateTime timestamp;
    private LocalDateTime updatedAt;
    private Long ageSeconds;
}
