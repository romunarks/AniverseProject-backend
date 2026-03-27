package com.aniverse.backend.dto;

// DTO para estadísticas por género
public class GeneroEstadisticaDTO {
    private String genero;
    private long cantidad;
    private double porcentaje;

    public GeneroEstadisticaDTO(String genero, long cantidad, double porcentaje) {
        this.genero = genero;
        this.cantidad = cantidad;
        this.porcentaje = porcentaje;
    }

    // Getters y setters
    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }

    public long getCantidad() { return cantidad; }
    public void setCantidad(long cantidad) { this.cantidad = cantidad; }

    public double getPorcentaje() { return porcentaje; }
    public void setPorcentaje(double porcentaje) { this.porcentaje = porcentaje; }
}