package org.example.gestiondeslieux.exceptions;

public class AlreadyExistsException extends RuntimeException {

    public AlreadyExistsException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s existe déjà avec %s : '%s'", resourceName, fieldName, fieldValue));
    }
}
