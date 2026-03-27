package com.aniverse.backend.dto;

public class AnimeResumidoDTO {
    private Long id;
    private Long jikanId;       // Nuevo campo para API externa
    private String titulo;
    private String imagenUrl;
    private String genero;
    private Double puntuacionPromedio;

    // Constructor
    public AnimeResumidoDTO(Long id, Long jikanId, String titulo, String imagenUrl, String genero, Double puntuacionPromedio) {
        this.id = id;
        this.jikanId = jikanId;
        this.titulo = titulo;
        this.imagenUrl = imagenUrl;
        this.genero = genero;
        this.puntuacionPromedio = puntuacionPromedio;
    }

    // Constructor vacío necesario para Jackson
    public AnimeResumidoDTO() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getJikanId() {
        return jikanId;
    }

    public void setJikanId(Long jikanId) {
        this.jikanId = jikanId;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public Double getPuntuacionPromedio() {
        return puntuacionPromedio;
    }

    public void setPuntuacionPromedio(Double puntuacionPromedio) {
        this.puntuacionPromedio = puntuacionPromedio;
    }
}