package org.example.gestiondeslieux.service;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TokenService {

    public UUID getIdFromToken(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new SecurityException("Token invalide");
        }

        String rawToken = token.replace("Bearer ", "");

        return UUID.fromString(rawToken);
    }
}
