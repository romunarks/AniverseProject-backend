// Excepción para operaciones duplicadas
package com.aniverse.backend.exception;

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}