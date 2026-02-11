package org.example.gestiondeslieux.service;

import org.example.gestiondeslieux.model.ShareToken;
import org.example.gestiondeslieux.repository.ShareTokenRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ShareTokenService {

    private final ShareTokenRepository repository;

    public ShareTokenService(ShareTokenRepository repository) {
        this.repository = repository;
    }

    // Lister les tokens actifs pour un utilisateur
    public List<ShareToken> getTokensByUser(Long userId) {
        return repository.findByUserIdAndRevokedFalse(userId);
    }

    // Filtrer par type de ressource
    public List<ShareToken> getTokensByUserAndType(Long userId, String resourceType) {
        return repository.findByUserIdAndResourceTypeAndRevokedFalse(userId, resourceType);
    }

    // Révoquer un token
    public Optional<ShareToken> revokeToken(UUID tokenId) {
        Optional<ShareToken> tokenOpt = repository.findById(tokenId);
        tokenOpt.ifPresent(token -> {
            token.setRevoked(true);
            repository.save(token);
        });
        return tokenOpt;
    }

    // Mettre à jour l’expiration
    public Optional<ShareToken> updateExpiration(UUID tokenId, LocalDateTime newExpiration) {
        Optional<ShareToken> tokenOpt = repository.findById(tokenId);
        tokenOpt.ifPresent(token -> {
            token.setExpiresAt(newExpiration);
            repository.save(token);
        });
        return tokenOpt;
    }

    // Créer un token
    public ShareToken createToken(Long userId, String resourceType, UUID resourceId, String permission, LocalDateTime expiresAt) {
        ShareToken token = new ShareToken();
        token.setUserId(userId);
        token.setResourceType(resourceType);
        token.setResourceId(resourceId);
        token.setPermission(permission);
        token.setCreatedAt(LocalDateTime.now());
        token.setExpiresAt(expiresAt);
        token.setToken(UUID.randomUUID().toString());
        return repository.save(token);
    }
}
