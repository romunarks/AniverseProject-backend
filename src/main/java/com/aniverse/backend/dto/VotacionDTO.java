package com.aniverse.backend.dto;

public class VotacionDTO {
    private Long id;
    private Long usuarioId;
    private String usuarioNombre;
    private Long animeId;
    private String animeTitulo;
    private Double puntuacion;

    public VotacionDTO(Long id, Long usuarioId, String usuarioNombre, Long animeId, String animeTitulo, Double puntuacion) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.usuarioNombre = usuarioNombre;
        this.animeId = animeId;
        this.animeTitulo = animeTitulo;
        this.puntuacion = puntuacion;
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

    public Double getPuntuacion() { return puntuacion; }
    public void setPuntuacion(Double puntuacion) { this.puntuacion = puntuacion; }
}
