package org.example.gestiondeslieux.repository;

import org.example.gestiondeslieux.model.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CollectionRepository extends JpaRepository<Collection, Long> {
    boolean existsByIdAndUserId(Long id, Long userId);
    List<Collection> findByUserId(Long userId);
    long countByUserId(Long userId);
    void deleteByUserIdAndTagFilterIsNull(Long userId);
    boolean existsByUserIdAndTagFilter(Long userId, String tagFilter);
    void deleteByUserIdAndTagFilter(Long userId, String tagFilter);
}
