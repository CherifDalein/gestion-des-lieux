package org.example.gestiondeslieux.dto;

import lombok.Data;
import org.example.gestiondeslieux.model.Place;

import java.util.ArrayList;
import java.util.List;

@Data
public class ImportExecutionResult {
    private List<Place> importedPlaces = new ArrayList<>();
    private int skipped;
    private List<String> errors = new ArrayList<>();
}
