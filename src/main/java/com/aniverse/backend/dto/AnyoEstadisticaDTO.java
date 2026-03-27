package com.aniverse.backend.dto;

// DTO para estadísticas por año
public class AnyoEstadisticaDTO {
    private int anyo;
    private long cantidad;

    public AnyoEstadisticaDTO(int anyo, long cantidad) {
        this.anyo = anyo;
        this.cantidad = cantidad;
    }

    // Getters y setters
    public int getAnyo() { return anyo; }
    public void setAnyo(int anyo) { this.anyo = anyo; }

    public long getCantidad() { return cantidad; }
    public void setCantidad(long cantidad) { this.cantidad = cantidad; }
}