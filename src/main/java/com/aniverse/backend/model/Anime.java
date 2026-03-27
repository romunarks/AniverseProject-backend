package com.aniverse.backend.model;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "anime", indexes = {
        @Index(name = "idx_anime_titulo", columnList = "titulo"),
        @Index(name = "idx_anime_genero", columnList = "genero"),
        @Index(name = "idx_anime_anyo_temporada", columnList = "anyo, temporada"),
        @Index(name = "idx_anime_eliminado", columnList = "eliminado")
})
public class Anime extends AuditableEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // En Anime.java, agrega este campo:
    @Column(unique = true)
    private Long jikanId;  // ID de Jikan API

    @Column(nullable = false)
    private String titulo;

    @Column(length = 1000)
    private String descripcion;

    private String genero;

    private String temporada;

    @Column(nullable = false)
    private int anyo;

    @Column
    private String imagenUrl;  // URL a la imagen de portada

    // Nuevos campos para soft delete
    @Column(nullable = false)
    private boolean eliminado = false;

    @Column
    private LocalDateTime fechaEliminacion;


    @OneToMany(mappedBy = "anime", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Favorito> favoritos;

    @OneToMany(mappedBy = "anime", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 10)
    private List<Resenya> resenyas;

    @OneToMany(mappedBy = "anime", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 10)
    private List<Votacion> votaciones;



    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public String getTemporada() {
        return temporada;
    }

    public void setTemporada(String temporada) {
        this.temporada = temporada;
    }

    public int getAnyo() {
        return anyo;
    }

    public void setAnyo(int anyo) {
        this.anyo = anyo;
    }

    public List<Favorito> getFavoritos() {
        return favoritos;
    }

    public void setFavoritos(List<Favorito> favoritos) {
        this.favoritos = favoritos;
    }

    public List<Resenya> getResenyas() {
        return resenyas;
    }

    public void setResenyas(List<Resenya> resenyas) {
        this.resenyas = resenyas;
    }

    public List<Votacion> getVotaciones() {
        return votaciones;
    }

    public void setVotaciones(List<Votacion> votaciones) {
        this.votaciones = votaciones;
    }
    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    // Añadir nuevos getters y setters
    public boolean isEliminado() {
        return eliminado;
    }

    public void setEliminado(boolean eliminado) {
        this.eliminado = eliminado;
    }

    public LocalDateTime getFechaEliminacion() {
        return fechaEliminacion;
    }

    public void setFechaEliminacion(LocalDateTime fechaEliminacion) {
        this.fechaEliminacion = fechaEliminacion;
    }

    // Getter y setter
    public Long getJikanId() {
        return jikanId;
    }

    public void setJikanId(Long jikanId) {
        this.jikanId = jikanId;
    }
}
