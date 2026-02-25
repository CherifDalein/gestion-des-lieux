package org.example.gestiondeslieux.exceptions;

public class UnauthorizedAccessException extends RuntimeException {

    public UnauthorizedAccessException(String action) {
        super(String.format("Accès non autorisé pour l'action : '%s'", action));
    }
}
