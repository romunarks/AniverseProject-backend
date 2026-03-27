package com.aniverse.backend.exception;

import com.aniverse.backend.dto.response.AniverseResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Manejo de errores de validación (cuando los @Valid fallan)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AniverseResponse<Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        AniverseResponse<Object> response = new AniverseResponse<>(false, "Error de validación", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // Manejo de recursos no encontrados
    @ExceptionHandler({NoSuchElementException.class, ResourceNotFoundException.class})
    public ResponseEntity<AniverseResponse<Object>> handleNoSuchElementException(
            Exception e) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new AniverseResponse<>(false, e.getMessage(), null));
    }

    // Manejo de argumentos ilegales
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<AniverseResponse<Object>> handleIllegalArgumentException(
            IllegalArgumentException e) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new AniverseResponse<>(false, e.getMessage(), null));
    }

    // Manejo de recursos duplicados
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<AniverseResponse<Object>> handleDuplicateResourceException(
            DuplicateResourceException e) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new AniverseResponse<>(false, e.getMessage(), null));
    }

    // Manejo de acceso denegado
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<AniverseResponse<Object>> handleAccessDeniedException(
            AccessDeniedException e) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new AniverseResponse<>(false, "No tienes permisos para realizar esta acción", null));
    }

    // Manejo de excepciones generales
    @ExceptionHandler(Exception.class)
    public ResponseEntity<AniverseResponse<Object>> handleGeneralException(
            Exception e) {

        // Loguear el error completo para depuración
        e.printStackTrace();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new AniverseResponse<>(false, "Error interno del servidor: " + e.getMessage(), null));
    }
    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<AniverseResponse<Object>> handleHttpClientErrorException(HttpClientErrorException ex) {
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(AniverseResponse.error("Error en API externa: " + ex.getMessage()));
    }
}