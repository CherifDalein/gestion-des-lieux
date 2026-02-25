package org.example.gestiondeslieux.request;

import lombok.Data;

@Data
public class PlaceSearchRequest {
    private String q;
    private String tag;
    private Double lat;
    private Double lon;
    private Double radiusKm = 5.0;
}
