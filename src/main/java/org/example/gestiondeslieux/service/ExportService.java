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
}
