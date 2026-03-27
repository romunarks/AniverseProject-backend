package com.aniverse.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ListaDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    private Long usuarioId;
    private String usuarioNombre;
    private boolean publica;
    private LocalDateTime createdAt;
    private int cantidadAnimes;

    // Constructor
    public ListaDTO(Long id, String nombre, String descripcion, Long usuarioId,
                    String usuarioNombre, boolean publica, LocalDateTime createdAt,
                    int cantidadAnimes) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.usuarioId = usuarioId;
        this.usuarioNombre = usuarioNombre;
        this.publica = publica;
        this.createdAt = createdAt;
        this.cantidadAnimes = cantidadAnimes;
    }

    // Getters y setters
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

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getUsuarioNombre() {
        return usuarioNombre;
    }

    public void setUsuarioNombre(String usuarioNombre) {
        this.usuarioNombre = usuarioNombre;
    }

    public boolean isPublica() {
        return publica;
    }

    public void setPublica(boolean publica) {
        this.publica = publica;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getCantidadAnimes() {
        return cantidadAnimes;
    }

    public void setCantidadAnimes(int cantidadAnimes) {
        this.cantidadAnimes = cantidadAnimes;
    }
}