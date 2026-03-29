package org.example.gestiondeslieux.service.token;

import org.example.gestiondeslieux.dto.AccessTokenDto;
import org.example.gestiondeslieux.enums.Permission;
import org.example.gestiondeslieux.enums.ResourceType;
import org.example.gestiondeslieux.model.AccessToken;
import org.example.gestiondeslieux.request.CreateTokenRequest;

import java.util.List;

public interface IAccessTokenService {
    AccessToken createToken(CreateTokenRequest request, Long userId);
    void revokeToken(Long tokenId, Long userId);
    void revokeTokenByValue(String token);
    List<AccessToken> getTokensByUser(Long userId);
    AccessToken getTokenById(Long id, Long userId);
    AccessToken validateAndGet(String token);
    boolean hasPermission(String token, ResourceType type, Long resourceId, Permission required);
    boolean canReadPlace(String token, Long placeId);
    void incrementAccessCount(String token);
    AccessToken getTokenByValue(String token);
    AccessTokenDto convertToDto(AccessToken token);
}
