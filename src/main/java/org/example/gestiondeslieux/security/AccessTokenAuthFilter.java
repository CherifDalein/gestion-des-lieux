package org.example.gestiondeslieux.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.gestiondeslieux.model.AccessToken;
import org.example.gestiondeslieux.service.token.IAccessTokenService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class AccessTokenAuthFilter extends OncePerRequestFilter {

    private final IAccessTokenService accessTokenService;

    public AccessTokenAuthFilter(IAccessTokenService accessTokenService) {
        this.accessTokenService = accessTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() != null &&
                SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        String tokenValue = request.getParameter("token");
        if (tokenValue == null) {
            tokenValue = request.getHeader("X-Access-Token");
        }

        if (tokenValue != null) {
            try {
                AccessToken accessToken = accessTokenService.validateAndGet(tokenValue);
                accessTokenService.incrementAccessCount(tokenValue);
                SecurityContextHolder.getContext()
                        .setAuthentication(new TokenAuthentication(accessToken));
            } catch (Exception ignored) {
            }
        }

        filterChain.doFilter(request, response);
    }
}
