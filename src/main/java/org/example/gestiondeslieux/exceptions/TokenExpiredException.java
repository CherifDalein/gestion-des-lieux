package org.example.gestiondeslieux.exceptions;

public class TokenExpiredException extends RuntimeException {

    public TokenExpiredException(String token) {
        super(String.format("Token expiré : '%s'", token));
    }
}
