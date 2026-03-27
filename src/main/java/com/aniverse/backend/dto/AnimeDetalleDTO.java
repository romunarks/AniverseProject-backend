package com.aniverse.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class AnimeDetalleDTO extends AnimeDTO {
    // Getters y setters
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    public AnimeDetalleDTO(Long id, String titulo, String descripcion, String genero, String imagenUrl,
                           LocalDateTime createdAt, LocalDateTime updatedAt, String createdBy, String updatedBy) {
        super(id, titulo, descripcion, genero, imagenUrl);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

}