package com.aniverse.backend.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "favorito",
        uniqueConstraints = {
                @UniqueConstraint(name = "unique_user_anime_favorite",
                        columnNames = {"usuario_id", "anime_id"})
        },
        indexes = {
                @Index(name = "idx_favorito_usuario", columnList = "usuario_id"),
                @Index(name = "idx_favorito_anime", columnList = "anime_id"),
                @Index(name = "idx_favorito_fecha", columnList = "fecha_agregado")
        })
public class Favorito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anime_id", nullable = false)
    private Anime anime;

    @CreationTimestamp
    @Column(name = "fecha_agregado", nullable = false, updatable = false)
    private LocalDateTime fechaAgregado;

    // Constructores
    public Favorito() {}

    public Favorito(Usuario usuario, Anime anime) {
        this.usuario = usuario;
        this.anime = anime;
    }

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

    public Anime getAnime() {
        return anime;
    }

    public void setAnime(Anime anime) {
        this.anime = anime;
    }

    public LocalDateTime getFechaAgregado() {
        return fechaAgregado;
    }

    public void setFechaAgregado(LocalDateTime fechaAgregado) {
        this.fechaAgregado = fechaAgregado;
    }

    // Métodos de utilidad
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Favorito)) return false;
        Favorito favorito = (Favorito) o;
        return usuario != null && anime != null &&
                usuario.getId().equals(favorito.usuario.getId()) &&
                anime.getId().equals(favorito.anime.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Favorito{" +
                "id=" + id +
                ", usuario=" + (usuario != null ? usuario.getNombre() : "null") +
                ", anime=" + (anime != null ? anime.getTitulo() : "null") +
                ", fechaAgregado=" + fechaAgregado +
                '}';
    }
}