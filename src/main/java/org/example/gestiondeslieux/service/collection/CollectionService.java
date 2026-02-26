package org.example.gestiondeslieux.service.collection;

import lombok.RequiredArgsConstructor;
import org.example.gestiondeslieux.controller.api.CollectionApiController;
import org.example.gestiondeslieux.dto.CollectionDto;
import org.example.gestiondeslieux.exceptions.AlreadyExistsException;
import org.example.gestiondeslieux.exceptions.ResourceNotFoundException;
import org.example.gestiondeslieux.model.Collection;
import org.example.gestiondeslieux.model.Place;
import org.example.gestiondeslieux.model.User;
import org.example.gestiondeslieux.repository.CollectionRepository;
import org.example.gestiondeslieux.repository.PlaceRepository;
import org.example.gestiondeslieux.repository.UserRepository;
import org.example.gestiondeslieux.request.CreateCollectionRequest;
import org.example.gestiondeslieux.request.UpdateCollectionRequest;
import org.hibernate.Hibernate;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Service
@RequiredArgsConstructor
public class CollectionService implements ICollectionService {

    private final CollectionRepository collectionRepository;
    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public Collection createCollection(CreateCollectionRequest request, Long userId) {
        ensureUniqueTagFilterOnCreate(userId, request.getTagFilter());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        Collection collection = Collection.builder()
                .name(request.getName())
                .tagFilter(request.getTagFilter())
                .user(user)
                .isShared(false)
                .build();
        return collectionRepository.save(collection);
    }

    @Override
    @Transactional
    public Collection updateCollection(Long id, UpdateCollectionRequest request, Long userId) {
        Collection collection = getCollectionById(id, userId);
        if (request.getName() != null) collection.setName(request.getName());
        if (request.getTagFilter() != null) {
            ensureUniqueTagFilterOnUpdate(userId, collection, request.getTagFilter());
            collection.setTagFilter(request.getTagFilter());
        }
        return collectionRepository.save(collection);
    }

    @Override
    @Transactional
    public void deleteCollection(Long id, Long userId) {
        Collection collection = getCollectionById(id, userId);
        collectionRepository.delete(collection);
    }

    @Override
    public Collection getCollectionById(Long id, Long userId) {
        Collection collection = collectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collection", "id", id));
        if (!collection.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Collection", "id", id);
        }
        return collection;
    }

    @Override
    public List<Collection> getCollectionsByUser(Long userId) {
        return collectionRepository.findByUserId(userId);
    }

    @Override
    public Page<Place> getPlacesInCollection(Long collectionId, Long userId, Pageable pageable) {
        Collection collection = getCollectionById(collectionId, userId);
        if (collection.getTagFilter() == null) {
            return placeRepository.findByUserId(userId, pageable);
        }
        return placeRepository.findByUserIdAndTagPaged(userId, collection.getTagFilter(), pageable);
    }

    @Override
    @Transactional
    public void syncCollectionsForUser(Long userId) {
        getOrCreateAllPlacesCollection(userId);
        List<String> tags = placeRepository.findDistinctTagsByUserId(userId);
        List<Collection> existing = collectionRepository.findByUserId(userId);

        // Create missing collections for new tags
        for (String tag : tags) {
            boolean exists = existing.stream()
                    .anyMatch(c -> tag.equals(c.getTagFilter()));
            if (!exists) {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
                Collection c = Collection.builder()
                        .name(tag)
                        .tagFilter(tag)
                        .user(user)
                        .isShared(false)
                        .build();
                collectionRepository.save(c);
            }
        }

        // Remove collections whose tag no longer exists
        for (Collection c : existing) {
            if (c.getTagFilter() != null && !tags.contains(c.getTagFilter())) {
                collectionRepository.delete(c);
            }
        }
    }

    @Override
    @Transactional
    public Collection getOrCreateAllPlacesCollection(Long userId) {
        return collectionRepository.findByUserIdAndTagFilterIsNull(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
                    Collection c = Collection.builder()
                            .name("Tous les lieux")
                            .tagFilter(null)
                            .user(user)
                            .isShared(false)
                            .build();
                    return collectionRepository.save(c);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public CollectionDto convertToDto(Collection collection) {
        Hibernate.initialize(collection.getUser());

        CollectionDto dto = modelMapper.map(collection, CollectionDto.class);
        dto.setEmail(collection.getUser().getEmail());
        dto.setPlaceCount(getPlacesInCollection(collection.getId(), collection.getUser().getId(),
                Pageable.ofSize(1)).getTotalElements());

        try {
            dto.add(linkTo(methodOn(CollectionApiController.class)
                    .getCollection(collection.getId(), null, null, null))
                    .withSelfRel());
            dto.add(linkTo(methodOn(CollectionApiController.class)
                    .getPlaces(collection.getId(), null, null, null))
                    .withRel("places"));
            dto.add(linkTo(methodOn(CollectionApiController.class)
                    .shareCollection(collection.getId(), null, null))
                    .withRel("share"));
        } catch (Exception ignored) {
        }

        return dto;
    }

    private void ensureUniqueTagFilterOnCreate(Long userId, String tagFilter) {
        if (tagFilter == null) {
            if (collectionRepository.existsByUserIdAndTagFilterIsNull(userId)) {
                throw new AlreadyExistsException("Collection", "tagFilter", "null");
            }
            return;
        }
        if (collectionRepository.existsByUserIdAndTagFilter(userId, tagFilter)) {
            throw new AlreadyExistsException("Collection", "tagFilter", tagFilter);
        }
    }

    private void ensureUniqueTagFilterOnUpdate(Long userId, Collection current, String nextTagFilter) {
        if (nextTagFilter.equals(current.getTagFilter())) {
            return;
        }
        if (collectionRepository.existsByUserIdAndTagFilter(userId, nextTagFilter)) {
            throw new AlreadyExistsException("Collection", "tagFilter", nextTagFilter);
        }
    }
}
