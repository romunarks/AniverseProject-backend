package com.aniverse.backend.dto;

import jakarta.validation.constraints.NotNull;

public class ListaAnimeDTO {
    private Long id;
    private Long animeId;
    private String animeTitulo;
    private String animeImagenUrl;
    private String notas;
    private Integer episodiosVistos;
    private String estado;

    // Constructor
    public ListaAnimeDTO(Long id, Long animeId, String animeTitulo, String animeImagenUrl,
                         String notas, Integer episodiosVistos, String estado) {
        this.id = id;
        this.animeId = animeId;
        this.animeTitulo = animeTitulo;
        this.animeImagenUrl = animeImagenUrl;
        this.notas = notas;
        this.episodiosVistos = episodiosVistos;
        this.estado = estado;
    }

    // Getters y setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAnimeId() {
        return animeId;
    }

    public void setAnimeId(Long animeId) {
        this.animeId = animeId;
    }

    public String getAnimeTitulo() {
        return animeTitulo;
    }

    public void setAnimeTitulo(String animeTitulo) {
        this.animeTitulo = animeTitulo;
    }

    public String getAnimeImagenUrl() {
        return animeImagenUrl;
    }

    public void setAnimeImagenUrl(String animeImagenUrl) {
        this.animeImagenUrl = animeImagenUrl;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    public Integer getEpisodiosVistos() {
        return episodiosVistos;
    }

    public void setEpisodiosVistos(Integer episodiosVistos) {
        this.episodiosVistos = episodiosVistos;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}