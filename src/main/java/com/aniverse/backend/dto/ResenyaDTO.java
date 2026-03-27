// ================================
// ResenyaDTO.java - DTO básico actualizado
// ================================
package com.aniverse.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public class ResenyaDTO {
    private Long id;
    private Long usuarioId;
    private String usuarioNombre;
    private Long animeId;
    private String animeTitulo;
    private String contenido;
    private Double puntuacion; // NUEVA PROPIEDAD

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fechaCreacion; // ACTUALIZADA (era 'fecha')

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fechaActualizacion; // NUEVA PROPIEDAD

    // Constructor vacío
    public ResenyaDTO() {}

    // CONSTRUCTOR ACTUALIZADO - con puntuación y fechas
    public ResenyaDTO(Long id, Long usuarioId, String usuarioNombre,
                      Long animeId, String animeTitulo, String contenido,
                      Double puntuacion, LocalDateTime fechaCreacion,
                      LocalDateTime fechaActualizacion) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.usuarioNombre = usuarioNombre;
        this.animeId = animeId;
        this.animeTitulo = animeTitulo;
        this.contenido = contenido;
        this.puntuacion = puntuacion;
        this.fechaCreacion = fechaCreacion;
        this.fechaActualizacion = fechaActualizacion;
    }

    // Constructor de compatibilidad (el que ya tenías)
    public ResenyaDTO(Long id, Long usuarioId, String usuarioNombre,
                      Long animeId, String animeTitulo, String contenido,
                      LocalDateTime fechaCreacion) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.usuarioNombre = usuarioNombre;
        this.animeId = animeId;
        this.animeTitulo = animeTitulo;
        this.contenido = contenido;
        this.fechaCreacion = fechaCreacion;
        this.puntuacion = 5.0; // Valor por defecto para compatibilidad
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getUsuarioNombre() { return usuarioNombre; }
    public void setUsuarioNombre(String usuarioNombre) { this.usuarioNombre = usuarioNombre; }

    public Long getAnimeId() { return animeId; }
    public void setAnimeId(Long animeId) { this.animeId = animeId; }

    public String getAnimeTitulo() { return animeTitulo; }
    public void setAnimeTitulo(String animeTitulo) { this.animeTitulo = animeTitulo; }

    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }

    public Double getPuntuacion() { return puntuacion; }
    public void setPuntuacion(Double puntuacion) { this.puntuacion = puntuacion; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
}
