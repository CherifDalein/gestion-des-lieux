package org.example.gestiondeslieux.service;

import org.example.gestiondeslieux.dto.CreatePlaceRequest;
import org.example.gestiondeslieux.model.Place;
import org.example.gestiondeslieux.repository.PlaceRepository;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service // CORRECTION : Suppression de @Data
public class PlaceService {

    private final PlaceRepository placeRepository;

    public PlaceService(PlaceRepository placeRepository) {
        this.placeRepository = placeRepository;
    }

    public Place createPlace(CreatePlaceRequest request, Long userId) {
        if (request.getLatitude() < -90 || request.getLatitude() > 90 ||
                request.getLongitude() < -180 || request.getLongitude() > 180) {
            throw new IllegalArgumentException("Coordonnées GPS invalides");
        }

        Place place = new Place();
        place.setLatitude(request.getLatitude());
        place.setLongitude(request.getLongitude());
        place.setTitle(request.getTitle());
        place.setDescription(request.getDescription());
        place.setUserId(userId);

        if (request.getTags() != null) {
            place.setTags(
                    request.getTags().stream()
                            .map(String::toLowerCase)
                            .map(String::trim)
                            .collect(Collectors.toSet())
            );
        }

        return placeRepository.save(place);
    }
}