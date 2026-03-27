// ===============================================
// ResenyaCrearDTO.java
// ===============================================
package com.aniverse.backend.dto;

import jakarta.validation.constraints.*;

public class ResenyaCrearDTO {

    // Identificadores del anime - se usa uno de los dos
    private Long animeId;      // ID local del anime
    private Long jikanId;      // ID de Jikan (para animes externos)

    @NotBlank(message = "El contenido de la reseña es obligatorio")
    @Size(min = 10, max = 1000, message = "El contenido debe tener entre 10 y 1000 caracteres")
    private String contenido;

    @NotNull(message = "La puntuación es obligatoria")
    @DecimalMin(value = "1.0", message = "La puntuación mínima es 1.0")
    @DecimalMax(value = "10.0", message = "La puntuación máxima es 10.0")
    private Double puntuacion;

    // Constructores
    public ResenyaCrearDTO() {}

    public ResenyaCrearDTO(Long animeId, Long jikanId, String contenido, Double puntuacion) {
        this.animeId = animeId;
        this.jikanId = jikanId;
        this.contenido = contenido;
        this.puntuacion = puntuacion;
    }

    // Getters y Setters
    public Long getAnimeId() {
        return animeId;
    }

    public void setAnimeId(Long animeId) {
        this.animeId = animeId;
    }

    public Long getJikanId() {
        return jikanId;
    }

    public void setJikanId(Long jikanId) {
        this.jikanId = jikanId;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public Double getPuntuacion() {
        return puntuacion;
    }

    public void setPuntuacion(Double puntuacion) {
        this.puntuacion = puntuacion;
    }

    // Validación personalizada
    public boolean isValid() {
        return (animeId != null || jikanId != null) &&
                contenido != null && !contenido.trim().isEmpty() &&
                puntuacion != null && puntuacion >= 1.0 && puntuacion <= 10.0;
    }

    @Override
    public String toString() {
        return "ResenyaCrearDTO{" +
                "animeId=" + animeId +
                ", jikanId=" + jikanId +
                ", contenido='" + (contenido != null ? contenido.substring(0, Math.min(contenido.length(), 50)) + "..." : "null") + '\'' +
                ", puntuacion=" + puntuacion +
                '}';
    }
}
