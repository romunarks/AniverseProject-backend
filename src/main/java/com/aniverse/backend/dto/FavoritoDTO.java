package com.aniverse.backend.dto;

import com.aniverse.backend.model.Anime;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class FavoritoDTO {

    private Long id;
    private Long usuarioId;
    private String usuarioNombre;
    private Long animeId;
    private String animeTitulo;
    private String animeImagenUrl;
    private String animeGenero;
    private Long animeJikanId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fechaAgregado;

    // AGREGAR: Objeto anime completo para compatibilidad con frontend
    private AnimeDTO anime;

    // Constructores
    public FavoritoDTO() {}

    public FavoritoDTO(Long id, Long usuarioId, String usuarioNombre, Long animeId, String animeTitulo) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.usuarioNombre = usuarioNombre;
        this.animeId = animeId;
        this.animeTitulo = animeTitulo;
    }

    // Constructor completo
    public FavoritoDTO(Long id, Long usuarioId, String usuarioNombre,
                       Long animeId, String animeTitulo, String animeImagenUrl,
                       String animeGenero, Long animeJikanId, LocalDateTime fechaAgregado) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.usuarioNombre = usuarioNombre;
        this.animeId = animeId;
        this.animeTitulo = animeTitulo;
        this.animeImagenUrl = animeImagenUrl;
        this.animeGenero = animeGenero;
        this.animeJikanId = animeJikanId;
        this.fechaAgregado = fechaAgregado;
    }

    // MÉTODO CORREGIDO: Crear objeto anime completo para el frontend
    public void setAnimeFromEntity(Anime anime) {
        if (anime != null) {
            // Establecer campos individuales (compatibilidad hacia atrás)
            this.animeImagenUrl = anime.getImagenUrl();
            this.animeGenero = anime.getGenero();
            this.animeJikanId = anime.getJikanId();

            // CREAR OBJETO ANIME COMPLETO PARA FRONTEND
            this.anime = new AnimeDTO(
                    anime.getId(),                    // id
                    anime.getJikanId(),              // jikanId
                    anime.getTitulo(),               // titulo
                    anime.getDescripcion(),          // descripcion
                    anime.getGenero(),               // genero
                    anime.getImagenUrl(),            // imagenUrl
                    null,                            // puntuacionPromedio (será calculada después)
                    anime.getAnyo(),                 // anyo
                    anime.getTemporada()             // temporada
            );
        }
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getAnimeGenero() {
        return animeGenero;
    }

    public void setAnimeGenero(String animeGenero) {
        this.animeGenero = animeGenero;
    }

    public Long getAnimeJikanId() {
        return animeJikanId;
    }

    public void setAnimeJikanId(Long animeJikanId) {
        this.animeJikanId = animeJikanId;
    }

    public LocalDateTime getFechaAgregado() {
        return fechaAgregado;
    }

    public void setFechaAgregado(LocalDateTime fechaAgregado) {
        this.fechaAgregado = fechaAgregado;
    }

    // GETTER Y SETTER PARA EL OBJETO ANIME
    public AnimeDTO getAnime() {
        return anime;
    }

    public void setAnime(AnimeDTO anime) {
        this.anime = anime;
    }

    @Override
    public String toString() {
        return "FavoritoDTO{" +
                "id=" + id +
                ", usuarioNombre='" + usuarioNombre + '\'' +
                ", animeTitulo='" + animeTitulo + '\'' +
                ", animeImagenUrl='" + animeImagenUrl + '\'' +
                ", fechaAgregado=" + fechaAgregado +
                '}';
    }
}