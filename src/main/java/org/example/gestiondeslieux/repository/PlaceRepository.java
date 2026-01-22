package org.example.gestiondeslieux.repository;


import org.example.gestiondeslieux.model.Place;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PlaceRepository extends JpaRepository<Place, UUID> {
}
