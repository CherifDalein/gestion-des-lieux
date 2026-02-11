package org.example.gestiondeslieux.repository;

import org.example.gestiondeslieux.model.ShareToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ShareTokenRepository extends JpaRepository<ShareToken, UUID> {

    List<ShareToken> findByUserIdAndRevokedFalse(Long userId);

    List<ShareToken> findByUserIdAndResourceTypeAndRevokedFalse(Long userId, String resourceType);

}
