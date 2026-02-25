package org.example.gestiondeslieux.service.token;

import lombok.RequiredArgsConstructor;
import org.example.gestiondeslieux.enums.Permission;
import org.example.gestiondeslieux.enums.ResourceType;
import org.example.gestiondeslieux.exceptions.ResourceNotFoundException;
import org.example.gestiondeslieux.exceptions.TokenExpiredException;
import org.example.gestiondeslieux.exceptions.TokenInvalidException;
import org.example.gestiondeslieux.exceptions.UnauthorizedAccessException;
import org.example.gestiondeslieux.model.AccessToken;
import org.example.gestiondeslieux.model.User;
import org.example.gestiondeslieux.repository.AccessTokenRepository;
import org.example.gestiondeslieux.repository.UserRepository;
import org.example.gestiondeslieux.request.CreateTokenRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccessTokenService implements IAccessTokenService {

    private final AccessTokenRepository accessTokenRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public AccessToken createToken(CreateTokenRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        AccessToken token = AccessToken.builder()
                .token(UUID.randomUUID().toString())
                .resourceType(request.getResourceType())
                .resourceId(request.getResourceId())
                .permission(request.getPermission())
                .expiresAt(request.getExpiresAt())
                .label(request.getLabel())
                .createdBy(user)
                .accessCount(0L)
                .build();
        return accessTokenRepository.save(token);
    }

    @Override
    @Transactional
    public void revokeToken(Long tokenId, Long userId) {
        AccessToken token = accessTokenRepository.findById(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException("AccessToken", "id", tokenId));
        if (!token.getCreatedBy().getId().equals(userId)) {
            throw new UnauthorizedAccessException("révoquer ce token");
        }
        token.setRevokedAt(LocalDateTime.now());
        accessTokenRepository.save(token);
    }

    @Override
    @Transactional
    public void revokeTokenByValue(String tokenValue) {
        AccessToken token = accessTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new ResourceNotFoundException("AccessToken", "token", tokenValue));
        token.setRevokedAt(LocalDateTime.now());
        accessTokenRepository.save(token);
    }

    @Override
    public List<AccessToken> getTokensByUser(Long userId) {
        return accessTokenRepository.findByCreatedById(userId);
    }

    @Override
    public AccessToken getTokenById(Long id, Long userId) {
        AccessToken token = accessTokenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AccessToken", "id", id));
        if (!token.getCreatedBy().getId().equals(userId)) {
            throw new ResourceNotFoundException("AccessToken", "id", id);
        }
        return token;
    }

    @Override
    public AccessToken validateAndGet(String tokenValue) {
        AccessToken token = accessTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new TokenInvalidException(tokenValue));
        if (token.getRevokedAt() != null) {
            throw new TokenInvalidException(tokenValue);
        }
        if (token.getExpiresAt() != null && token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new TokenExpiredException(tokenValue);
        }
        return token;
    }

    @Override
    public boolean hasPermission(String tokenValue, ResourceType type, Long resourceId, Permission required) {
        try {
            AccessToken token = validateAndGet(tokenValue);
            if (token.getResourceType() != type) return false;
            if (!token.getResourceId().equals(resourceId)) return false;
            if (required == Permission.WRITE && token.getPermission() == Permission.READ) return false;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional
    public void incrementAccessCount(String tokenValue) {
        accessTokenRepository.findByToken(tokenValue).ifPresent(t -> {
            t.setAccessCount(t.getAccessCount() + 1);
            accessTokenRepository.save(t);
        });
    }

    @Override
    public AccessToken getTokenByValue(String tokenValue) {
        return accessTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new ResourceNotFoundException("AccessToken", "token", tokenValue));
    }
}
