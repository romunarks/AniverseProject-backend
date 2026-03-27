// ===============================================
// ResenyaActualizarDTO.java
// ===============================================
package com.aniverse.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public class ResenyaActualizarDTO {

    @Size(min = 10, max = 1000, message = "El contenido debe tener entre 10 y 1000 caracteres")
    private String contenido;

    @DecimalMin(value = "1.0", message = "La puntuación mínima es 1.0")
    @DecimalMax(value = "10.0", message = "La puntuación máxima es 10.0")
    private Double puntuacion;

    // Constructores
    public ResenyaActualizarDTO() {}

    public ResenyaActualizarDTO(String contenido, Double puntuacion) {
        this.contenido = contenido;
        this.puntuacion = puntuacion;
    }

    // Getters y Setters
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

    // Validación
    public boolean hasValidData() {
        return (contenido != null && !contenido.trim().isEmpty()) || puntuacion != null;
    }

    @Override
    public String toString() {
        return "ResenyaActualizarDTO{" +
                "contenido='" + (contenido != null ? contenido.substring(0, Math.min(contenido.length(), 50)) + "..." : "null") + '\'' +
                ", puntuacion=" + puntuacion +
                '}';
    }
}