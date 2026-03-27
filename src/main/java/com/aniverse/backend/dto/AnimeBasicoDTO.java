// 1. CREAR AnimeBasicoDTO.java - Solo campos esenciales
package com.aniverse.backend.dto;

public class AnimeBasicoDTO {
    private Long id;
    private Long jikanId;
    private String titulo;
    private String imagenUrl;

    // Constructor
    public AnimeBasicoDTO(Long id, Long jikanId, String titulo, String imagenUrl) {
        this.id = id;
        this.jikanId = jikanId;
        this.titulo = titulo;
        this.imagenUrl = imagenUrl;
    }

    // Constructor desde entidad
    public AnimeBasicoDTO(com.aniverse.backend.model.Anime anime) {
        this.id = anime.getId();
        this.jikanId = anime.getJikanId() != null ? anime.getJikanId().longValue() : null;
        this.titulo = anime.getTitulo();
        this.imagenUrl = anime.getImagenUrl();
    }

    // Constructor vacío
    public AnimeBasicoDTO() {}

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getJikanId() { return jikanId; }
    public void setJikanId(Long jikanId) { this.jikanId = jikanId; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }
}
