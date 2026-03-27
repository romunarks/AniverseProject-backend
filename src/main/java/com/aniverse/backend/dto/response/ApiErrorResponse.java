package com.aniverse.backend.dto.response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ApiErrorResponse extends AniverseResponse<Object> {
    private List<FieldError> errors;

    public ApiErrorResponse(String message) {
        super(false, message, null);
        this.errors = new ArrayList<>();
    }

    public ApiErrorResponse(String message, List<FieldError> errors) {
        super(false, message, null);
        this.errors = errors;
    }

    // Para errores de validación de campos
    public static ApiErrorResponse fromValidationErrors(Map<String, String> fieldErrors) {
        List<FieldError> errors = new ArrayList<>();
        fieldErrors.forEach((field, message) ->
                errors.add(new FieldError(field, message))
        );
        return new ApiErrorResponse("Error de validación", errors);
    }

    // Clase interna para representar errores de campo
    public static class FieldError {
        private String field;
        private String message;

        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        // Getters y Setters
        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    // Getters y Setters
    public List<FieldError> getErrors() {
        return errors;
    }

    public void setErrors(List<FieldError> errors) {
        this.errors = errors;
    }

    public void addError(String field, String message) {
        this.errors.add(new FieldError(field, message));
    }
}