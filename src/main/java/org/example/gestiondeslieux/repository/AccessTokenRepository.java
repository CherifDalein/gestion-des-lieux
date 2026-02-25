package org.example.gestiondeslieux.repository;

import org.example.gestiondeslieux.model.AccessToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AccessTokenRepository extends JpaRepository<AccessToken, Long> {

    Optional<AccessToken> findByToken(String token);

    List<AccessToken> findByCreatedByIdAndRevokedAtIsNull(Long userId);

    List<AccessToken> findByCreatedById(Long userId);

    @Query("SELECT t FROM AccessToken t WHERE t.token = :token AND t.revokedAt IS NULL " +
           "AND (t.expiresAt IS NULL OR t.expiresAt > :now)")
    Optional<AccessToken> findValidToken(@Param("token") String token, @Param("now") LocalDateTime now);
}
