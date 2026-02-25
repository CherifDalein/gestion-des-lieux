package org.example.gestiondeslieux.service.place;

import org.example.gestiondeslieux.enums.ExportFormat;
import org.example.gestiondeslieux.enums.Permission;
import org.example.gestiondeslieux.enums.ResourceType;
import org.example.gestiondeslieux.exceptions.ResourceNotFoundException;
import org.example.gestiondeslieux.model.Place;
import org.example.gestiondeslieux.model.User;
import org.example.gestiondeslieux.repository.PlaceRepository;
import org.example.gestiondeslieux.repository.UserRepository;
import org.example.gestiondeslieux.request.CreatePlaceRequest;
import org.example.gestiondeslieux.request.PlaceSearchRequest;
import org.example.gestiondeslieux.request.UpdatePlaceRequest;
import org.example.gestiondeslieux.service.collection.ICollectionService;
import org.example.gestiondeslieux.service.export.IExportService;
import org.example.gestiondeslieux.service.token.IAccessTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class PlaceService implements IPlaceService {

    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;
    private final ICollectionService collectionService;
    private final IAccessTokenService accessTokenService;
    private final IExportService exportService;

    @Autowired
    public PlaceService(PlaceRepository placeRepository,
                        UserRepository userRepository,
                        @Lazy ICollectionService collectionService,
                        @Lazy IAccessTokenService accessTokenService,
                        @Lazy IExportService exportService) {
        this.placeRepository = placeRepository;
        this.userRepository = userRepository;
        this.collectionService = collectionService;
        this.accessTokenService = accessTokenService;
        this.exportService = exportService;
    }

    @Override
    @Transactional
    public Place createPlace(CreatePlaceRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        Place place = Place.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .imageUrl(request.getImageUrl())
                .tags(request.getTags() != null ? new ArrayList<>(request.getTags()) : new ArrayList<>())
                .user(user)
                .build();
        Place saved = placeRepository.save(place);
        collectionService.syncCollectionsForUser(userId);
        return saved;
    }

    @Override
    @Transactional
    public Place updatePlace(Long id, UpdatePlaceRequest request, Long userId) {
        Place place = getPlaceById(id, userId);
        if (request.getTitle() != null) place.setTitle(request.getTitle());
        if (request.getDescription() != null) place.setDescription(request.getDescription());
        if (request.getLatitude() != null) place.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) place.setLongitude(request.getLongitude());
        if (request.getImageUrl() != null) place.setImageUrl(request.getImageUrl());
        if (request.getTags() != null) place.setTags(new ArrayList<>(request.getTags()));
        Place saved = placeRepository.save(place);
        collectionService.syncCollectionsForUser(userId);
        return saved;
    }

    @Override
    @Transactional
    public void deletePlace(Long id, Long userId) {
        Place place = getPlaceById(id, userId);
        placeRepository.delete(place);
        collectionService.syncCollectionsForUser(userId);
    }

    @Override
    public Place getPlaceById(Long id, Long userId) {
        Place place = placeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Place", "id", id));
        if (!place.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Place", "id", id);
        }
        return place;
    }

    @Override
    public Place getPlaceByIdWithToken(Long id, Long userId, String accessToken) {
        Place place = placeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Place", "id", id));
        if (userId != null && place.getUser().getId().equals(userId)) return place;
        if (accessToken != null && accessTokenService.hasPermission(
                accessToken, ResourceType.PLACE, id, Permission.READ)) {
            accessTokenService.incrementAccessCount(accessToken);
            return place;
        }
        throw new ResourceNotFoundException("Place", "id", id);
    }

    @Override
    public Page<Place> getPlacesByUser(Long userId, Pageable pageable) {
        return placeRepository.findByUserId(userId, pageable);
    }

    @Override
    public Page<Place> searchPlaces(PlaceSearchRequest request, Long userId, Pageable pageable) {
        String keyword = request.getQ() != null ? request.getQ() : "";
        return placeRepository
                .findByUserIdAndTitleContainingIgnoreCaseOrUserIdAndDescriptionContainingIgnoreCase(
                        userId, keyword, userId, keyword, pageable);
    }

    @Override
    public Page<Object[]> searchNearby(Double lat, Double lon, Double radiusKm, Long userId, Pageable pageable) {
        return placeRepository.findNearby(lat, lon, radiusKm, userId, pageable);
    }

    @Override
    public List<Place> getPlacesByTag(String tag, Long userId) {
        return placeRepository.findByUserIdAndTag(userId, tag);
    }

    @Override
    public List<String> getAllTagsByUser(Long userId) {
        return placeRepository.findDistinctTagsByUserId(userId);
    }

    @Override
    @Transactional
    public void addTagToPlace(Long placeId, String tag, Long userId) {
        Place place = getPlaceById(placeId, userId);
        if (!place.getTags().contains(tag)) {
            place.getTags().add(tag);
            placeRepository.save(place);
            collectionService.syncCollectionsForUser(userId);
        }
    }

    @Override
    @Transactional
    public void removeTagFromPlace(Long placeId, String tag, Long userId) {
        Place place = getPlaceById(placeId, userId);
        place.getTags().remove(tag);
        placeRepository.save(place);
        collectionService.syncCollectionsForUser(userId);
    }

    @Override
    public String exportCollection(Long collectionId, ExportFormat format, Long userId) {
        List<Place> places = collectionService
                .getPlacesInCollection(collectionId, userId, Pageable.unpaged())
                .getContent();
        String name = collectionService.getCollectionById(collectionId, userId).getName();
        return switch (format) {
            case GPX     -> exportService.exportToGpx(places, name);
            case KML     -> exportService.exportToKml(places, name);
            case GEOJSON -> exportService.exportToGeoJson(places, name);
        };
    }

    @Override
    public String exportPlaceById(Long placeId, ExportFormat format, Long userId) {
        Place place = getPlaceById(placeId, userId);
        return switch (format) {
            case GPX     -> exportService.exportToGpx(List.of(place), place.getTitle());
            case KML     -> exportService.exportToKml(List.of(place), place.getTitle());
            case GEOJSON -> exportService.exportToGeoJson(List.of(place), place.getTitle());
        };
    }

    @Override
    @Transactional
    public List<Place> importPlaces(String content, ExportFormat format, Long userId) {
        return switch (format) {
            case GPX     -> exportService.importFromGpx(content, userId);
            case KML     -> exportService.importFromKml(content, userId);
            case GEOJSON -> exportService.importFromGeoJson(content, userId);
        };
    }
}
