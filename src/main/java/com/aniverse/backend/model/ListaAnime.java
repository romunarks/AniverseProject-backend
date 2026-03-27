package com.aniverse.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class ListaAnime extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "lista_id", nullable = false)
    private Lista lista;

    @ManyToOne
    @JoinColumn(name = "anime_id", nullable = false)
    private Anime anime;

    @Column
    private String notas;

    @Column
    private Integer episodiosVistos;

    @Column
    private String estado; // Ejemplo: VISTO, VIENDO, PENDIENTE, ABANDONADO

    @Column(nullable = false)
    private LocalDateTime fechaAgregado = LocalDateTime.now();

    // Getters y setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Lista getLista() {
        return lista;
    }

    public void setLista(Lista lista) {
        this.lista = lista;
    }

    public Anime getAnime() {
        return anime;
    }

    public void setAnime(Anime anime) {
        this.anime = anime;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    public Integer getEpisodiosVistos() {
        return episodiosVistos;
    }

    public void setEpisodiosVistos(Integer episodiosVistos) {
        this.episodiosVistos = episodiosVistos;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaAgregado() {
        return fechaAgregado;
    }

    public void setFechaAgregado(LocalDateTime fechaAgregado) {
        this.fechaAgregado = fechaAgregado;
    }
}