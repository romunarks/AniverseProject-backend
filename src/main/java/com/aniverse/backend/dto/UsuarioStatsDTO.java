package com.aniverse.backend.dto;

import java.util.Map;

public class UsuarioStatsDTO {
    private Long usuarioId;
    private String nombreUsuario;
    private Long totalFavoritos;
    private Long totalResenyas;
    private Long totalListas;
    private Long totalVotaciones;
    private Long totalSeguidores;
    private Long totalSiguiendo;
    private Map<String, Integer> generosPreferidos;
    private Double puntuacionPromedio;

    // Constructor
    public UsuarioStatsDTO(Long usuarioId, String nombreUsuario) {
        this.usuarioId = usuarioId;
        this.nombreUsuario = nombreUsuario;
    }

    // Getters y setters
    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public Long getTotalFavoritos() {
        return totalFavoritos;
    }

    public void setTotalFavoritos(Long totalFavoritos) {
        this.totalFavoritos = totalFavoritos;
    }

    public Long getTotalResenyas() {
        return totalResenyas;
    }

    public void setTotalResenyas(Long totalResenyas) {
        this.totalResenyas = totalResenyas;
    }

    public Long getTotalListas() {
        return totalListas;
    }

    public void setTotalListas(Long totalListas) {
        this.totalListas = totalListas;
    }

    public Long getTotalVotaciones() {
        return totalVotaciones;
    }

    public void setTotalVotaciones(Long totalVotaciones) {
        this.totalVotaciones = totalVotaciones;
    }

    public Map<String, Integer> getGenerosPreferidos() {
        return generosPreferidos;
    }

    public void setGenerosPreferidos(Map<String, Integer> generosPreferidos) {
        this.generosPreferidos = generosPreferidos;
    }

    public Long getTotalSeguidores() {
        return totalSeguidores;
    }

    public void setTotalSeguidores(Long totalSeguidores) {
        this.totalSeguidores = totalSeguidores;
    }

    public Long getTotalSiguiendo() {
        return totalSiguiendo;
    }

    public void setTotalSiguiendo(Long totalSiguiendo) {
        this.totalSiguiendo = totalSiguiendo;
    }

    public Double getPuntuacionPromedio() {
        return puntuacionPromedio;
    }

    public void setPuntuacionPromedio(Double puntuacionPromedio) {
        this.puntuacionPromedio = puntuacionPromedio;
    }
}