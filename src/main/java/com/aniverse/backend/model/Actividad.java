package com.aniverse.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "actividad", indexes = {
        @Index(name = "idx_actividad_usuario", columnList = "usuario_id"),
        @Index(name = "idx_actividad_fecha", columnList = "fecha")
})
public class Actividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private String tipo; // RESEÑA, VALORACION, FAVORITO, LISTA, etc.

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(name = "objeto_id", nullable = false)
    private Long objetoId; // ID del anime, reseña, etc.

    @Column(name = "objeto_tipo", nullable = false)
    private String objetoTipo; // ANIME, RESEÑA, LISTA, etc.

    @Column(length = 1000)
    private String datos; // JSON con datos adicionales específicos del tipo de actividad

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
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

    public String getDatos() {
        return datos;
    }

    public void setDatos(String datos) {
        this.datos = datos;
    }
}