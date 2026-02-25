package org.example.gestiondeslieux.dto;

import lombok.Data;
import java.util.List;

@Data
public class ImportResultDto {
    private int imported;
    private int skipped;
    private List<String> errors;
    private List<PlaceDto> places;
}
