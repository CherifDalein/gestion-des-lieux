package org.example.gestiondeslieux.service.place;

import org.example.gestiondeslieux.enums.ExportFormat;
import org.example.gestiondeslieux.dto.PlaceDto;
import org.example.gestiondeslieux.dto.PlaceWithDistanceDto;
import org.example.gestiondeslieux.model.Place;
import org.example.gestiondeslieux.request.CreatePlaceRequest;
import org.example.gestiondeslieux.request.PlaceSearchRequest;
import org.example.gestiondeslieux.request.UpdatePlaceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IPlaceService {
    Place createPlace(CreatePlaceRequest request, Long userId);
    Place updatePlace(Long id, UpdatePlaceRequest request, Long userId);
    void deletePlace(Long id, Long userId);
    Place getPlaceById(Long id, Long userId);
    Place getPlaceByIdWithToken(Long id, Long userId, String accessToken);
    Page<Place> getPlacesByUser(Long userId, Pageable pageable);
    Page<Place> searchPlaces(PlaceSearchRequest request, Long userId, Pageable pageable);
    Page<Object[]> searchNearby(Double lat, Double lon, Double radiusKm, Long userId, Pageable pageable);
    List<Place> getPlacesByTag(String tag, Long userId);
    List<String> getAllTagsByUser(Long userId);
    void addTagToPlace(Long placeId, String tag, Long userId);
    void removeTagFromPlace(Long placeId, String tag, Long userId);
    String exportCollection(Long collectionId, ExportFormat format, Long userId);
    String exportPlaceById(Long placeId, ExportFormat format, Long userId);
    List<Place> importPlaces(String content, ExportFormat format, Long userId);
    PlaceDto convertToDto(Place place);
    PlaceWithDistanceDto convertNearbyToDto(Object[] row);
}
