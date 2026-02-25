package org.example.gestiondeslieux.repository;

import org.example.gestiondeslieux.model.CurrentLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CurrentLocationRepository extends JpaRepository<CurrentLocation, Long> {
    Optional<CurrentLocation> findByUserId(Long userId);
    Optional<CurrentLocation> findByShareToken(String shareToken);
}
