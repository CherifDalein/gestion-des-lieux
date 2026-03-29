package org.example.gestiondeslieux.repository;

import org.example.gestiondeslieux.model.Place;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    Page<Place> findByUserId(Long userId, Pageable pageable);
    long countByUserId(Long userId);

    Page<Place> findByUserIdAndTitleContainingIgnoreCaseOrUserIdAndDescriptionContainingIgnoreCase(
        Long userId1, String titleKeyword, Long userId2, String descKeyword, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Place p JOIN p.tags t WHERE p.user.id = :userId AND t = :tag")
    List<Place> findByUserIdAndTag(@Param("userId") Long userId, @Param("tag") String tag);

    @Query("SELECT DISTINCT p FROM Place p JOIN p.tags t WHERE p.user.id = :userId AND t = :tag")
    Page<Place> findByUserIdAndTagPaged(@Param("userId") Long userId, @Param("tag") String tag, Pageable pageable);

    @Query("SELECT COUNT(DISTINCT p) FROM Place p JOIN p.tags t " +
           "WHERE p.id = :placeId AND p.user.id = :userId AND t = :tag")
    long countMatchingSharedCollectionAccess(@Param("placeId") Long placeId,
                                             @Param("userId") Long userId,
                                             @Param("tag") String tag);

    @Query("SELECT DISTINCT t FROM Place p JOIN p.tags t WHERE p.user.id = :userId ORDER BY t")
    List<String> findDistinctTagsByUserId(@Param("userId") Long userId);

    @Query("SELECT MAX(p.updatedAt) FROM Place p WHERE p.user.id = :userId")
    Optional<LocalDateTime> findMaxUpdatedAtByUserId(@Param("userId") Long userId);

    @Query(value = """
        SELECT p.id, p.title, p.latitude, p.longitude, (6371 * acos(
            cos(radians(:lat)) * cos(radians(p.latitude))
            * cos(radians(p.longitude) - radians(:lon))
            + sin(radians(:lat)) * sin(radians(p.latitude))
        )) AS distance_km
        FROM places p
        WHERE p.user_id = :userId
        AND (6371 * acos(
            cos(radians(:lat)) * cos(radians(p.latitude))
            * cos(radians(p.longitude) - radians(:lon))
            + sin(radians(:lat)) * sin(radians(p.latitude))
        )) <= :radiusKm
        ORDER BY distance_km ASC
        """,
        countQuery = """
            SELECT count(*) FROM places p
            WHERE p.user_id = :userId
            AND (6371 * acos(
                cos(radians(:lat)) * cos(radians(p.latitude))
                * cos(radians(p.longitude) - radians(:lon))
                + sin(radians(:lat)) * sin(radians(p.latitude))
            )) <= :radiusKm
            """,
        nativeQuery = true)
    Page<Object[]> findNearby(@Param("lat") Double lat,
                               @Param("lon") Double lon,
                               @Param("radiusKm") Double radiusKm,
                               @Param("userId") Long userId,
                               Pageable pageable);

    boolean existsByIdAndUserId(Long id, Long userId);
    boolean existsByUserIdAndTitleIgnoreCaseAndLatitudeAndLongitude(
            Long userId, String title, Double latitude, Double longitude);
}
