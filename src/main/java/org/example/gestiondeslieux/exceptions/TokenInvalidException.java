package org.example.gestiondeslieux.exceptions;

public class TokenInvalidException extends RuntimeException {

    public TokenInvalidException(String token) {
        super(String.format("Token invalide : '%s'", token));
    }
}
