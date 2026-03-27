package com.aniverse.backend.dto.response;

import java.time.LocalDateTime;

public class AniverseResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private Long timestamp = System.currentTimeMillis();

    // Constructor vacío
    public AniverseResponse() {
    }

    // Constructor con mensaje
    public AniverseResponse(String message) {
        this.success = true;
        this.message = message;
    }

    // Constructor con mensaje y datos
    public AniverseResponse(String message, T data) {
        this.success = true;
        this.message = message;
        this.data = data;
    }

    // Constructor completo
    public AniverseResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    // Constructor privado para el builder
    private AniverseResponse(Builder<T> builder) {
        this.success = builder.success;
        this.message = builder.message;
        this.data = builder.data;
        this.timestamp = builder.timestamp != null ? builder.timestamp : System.currentTimeMillis();
    }

    // Métodos estáticos de fábrica
    public static <T> AniverseResponse<T> success(T data) {
        return new AniverseResponse<>(true, "La operación se completó con éxito", data);
    }

    public static <T> AniverseResponse<T> success(String message, T data) {
        return new AniverseResponse<>(true, message, data);
    }

    public static <T> AniverseResponse<T> success(String message) {
        return new AniverseResponse<>(true, message, null);
    }

    public static <T> AniverseResponse<T> error(String message) {
        return new AniverseResponse<>(false, message, null);
    }

    // Método builder estático
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    // ================================
    // CLASE BUILDER INTERNA ESTÁTICA
    // ================================
    public static class Builder<T> {
        private boolean success;
        private String message;
        private T data;
        private Long timestamp;

        public Builder<T> success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder<T> message(String message) {
            this.message = message;
            return this;
        }

        public Builder<T> data(T data) {
            this.data = data;
            return this;
        }

        public Builder<T> timestamp(Long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public AniverseResponse<T> build() {
            return new AniverseResponse<>(this);
        }
    }

    // Getters y Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}