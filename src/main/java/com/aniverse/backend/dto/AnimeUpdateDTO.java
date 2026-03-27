package com.aniverse.backend.dto;

import jakarta.validation.constraints.*;

public class AnimeUpdateDTO {
    @Size(min = 1, max = 100, message = "El título debe tener entre 1 y 100 caracteres")
    private String titulo;

    @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
    private String descripcion;

    private String genero;

    @Min(value = 1900, message = "El año debe ser posterior a 1900")
    @Max(value = 2025, message = "El año no puede ser futuro")
    private Integer anyo;

    private String temporada;

    private String imagenUrl;

    private Long jikanId;

    // Getters y setters
    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public Integer getAnyo() {
        return anyo;
    }

    public void setAnyo(Integer anyo) {
        this.anyo = anyo;
    }

    public String getTemporada() {
        return temporada;
    }

    public void setTemporada(String temporada) {
        this.temporada = temporada;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    public Long getJikanId() {
        return jikanId;
    }

    public void setJikanId(Long jikanId) {
        this.jikanId = jikanId;
    }
}