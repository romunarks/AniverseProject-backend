package com.aniverse.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

@Entity
public class Votacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "anime_id", nullable = false)
    private Anime anime;

    @Column(nullable = false)
    @DecimalMin(value = "1.0", message = "La puntuación mínima permitida es 1.0")
    @DecimalMax(value = "5.0", message = "La puntuación máxima permitida es 5.0")
    private Double puntuacion;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Anime getAnime() { return anime; }
    public void setAnime(Anime anime) { this.anime = anime; }

    public Double getPuntuacion() { return puntuacion; }
    public void setPuntuacion(Double puntuacion) {
        if (puntuacion < 1.0 || puntuacion > 5.0) {
            throw new IllegalArgumentException("La puntuación debe estar entre 1.0 y 5.0.");
        }
        this.puntuacion = puntuacion;
    }
}
