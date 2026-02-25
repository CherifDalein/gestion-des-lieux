package org.example.gestiondeslieux.repository;

import org.example.gestiondeslieux.model.PlaceImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaceImageRepository extends JpaRepository<PlaceImage, Long> {
    List<PlaceImage> findByPlaceId(Long placeId);
}
