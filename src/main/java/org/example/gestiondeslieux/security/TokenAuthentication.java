package org.example.gestiondeslieux.security;

import org.example.gestiondeslieux.model.AccessToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

public class TokenAuthentication implements Authentication {

    private final AccessToken accessToken;
    private boolean authenticated = true;

    public TokenAuthentication(AccessToken accessToken) {
        this.accessToken = accessToken;
    }

    public AccessToken getAccessToken() {
        return accessToken;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_TOKEN_ACCESS"));
    }

    @Override
    public Object getCredentials() {
        return accessToken.getToken();
    }

    @Override
    public Object getDetails() {
        return accessToken;
    }

    @Override
    public Object getPrincipal() {
        return accessToken.getToken();
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) {
        this.authenticated = isAuthenticated;
    }

    @Override
    public String getName() {
        return "token:" + accessToken.getToken();
    }
}
