package org.example.gestiondeslieux.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
public class PublicLocationDto extends RepresentationModel<PublicLocationDto> {
    private Double latitude;
    private Double longitude;
    private Double accuracy;
    private LocalDateTime timestamp;
    private LocalDateTime updatedAt;
    private Long ageSeconds;
}
