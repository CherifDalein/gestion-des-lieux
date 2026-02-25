package org.example.gestiondeslieux.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
public class CurrentLocationDto extends RepresentationModel<CurrentLocationDto> {
    private Long id;
    private Double latitude;
    private Double longitude;
    private Double accuracy;
    private LocalDateTime timestamp;
    private Boolean isShared;
    private LocalDateTime updatedAt;
}
