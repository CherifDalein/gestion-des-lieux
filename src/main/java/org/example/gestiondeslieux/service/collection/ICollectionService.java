package org.example.gestiondeslieux.service.collection;

import org.example.gestiondeslieux.dto.CollectionDto;
import org.example.gestiondeslieux.model.Collection;
import org.example.gestiondeslieux.model.Place;
import org.example.gestiondeslieux.request.CreateCollectionRequest;
import org.example.gestiondeslieux.request.UpdateCollectionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ICollectionService {
    Collection createCollection(CreateCollectionRequest request, Long userId);
    Collection updateCollection(Long id, UpdateCollectionRequest request, Long userId);
    void deleteCollection(Long id, Long userId);
    Collection getCollectionById(Long id, Long userId);
    List<Collection> getCollectionsByUser(Long userId);
    Page<Place> getPlacesInCollection(Long collectionId, Long userId, Pageable pageable);
    void syncCollectionsForUser(Long userId);
    CollectionDto convertToDto(Collection collection);
}
