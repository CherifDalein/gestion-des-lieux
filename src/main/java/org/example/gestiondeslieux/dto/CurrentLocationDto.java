package org.example.gestiondeslieux.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
public class CurrentLocationDto extends RepresentationModel<CurrentLocationDto> {
    @Schema(example = "7")
    private Long id;
    @Schema(example = "48.8566")
    private Double latitude;
    @Schema(example = "2.3522")
    private Double longitude;
    @Schema(example = "12.5")
    private Double accuracy;
    @Schema(example = "2026-02-25T22:30:00")
    private LocalDateTime timestamp;
    @Schema(example = "true")
    private Boolean isShared;
    @Schema(example = "2026-02-25T22:31:00")
    private LocalDateTime updatedAt;
}
