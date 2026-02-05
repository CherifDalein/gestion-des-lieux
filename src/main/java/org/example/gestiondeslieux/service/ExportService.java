package org.example.gestiondeslieux.service;

import org.example.gestiondeslieux.dto.GeoJsonFeature;
import org.example.gestiondeslieux.dto.GeoJsonFeatureCollection;
import org.example.gestiondeslieux.model.Place;
import org.example.gestiondeslieux.repository.PlaceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExportService {

    private PlaceRepository placeRepository;

    public ExportService(PlaceRepository placeRepository) {
        this.placeRepository = placeRepository;
    }

    public GeoJsonFeatureCollection exportAllPlacesToGeoJson() {
        List<Place> places = placeRepository.findAll();
        return buildGeoJsonFeatureCollection(places);
    }

    public GeoJsonFeatureCollection exportPlacesByTagToGeoJson(String tag) {
        List<Place> places = placeRepository.findByTagsContaining(tag);
        return buildGeoJsonFeatureCollection(places);
    }

    private GeoJsonFeatureCollection buildGeoJsonFeatureCollection(List<Place> places) {
        List<GeoJsonFeature> features = places.stream().map(place -> {
            Map<String, Object> geometry = Map.of(
                    "type", "Point",
                    "coordinates", List.of(place.getLongitude(), place.getLatitude())
            );

            Map<String, Object> properties = Map.of(
                    "id", place.getId(),
                    "title", place.getTitle(),
                    "description", place.getDescription(),
                    "tags", place.getTags()
            );

            return new GeoJsonFeature(geometry, properties);
        }).collect(Collectors.toList());

        return new GeoJsonFeatureCollection(features);
    }
    // Export de toutes les collecitons en GPX
    public String exportAllPlacesToGpx() {
        List<Place> places = placeRepository.findAll();
        return exportToGpx(places);
    }

    // Export par tag en GPX
    public String exportPlacesByTagToGpx(String tag) {
        List<Place> places = placeRepository.findAll().stream()
                .filter(place -> place.getTags() != null && place.getTags().contains(tag.toLowerCase()))
                .collect(Collectors.toList());

        return exportToGpx(places);
    }

    public String exportToGpx(List<Place> places) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                <?xml version="1.0" encoding="UTF-8" standalone="no" ?>
                <gpx version="1.1" creator="GestionDesLieux"
                     xmlns="http://www.topografix.com/GPX/1/1"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://www.topografix.com/GPX/1/1
                     http://www.topografix.com/GPX/1/1/gpx.xsd">
                """);

        for (Place place : places) {
            sb.append("<wpt lat=\"").append(place.getLatitude())
                    .append("\" lon=\"").append(place.getLongitude()).append("\">\n");

            if (place.getTitle() != null) {
                sb.append("<name>").append(escapeXml(place.getTitle())).append("</name>\n");
            }
            if (place.getDescription() != null) {
                sb.append("<desc>").append(escapeXml(place.getDescription())).append("</desc>\n");
            }
            if (place.getLongitude() != null) {
                sb.append("<lon>").append((place.getLongitude())).append("</lon>\n");
            }
            if (place.getLatitude() != null) {
                sb.append("<lat>").append((place.getLatitude())).append("</lat>\n");
            }
            if (place.getTags() != null && !place.getTags().isEmpty()) {
                sb.append("<tags>").append(String.join(",", place.getTags())).append("</tags>\n");
            }

            sb.append("</wpt>\n");
        }

        sb.append("</gpx>");
        return sb.toString();
    }

    private String escapeXml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
