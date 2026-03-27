package com.aniverse.backend.dto;

import java.time.LocalDateTime;

public class NotificacionDTO {
    private Long id;
    private Long destinatarioId;
    private String destinatarioNombre;
    private Long emisorId;
    private String emisorNombre;
    private String tipo;
    private String mensaje;
    private boolean leida;
    private LocalDateTime fecha;
    private Long objetoId;
    private String objetoTipo;
    private String url;

    // Constructor vacío
    public NotificacionDTO() {}

    // Constructor con todos los campos
    public NotificacionDTO(Long id, Long destinatarioId, String destinatarioNombre,
                           Long emisorId, String emisorNombre, String tipo,
                           String mensaje, boolean leida, LocalDateTime fecha,
                           Long objetoId, String objetoTipo, String url) {
        this.id = id;
        this.destinatarioId = destinatarioId;
        this.destinatarioNombre = destinatarioNombre;
        this.emisorId = emisorId;
        this.emisorNombre = emisorNombre;
        this.tipo = tipo;
        this.mensaje = mensaje;
        this.leida = leida;
        this.fecha = fecha;
        this.objetoId = objetoId;
        this.objetoTipo = objetoTipo;
        this.url = url;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDestinatarioId() {
        return destinatarioId;
    }

    public void setDestinatarioId(Long destinatarioId) {
        this.destinatarioId = destinatarioId;
    }

    public String getDestinatarioNombre() {
        return destinatarioNombre;
    }

    public void setDestinatarioNombre(String destinatarioNombre) {
        this.destinatarioNombre = destinatarioNombre;
    }

    public Long getEmisorId() {
        return emisorId;
    }

    public void setEmisorId(Long emisorId) {
        this.emisorId = emisorId;
    }

    public String getEmisorNombre() {
        return emisorNombre;
    }

    public void setEmisorNombre(String emisorNombre) {
        this.emisorNombre = emisorNombre;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public boolean isLeida() {
        return leida;
    }

    public void setLeida(boolean leida) {
        this.leida = leida;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public Long getObjetoId() {
        return objetoId;
    }

    public void setObjetoId(Long objetoId) {
        this.objetoId = objetoId;
    }

    public String getObjetoTipo() {
        return objetoTipo;
    }

    public void setObjetoTipo(String objetoTipo) {
        this.objetoTipo = objetoTipo;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}