package org.example.gestiondeslieux.exceptions;

public class InvalidFormatException extends RuntimeException {

    public InvalidFormatException(String format, String detail) {
        super(String.format("Format '%s' invalide : %s", format, detail));
    }
}
