package org.example.gestiondeslieux.security;

import org.example.gestiondeslieux.exceptions.UnauthorizedAccessException;
import org.springframework.security.core.Authentication;

public final class AuthContextUtils {

    private AuthContextUtils() {
    }

    public static Long requireUserId(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof Long uid) {
            return uid;
        }
        throw new UnauthorizedAccessException("authentification utilisateur requise");
    }

    public static Long userIdOrNull(Authentication auth) {
        return (auth != null && auth.getPrincipal() instanceof Long uid) ? uid : null;
    }

    public static String resolveToken(Authentication auth, String tokenParam) {
        if (tokenParam != null && !tokenParam.isBlank()) {
            return tokenParam;
        }
        if (auth instanceof TokenAuthentication tokenAuth) {
            return tokenAuth.getAccessToken().getToken();
        }
        if (auth != null && auth.getPrincipal() instanceof String principal) {
            return principal;
        }
        return null;
    }

    public static String requireUsername(Authentication auth) {
        if (auth != null
                && auth.getPrincipal() instanceof Long
                && auth.getCredentials() instanceof String username
                && !username.isBlank()) {
            return username;
        }
        throw new UnauthorizedAccessException("authentification utilisateur requise");
    }
}
