package org.example.gestiondeslieux.exceptions;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(AlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleAlreadyExists(AlreadyExistsException ex) {
        return buildError(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return buildError(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedAccessException ex) {
        return buildError(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage());
    }

    @ExceptionHandler(TokenInvalidException.class)
    public ResponseEntity<Map<String, Object>> handleTokenInvalid(TokenInvalidException ex) {
        return buildError(HttpStatus.UNAUTHORIZED, "TOKEN_INVALID", ex.getMessage());
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<Map<String, Object>> handleTokenExpired(TokenExpiredException ex) {
        return buildError(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", ex.getMessage());
    }

    @ExceptionHandler(InvalidFormatException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidFormat(InvalidFormatException ex) {
        return buildError(HttpStatus.BAD_REQUEST, "INVALID_FORMAT", ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidJson(HttpMessageNotReadableException ex) {
        return buildError(HttpStatus.BAD_REQUEST, "INVALID_JSON",
                "Le corps JSON de la requête est invalide");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParameter(MissingServletRequestParameterException ex) {
        return buildError(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER",
                "Paramètre manquant : " + ex.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return buildError(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                "Type invalide pour le paramètre : " + ex.getName());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fields.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        Map<String, Object> body = new HashMap<>();
        body.put("error", "VALIDATION_ERROR");
        body.put("fields", fields);
        body.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> fields = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            fields.put(violation.getPropertyPath().toString(), violation.getMessage());
        }
        Map<String, Object> body = new HashMap<>();
        body.put("error", "VALIDATION_ERROR");
        body.put("fields", fields);
        body.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return buildError(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                "Méthode HTTP non supportée pour cette route");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        return buildError(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                "Content-Type non supporté");
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Map<String, Object>> handleNotAcceptable(HttpMediaTypeNotAcceptableException ex) {
        return buildError(HttpStatus.NOT_ACCEPTABLE, "NOT_ACCEPTABLE",
                "Le client a demandé un format de réponse non supporté");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleFileTooLarge(MaxUploadSizeExceededException ex) {
        return buildError(HttpStatus.CONTENT_TOO_LARGE, "FILE_TOO_LARGE", "La taille du fichier dépasse la limite autorisée");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        log.error("Unhandled exception", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Une erreur interne est survenue");
    }

    private ResponseEntity<Map<String, Object>> buildError(HttpStatus status, String error, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", error);
        body.put("message", message);
        body.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }
}
