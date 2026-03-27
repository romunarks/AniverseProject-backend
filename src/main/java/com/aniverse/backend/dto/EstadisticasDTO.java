package com.aniverse.backend.dto;

import java.util.List;
import java.util.Map;

// DTO para estadísticas generales
public class EstadisticasDTO {
    private long totalAnimes;
    private long totalUsuarios;
    private long totalResenyas;
    private long totalVotaciones;
    private double puntuacionPromedio;
    private List<GeneroEstadisticaDTO> distribucionGeneros;
    private List<AnyoEstadisticaDTO> distribucionAnyos;

    // Constructor, getters y setters
    public EstadisticasDTO() {}

    // Getters y setters
    public long getTotalAnimes() { return totalAnimes; }
    public void setTotalAnimes(long totalAnimes) { this.totalAnimes = totalAnimes; }

    public long getTotalUsuarios() { return totalUsuarios; }
    public void setTotalUsuarios(long totalUsuarios) { this.totalUsuarios = totalUsuarios; }

    public long getTotalResenyas() { return totalResenyas; }
    public void setTotalResenyas(long totalResenyas) { this.totalResenyas = totalResenyas; }

    public long getTotalVotaciones() { return totalVotaciones; }
    public void setTotalVotaciones(long totalVotaciones) { this.totalVotaciones = totalVotaciones; }

    public double getPuntuacionPromedio() { return puntuacionPromedio; }
    public void setPuntuacionPromedio(double puntuacionPromedio) { this.puntuacionPromedio = puntuacionPromedio; }

    public List<GeneroEstadisticaDTO> getDistribucionGeneros() { return distribucionGeneros; }
    public void setDistribucionGeneros(List<GeneroEstadisticaDTO> distribucionGeneros) { this.distribucionGeneros = distribucionGeneros; }

    public List<AnyoEstadisticaDTO> getDistribucionAnyos() { return distribucionAnyos; }
    public void setDistribucionAnyos(List<AnyoEstadisticaDTO> distribucionAnyos) { this.distribucionAnyos = distribucionAnyos; }
}
