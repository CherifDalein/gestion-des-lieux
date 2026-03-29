package org.example.gestiondeslieux.service.token;

import lombok.RequiredArgsConstructor;
import org.example.gestiondeslieux.controller.api.AccessTokenApiController;
import org.example.gestiondeslieux.controller.api.CollectionApiController;
import org.example.gestiondeslieux.controller.api.PlaceApiController;
import org.example.gestiondeslieux.dto.AccessTokenDto;
import org.example.gestiondeslieux.enums.Permission;
import org.example.gestiondeslieux.enums.ResourceType;
import org.example.gestiondeslieux.exceptions.ResourceNotFoundException;
import org.example.gestiondeslieux.exceptions.TokenExpiredException;
import org.example.gestiondeslieux.exceptions.TokenInvalidException;
import org.example.gestiondeslieux.exceptions.UnauthorizedAccessException;
import org.example.gestiondeslieux.model.AccessToken;
import org.example.gestiondeslieux.model.Collection;
import org.example.gestiondeslieux.model.User;
import org.example.gestiondeslieux.repository.AccessTokenRepository;
import org.example.gestiondeslieux.repository.CollectionRepository;
import org.example.gestiondeslieux.repository.CurrentLocationRepository;
import org.example.gestiondeslieux.repository.PlaceRepository;
import org.example.gestiondeslieux.repository.UserRepository;
import org.example.gestiondeslieux.request.CreateTokenRequest;
import org.hibernate.Hibernate;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Service
@RequiredArgsConstructor
public class AccessTokenService implements IAccessTokenService {

    private final AccessTokenRepository accessTokenRepository;
    private final UserRepository userRepository;
    private final PlaceRepository placeRepository;
    private final CollectionRepository collectionRepository;
    private final CurrentLocationRepository currentLocationRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public AccessToken createToken(CreateTokenRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        ensureResourceOwnership(request.getResourceType(), request.getResourceId(), userId);
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
            if (!isResourceOwnedByUser(type, resourceId, token.getCreatedBy().getId())) return false;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean canReadPlace(String tokenValue, Long placeId) {
        try {
            AccessToken token = validateAndGet(tokenValue);
            if (token.getResourceType() == ResourceType.PLACE) {
                return token.getResourceId().equals(placeId)
                        && isResourceOwnedByUser(ResourceType.PLACE, placeId, token.getCreatedBy().getId());
            }
            if (token.getResourceType() != ResourceType.COLLECTION) {
                return false;
            }

            Collection collection = collectionRepository.findById(token.getResourceId())
                    .orElse(null);
            if (collection == null) {
                return false;
            }
            if (!collectionRepository.existsByIdAndUserId(token.getResourceId(), token.getCreatedBy().getId())) {
                return false;
            }
            String tagFilter = collection.getTagFilter();
            if (tagFilter == null || tagFilter.isBlank()) {
                return false;
            }

            return placeRepository.countMatchingSharedCollectionAccess(
                    placeId,
                    token.getCreatedBy().getId(),
                    tagFilter
            ) > 0;
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

    @Override
    @Transactional(readOnly = true)
    public AccessTokenDto convertToDto(AccessToken token) {
        Hibernate.initialize(token.getCreatedBy());

        AccessTokenDto dto = modelMapper.map(token, AccessTokenDto.class);
        dto.setIsValid(token.getRevokedAt() == null
                && (token.getExpiresAt() == null || token.getExpiresAt().isAfter(LocalDateTime.now())));

        try {
            dto.add(linkTo(methodOn(AccessTokenApiController.class).getToken(token.getId(), null)).withSelfRel());
            dto.add(linkTo(methodOn(AccessTokenApiController.class).revokeToken(token.getId(), null)).withRel("revoke"));
            if (token.getResourceType() == ResourceType.PLACE) {
                dto.add(linkTo(methodOn(PlaceApiController.class)
                        .getPlace(token.getResourceId(), token.getToken(), null, null))
                        .withRel("resource"));
            } else if (token.getResourceType() == ResourceType.COLLECTION) {
                dto.add(linkTo(methodOn(CollectionApiController.class)
                        .getCollection(token.getResourceId(), token.getToken(), null, null))
                        .withRel("resource"));
            }
        } catch (Exception ignored) {
        }

        return dto;
    }

    private void ensureResourceOwnership(ResourceType type, Long resourceId, Long userId) {
        if (!isResourceOwnedByUser(type, resourceId, userId)) {
            throw new ResourceNotFoundException(type.name(), "id", resourceId);
        }
    }

    private boolean isResourceOwnedByUser(ResourceType type, Long resourceId, Long userId) {
        return switch (type) {
            case PLACE -> placeRepository.existsByIdAndUserId(resourceId, userId);
            case COLLECTION -> collectionRepository.existsByIdAndUserId(resourceId, userId);
            case CURRENT_LOCATION -> currentLocationRepository.existsByIdAndUserId(resourceId, userId);
        };
    }
}
