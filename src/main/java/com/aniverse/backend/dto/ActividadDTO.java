package com.aniverse.backend.dto;

import java.time.LocalDateTime;

public class ActividadDTO {
    private Long id;
    private Long usuarioId;
    private String usuarioNombre;
    private String tipo;
    private LocalDateTime fecha;
    private Long objetoId;
    private String objetoTipo;
    private String objetoNombre; // Nombre del anime, título de la reseña, etc.
    private String mensaje; // Mensaje descriptivo de la actividad
    private String url; // URL para acceder al contenido relacionado

    // Constructor vacío
    public ActividadDTO() {}

    // Constructor básico
    public ActividadDTO(Long id, Long usuarioId, String usuarioNombre, String tipo,
                        LocalDateTime fecha, Long objetoId, String objetoTipo,
                        String objetoNombre, String mensaje, String url) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.usuarioNombre = usuarioNombre;
        this.tipo = tipo;
        this.fecha = fecha;
        this.objetoId = objetoId;
        this.objetoTipo = objetoTipo;
        this.objetoNombre = objetoNombre;
        this.mensaje = mensaje;
        this.url = url;
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

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
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

    public String getObjetoNombre() {
        return objetoNombre;
    }

    public void setObjetoNombre(String objetoNombre) {
        this.objetoNombre = objetoNombre;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}