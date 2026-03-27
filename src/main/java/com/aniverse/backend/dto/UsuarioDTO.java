package com.aniverse.backend.dto;

import java.util.List;


public class UsuarioDTO {


    private Long id;
    private String nombre;
    private String email;
    private List<String> roles;
    private long seguidoresCount;
    private long siguiendoCount;
    private boolean siguiendoPorUsuarioActual;

    public UsuarioDTO(Long id, String nombre, String email, List<String> roles) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.roles = roles;
    }

    // Getters y Setters
    // Y sus respectivos getters y setters
    public long getSeguidoresCount() {
        return seguidoresCount;
    }

    public void setSeguidoresCount(long seguidoresCount) {
        this.seguidoresCount = seguidoresCount;
    }

    public long getSiguiendoCount() {
        return siguiendoCount;
    }

    public void setSiguiendoCount(long siguiendoCount) {
        this.siguiendoCount = siguiendoCount;
    }

    public boolean isSiguiendoPorUsuarioActual() {
        return siguiendoPorUsuarioActual;
    }

    public void setSiguiendoPorUsuarioActual(boolean siguiendoPorUsuarioActual) {
        this.siguiendoPorUsuarioActual = siguiendoPorUsuarioActual;
    }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}
