package com.aniverse.backend.dto;

import java.time.LocalDateTime;

public class SeguidorDTO {
    private Long id;
    private Long seguidorId;
    private String seguidorNombre;
    private String seguidorEmail;
    private Long seguidoId;
    private String seguidoNombre;
    private String seguidoEmail;
    private LocalDateTime fechaSeguimiento;

    // Constructor vacío
    public SeguidorDTO() {}

    // Constructor completo
    public SeguidorDTO(Long id, Long seguidorId, String seguidorNombre, String seguidorEmail,
                       Long seguidoId, String seguidoNombre, String seguidoEmail,
                       LocalDateTime fechaSeguimiento) {
        this.id = id;
        this.seguidorId = seguidorId;
        this.seguidorNombre = seguidorNombre;
        this.seguidorEmail = seguidorEmail;
        this.seguidoId = seguidoId;
        this.seguidoNombre = seguidoNombre;
        this.seguidoEmail = seguidoEmail;
        this.fechaSeguimiento = fechaSeguimiento;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSeguidorId() {
        return seguidorId;
    }

    public void setSeguidorId(Long seguidorId) {
        this.seguidorId = seguidorId;
    }

    public String getSeguidorNombre() {
        return seguidorNombre;
    }

    public void setSeguidorNombre(String seguidorNombre) {
        this.seguidorNombre = seguidorNombre;
    }

    public String getSeguidorEmail() {
        return seguidorEmail;
    }

    public void setSeguidorEmail(String seguidorEmail) {
        this.seguidorEmail = seguidorEmail;
    }

    public Long getSeguidoId() {
        return seguidoId;
    }

    public void setSeguidoId(Long seguidoId) {
        this.seguidoId = seguidoId;
    }

    public String getSeguidoNombre() {
        return seguidoNombre;
    }

    public void setSeguidoNombre(String seguidoNombre) {
        this.seguidoNombre = seguidoNombre;
    }

    public String getSeguidoEmail() {
        return seguidoEmail;
    }

    public void setSeguidoEmail(String seguidoEmail) {
        this.seguidoEmail = seguidoEmail;
    }

    public LocalDateTime getFechaSeguimiento() {
        return fechaSeguimiento;
    }

    public void setFechaSeguimiento(LocalDateTime fechaSeguimiento) {
        this.fechaSeguimiento = fechaSeguimiento;
    }
}