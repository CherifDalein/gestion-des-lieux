package org.example.gestiondeslieux.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class GeoJsonFeatureCollection {
    private String type = "FeatureCollection";
    private List<GeoJsonFeature> features;

    public GeoJsonFeatureCollection(List<GeoJsonFeature> features) {
        this.features = features;
    }
}
