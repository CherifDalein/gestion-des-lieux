package org.example.gestiondeslieux.repository;


import org.example.gestiondeslieux.model.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface PlaceRepository extends JpaRepository<Place, UUID> {
    @Query(value = """
        SELECT DISTINCT p.*
        FROM place p
        LEFT JOIN place_tags t ON p.id = t.place_id
        WHERE
          (:keyword IS NULL OR
           LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
           LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:tag IS NULL OR t.tag = :tag)
        AND (
            :lat IS NULL OR :lng IS NULL OR :radius IS NULL
            OR (
                6371 * acos(
                    cos(radians(:lat)) *
                    cos(radians(p.latitude)) *
                    cos(radians(p.longitude) - radians(:lng)) +
                    sin(radians(:lat)) *
                    sin(radians(p.latitude))
                )
            ) <= :radius
        )
        """, nativeQuery = true)
    List<Place> searchPlaces(
            @Param("keyword") String keyword,
            @Param("tag") String tag,
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("radius") Double radius
    );

    List<Place> findByTagsContaining(String tag);
}
