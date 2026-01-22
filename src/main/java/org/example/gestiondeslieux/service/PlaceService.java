package org.example.gestiondeslieux.service;

import org.example.gestiondeslieux.dto.CreatePlaceRequest;
import org.example.gestiondeslieux.model.Place;
import org.example.gestiondeslieux.repository.PlaceRepository;
import org.springframework.stereotype.Service;
import lombok.Data;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Service
public class PlaceService {

    private final PlaceRepository placeRepository;

    public PlaceService(PlaceRepository placeRepository) {
        this.placeRepository = placeRepository;
    }

    public Place createPlace(CreatePlaceRequest request) {

        // Validation GPS simple
        if (request.getLatitude() < -90 || request.getLatitude() > 90 ||
                request.getLongitude() < -180 || request.getLongitude() > 180) {
            throw new IllegalArgumentException("Coordonnées GPS invalides");
        }

        Place place = new Place();
        place.setLatitude(request.getLatitude());
        place.setLongitude(request.getLongitude());
        place.setTitle(request.getTitle());
        place.setDescription(request.getDescription());
        place.setUserId(UUID.fromString("411a8d32-0661-4c9a-8daf-54a10fc49804"));

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

    public List<Place> getAllPlaces() {
        return placeRepository.findAll();
    }

    public Optional<Place> getPlaceById(UUID id) {
        return placeRepository.findById(id);
    }

    public Place updatePlace(UUID id, CreatePlaceRequest request) {
        return placeRepository.findById(id).map(existingPlace -> {
            existingPlace.setTitle(request.getTitle());
            existingPlace.setDescription(request.getDescription());
            existingPlace.setLatitude(request.getLatitude());
            existingPlace.setLongitude(request.getLongitude());
            existingPlace.setTags(request.getTags());
            return placeRepository.save(existingPlace);
        }).orElseThrow(()-> new RuntimeException("Lieu non trouvé"));
    }


    public boolean deletePlace(UUID id) {
        if (placeRepository.existsById(id)) {
            placeRepository.deleteById(id);
            return true;
        }
        return false;
    }

}

