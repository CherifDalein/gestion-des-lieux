package org.example.gestiondeslieux.service.place;

import org.example.gestiondeslieux.controller.api.ImageApiController;
import org.example.gestiondeslieux.controller.api.PlaceApiController;
import org.example.gestiondeslieux.dto.ImportExecutionResult;
import org.example.gestiondeslieux.dto.PlaceDto;
import org.example.gestiondeslieux.dto.PlaceWithDistanceDto;
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
import org.hibernate.Hibernate;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Service
public class PlaceService implements IPlaceService {

    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;
    private final ICollectionService collectionService;
    private final IAccessTokenService accessTokenService;
    private final IExportService exportService;
    private final ModelMapper modelMapper;

    @Autowired
    public PlaceService(PlaceRepository placeRepository,
                        UserRepository userRepository,
                        @Lazy ICollectionService collectionService,
                        @Lazy IAccessTokenService accessTokenService,
                        @Lazy IExportService exportService,
                        ModelMapper modelMapper) {
        this.placeRepository = placeRepository;
        this.userRepository = userRepository;
        this.collectionService = collectionService;
        this.accessTokenService = accessTokenService;
        this.exportService = exportService;
        this.modelMapper = modelMapper;
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
    public ImportExecutionResult importPlaces(String content,
                                              ExportFormat format,
                                              Long userId,
                                              List<String> defaultTags,
                                              boolean skipDuplicates) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        List<Place> parsed = switch (format) {
            case GPX -> exportService.importFromGpx(content);
            case KML -> exportService.importFromKml(content);
            case GEOJSON -> exportService.importFromGeoJson(content);
        };

        ImportExecutionResult result = new ImportExecutionResult();
        Set<String> seenInPayload = new LinkedHashSet<>();

        for (Place candidate : parsed) {
            String title = normalizeTitle(candidate.getTitle());
            Double latitude = candidate.getLatitude();
            Double longitude = candidate.getLongitude();

            if (latitude == null || longitude == null) {
                result.getErrors().add("Lieu ignoré : coordonnées manquantes ou invalides");
                continue;
            }

            String duplicateKey = buildDuplicateKey(title, latitude, longitude);
            boolean duplicateInPayload = !seenInPayload.add(duplicateKey);
            boolean duplicateInDatabase = placeRepository
                    .existsByUserIdAndTitleIgnoreCaseAndLatitudeAndLongitude(userId, title, latitude, longitude);

            if (skipDuplicates && (duplicateInPayload || duplicateInDatabase)) {
                result.setSkipped(result.getSkipped() + 1);
                continue;
            }

            Place place = Place.builder()
                    .title(title)
                    .description(candidate.getDescription())
                    .latitude(latitude)
                    .longitude(longitude)
                    .imageUrl(candidate.getImageUrl())
                    .tags(mergeTags(candidate.getTags(), defaultTags))
                    .user(user)
                    .build();
            result.getImportedPlaces().add(placeRepository.save(place));
        }

        if (!result.getImportedPlaces().isEmpty()) {
            collectionService.syncCollectionsForUser(userId);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public PlaceDto convertToDto(Place place) {
        Hibernate.initialize(place.getTags());
        Hibernate.initialize(place.getUser());

        PlaceDto dto = modelMapper.map(place, PlaceDto.class);
        if (place.getUser() != null) {
            dto.setEmail(place.getUser().getEmail());
        }

        try {
            dto.add(linkTo(methodOn(PlaceApiController.class)
                    .getPlace(place.getId(), null, null, null))
                    .withSelfRel());
            dto.add(linkTo(methodOn(PlaceApiController.class)
                    .updatePlace(place.getId(), null, null))
                    .withRel("update"));
            dto.add(linkTo(methodOn(PlaceApiController.class)
                    .deletePlace(place.getId(), null))
                    .withRel("delete"));
            dto.add(linkTo(methodOn(PlaceApiController.class)
                    .addTag(place.getId(), "tag", null))
                    .withRel("addTag"));
            if (place.getImageUrl() != null && !place.getImageUrl().isBlank()) {
                Long imageId = extractImageId(place.getImageUrl());
                if (imageId != null) {
                    dto.add(linkTo(methodOn(ImageApiController.class).getImage(imageId, null, null)).withRel("image"));
                }
            }
        } catch (Exception ignored) {
        }

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public PlaceWithDistanceDto convertNearbyToDto(Object[] row) {
        PlaceWithDistanceDto dto = new PlaceWithDistanceDto();
        dto.setId(row[0] != null ? ((Number) row[0]).longValue() : null);
        dto.setTitle((String) row[1]);
        dto.setLatitude(row[2] != null ? ((Number) row[2]).doubleValue() : null);
        dto.setLongitude(row[3] != null ? ((Number) row[3]).doubleValue() : null);
        dto.setDistanceKm(row[4] != null ? ((Number) row[4]).doubleValue() : null);

        try {
            if (dto.getId() != null) {
                dto.add(linkTo(methodOn(PlaceApiController.class)
                        .getPlace(dto.getId(), null, null, null))
                        .withSelfRel());
            }
        } catch (Exception ignored) {
        }
        return dto;
    }

    private Long extractImageId(String imageUrl) {
        int idx = imageUrl.lastIndexOf('/');
        if (idx < 0 || idx == imageUrl.length() - 1) {
            return null;
        }
        try {
            return Long.parseLong(imageUrl.substring(idx + 1));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String normalizeTitle(String title) {
        return (title == null || title.isBlank()) ? "Imported" : title.trim();
    }

    private String buildDuplicateKey(String title, Double latitude, Double longitude) {
        return title.toLowerCase() + "|" + latitude + "|" + longitude;
    }

    private List<String> mergeTags(List<String> importedTags, List<String> defaultTags) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        addNormalizedTags(merged, importedTags);
        addNormalizedTags(merged, defaultTags);
        return new ArrayList<>(merged);
    }

    private void addNormalizedTags(Set<String> target, List<String> source) {
        if (source == null) {
            return;
        }
        for (String tag : source) {
            if (tag == null) {
                continue;
            }
            String normalized = tag.trim();
            if (!normalized.isEmpty()) {
                target.add(normalized);
            }
        }
    }
}
