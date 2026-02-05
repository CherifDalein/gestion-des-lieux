package org.example.gestiondeslieux.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class GeoJsonFeature {
    private String type = "Feature";
    private Map<String, Object> geometry;
    private Map<String, Object> properties;

    public GeoJsonFeature(Map<String, Object> geometry, Map<String, Object> properties) {
        this.geometry = geometry;
        this.properties = properties;
    }
}
