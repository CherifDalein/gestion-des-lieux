package org.example.gestiondeslieux.repository;

import org.example.gestiondeslieux.model.Place;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlaceRepository extends JpaRepository<Place, UUID> {

    // Vérifie si un lieu existe avec ces coordonnées
    boolean existsByLatitudeAndLongitude(Double latitude, Double longitude);

    // Récupère un lieu par coordonnées
    Optional<Place> findByLatitudeAndLongitude(Double latitude, Double longitude);
}
