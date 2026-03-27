package com.aniverse.backend.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "resenya",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "unique_user_anime_review",
                        columnNames = {"usuario_id", "anime_id", "eliminado"}
                )
        },
        indexes = {
                @Index(name = "idx_resenya_usuario", columnList = "usuario_id"),
                @Index(name = "idx_resenya_anime", columnList = "anime_id"),
                @Index(name = "idx_resenya_puntuacion", columnList = "puntuacion"),
                @Index(name = "idx_resenya_fecha_creacion", columnList = "fecha_creacion"),
                @Index(name = "idx_resenya_eliminado", columnList = "eliminado")
        })
public class Resenya {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anime_id", nullable = false)
    private Anime anime;

    @Column(nullable = false, length = 1000)
    @NotBlank(message = "El contenido de la reseña no puede estar vacío")
    @Size(min = 10, max = 1000, message = "El contenido debe tener entre 10 y 1000 caracteres")
    private String contenido;

    @Column(nullable = false)
    @NotNull(message = "La puntuación es obligatoria")
    @DecimalMin(value = "1.0", message = "La puntuación mínima es 1.0")
    @DecimalMax(value = "10.0", message = "La puntuación máxima es 10.0")
    private Double puntuacion;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fechaActualizacion;

    // Campos para soft delete
    @Column(nullable = false)
    private boolean eliminado = false;

    @Column(name = "fecha_eliminacion")
    private LocalDateTime fechaEliminacion;

    @OneToMany(mappedBy = "resenya", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comentario> comentarios;

    // Constructores
    public Resenya() {}

    public Resenya(Usuario usuario, Anime anime, String contenido, Double puntuacion) {
        this.usuario = usuario;
        this.anime = anime;
        this.contenido = contenido;
        this.puntuacion = puntuacion;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Anime getAnime() { return anime; }
    public void setAnime(Anime anime) { this.anime = anime; }

    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }

    public Double getPuntuacion() { return puntuacion; }
    public void setPuntuacion(Double puntuacion) { this.puntuacion = puntuacion; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }

    public boolean isEliminado() { return eliminado; }
    public void setEliminado(boolean eliminado) { this.eliminado = eliminado; }

    public LocalDateTime getFechaEliminacion() { return fechaEliminacion; }
    public void setFechaEliminacion(LocalDateTime fechaEliminacion) { this.fechaEliminacion = fechaEliminacion; }

    public List<Comentario> getComentarios() { return comentarios; }
    public void setComentarios(List<Comentario> comentarios) { this.comentarios = comentarios; }

    // Métodos de utilidad
    public boolean isPuntuacionValida() {
        return puntuacion != null && puntuacion >= 1.0 && puntuacion <= 10.0;
    }

    public void marcarComoEliminado() {
        this.eliminado = true;
        this.fechaEliminacion = LocalDateTime.now();
    }

    public void restaurar() {
        this.eliminado = false;
        this.fechaEliminacion = null;
    }

    @Override
    public String toString() {
        return "Resenya{" +
                "id=" + id +
                ", usuario=" + (usuario != null ? usuario.getNombre() : "null") +
                ", anime=" + (anime != null ? anime.getTitulo() : "null") +
                ", puntuacion=" + puntuacion +
                ", fechaCreacion=" + fechaCreacion +
                ", eliminado=" + eliminado +
                '}';
    }
}