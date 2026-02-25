package org.example.gestiondeslieux.repository;

import org.example.gestiondeslieux.model.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CollectionRepository extends JpaRepository<Collection, Long> {
    List<Collection> findByUserId(Long userId);
    Optional<Collection> findByUserIdAndTagFilterIsNull(Long userId);
    Optional<Collection> findByUserIdAndTagFilter(Long userId, String tagFilter);
    boolean existsByUserIdAndTagFilter(Long userId, String tagFilter);
    void deleteByUserIdAndTagFilter(Long userId, String tagFilter);
}
