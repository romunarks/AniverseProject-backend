package com.aniverse.backend.dto;

public class UsuarioResumidoDTO {
    private Long id;
    private String nombre;
    private String email;
    private boolean siguiendo;

    // Constructor vacío
    public UsuarioResumidoDTO() {}

    // Constructor
    public UsuarioResumidoDTO(Long id, String nombre, String email, boolean siguiendo) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.siguiendo = siguiendo;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isSiguiendo() {
        return siguiendo;
    }

    public void setSiguiendo(boolean siguiendo) {
        this.siguiendo = siguiendo;
    }
}