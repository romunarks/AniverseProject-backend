package com.aniverse.backend.dto;

import jakarta.validation.constraints.NotNull;

public class ListaAnimeCreateDTO {
    @NotNull(message = "El ID del anime es obligatorio")
    private Long animeId;

    private String notas;

    private Integer episodiosVistos;

    private String estado;

    // Getters y setters
    public Long getAnimeId() {
        return animeId;
    }

    public void setAnimeId(Long animeId) {
        this.animeId = animeId;
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