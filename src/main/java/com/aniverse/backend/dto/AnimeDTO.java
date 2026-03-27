package com.aniverse.backend.dto;

// No son necesarios otros imports para esta estructura de DTO básica

public class AnimeDTO {

    private Long id;                // ID local en nuestra base de datos
    private Long jikanId;           // ID de la API externa (Jikan/MyAnimeList)
    private String titulo;
    private String descripcion;
    private String genero;
    private String imagenUrl;
    private Double puntuacionPromedio; // El campo sigue siendo Double para flexibilidad general
    private Integer anyo;              // El campo sigue siendo Integer para flexibilidad general (puede ser null)
    private String temporada;

    // Constructor vacío necesario para Jackson y otras librerías de serialización
    public AnimeDTO() {
    }

    // ---- CONSTRUCTOR PRINCIPAL PARA LAS CONSULTAS JPQL ----
    // Acepta 'double' para puntuacionPromedio si CAST a Double es problemático y AVG devuelve double
    // Acepta 'int' para anyo, que coincide con el tipo en la entidad Anime.
    // Java auto-boxeará int a Integer y double a Double al asignar a los campos de la clase.
    public AnimeDTO(Long id, Long jikanId, String titulo, String descripcion, String genero,
                    String imagenUrl, Double puntuacionPromedio, int anyo, String temporada) {
        this.id = id;
        this.jikanId = jikanId;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.genero = genero;
        this.imagenUrl = imagenUrl;
        this.puntuacionPromedio = puntuacionPromedio; // Se asigna el Double directamente
        this.anyo = anyo; // Autoboxing de int a Integer
        this.temporada = temporada;
    }

    // ---- Otros constructores que ya tenías (si aún los necesitas para otras partes del código) ----

    // Constructor con campos esenciales y jikanId (puntuacionPromedio, anyo, temporada serán null/0)
    public AnimeDTO(Long id, Long jikanId, String titulo, String descripcion, String genero, String imagenUrl) {
        this.id = id;
        this.jikanId = jikanId;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.genero = genero;
        this.imagenUrl = imagenUrl;
        // puntuacionPromedio, anyo, temporada quedarán como null/0 por defecto
    }

    // Constructor compatible con código existente (jikanId = null, y otros campos nuevos también)
    public AnimeDTO(Long id, String titulo, String descripcion, String genero, String imagenUrl) {
        this(id, null, titulo, descripcion, genero, imagenUrl); // Llama al constructor de arriba
        // puntuacionPromedio, anyo, temporada quedarán como null/0 por defecto
    }


    // --- Getters y Setters ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getJikanId() {
        return jikanId;
    }

    public void setJikanId(Long jikanId) {
        this.jikanId = jikanId;
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

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    public Double getPuntuacionPromedio() {
        return puntuacionPromedio;
    }

    public void setPuntuacionPromedio(Double puntuacionPromedio) {
        this.puntuacionPromedio = puntuacionPromedio;
    }

    public Integer getAnyo() {
        return anyo;
    }

    public void setAnyo(Integer anyo) {
        this.anyo = anyo;
    }

    public String getTemporada() {
        return temporada;
    }

    public void setTemporada(String temporada) {
        this.temporada = temporada;
    }
}