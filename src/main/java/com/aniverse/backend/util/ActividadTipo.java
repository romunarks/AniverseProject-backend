package com.aniverse.backend.util;

/**
 * Enumeración para los diferentes tipos de actividades que pueden ser registradas en el sistema
 * Convertido desde constantes String a enum para mayor seguridad de tipos
 */
public enum ActividadTipo {

    // ===== TUS ACTIVIDADES EXISTENTES (MANTENIENDO COMPATIBILIDAD) =====
    RESENYA("Reseña"),
    VALORACION("Valoración"),
    FAVORITO_ADD("Anime agregado a favoritos"),
    FAVORITO_REMOVE("Anime eliminado de favoritos"), // Era FAVORITO_REMOVIDO
    LISTA_CREADA("Lista creada"),
    LISTA_ACTUALIZADA("Lista actualizada"),
    ANIME_AGREGADO_LISTA("Anime agregado a lista"),
    SEGUIR_USUARIO("Usuario seguido"),
    RESENYA_UPDATE("Reseña actualizada"),
    RESENYA_DELETE("Reseña eliminada"),

    // ===== NUEVAS ACTIVIDADES ADICIONALES =====
    RESENYA_CREATE("Reseña creada"), // Alias para RESENYA
    FAVORITO_REMOVE_ALT("Favorito eliminado"), // Alias para compatibilidad

    // ===== ACTIVIDADES DE VOTACIONES =====
    VOTACION_CREATE("Votación creada"),
    VOTACION_UPDATE("Votación actualizada"),
    VOTACION_DELETE("Votación eliminada"),

    // ===== ACTIVIDADES DE USUARIOS =====
    USER_LOGIN("Usuario inició sesión"),
    USER_LOGOUT("Usuario cerró sesión"),
    USER_REGISTER("Usuario registrado"),
    USER_UPDATE_PROFILE("Perfil de usuario actualizado"),

    // ===== ACTIVIDADES DE SEGUIMIENTO =====
    USER_FOLLOW("Usuario seguido"), // Alias para SEGUIR_USUARIO
    USER_UNFOLLOW("Usuario no seguido"),

    // ===== ACTIVIDADES DE ANIMES =====
    ANIME_CREATE("Anime creado"),
    ANIME_UPDATE("Anime actualizado"),
    ANIME_DELETE("Anime eliminado"),

    // ===== ACTIVIDADES DE COMENTARIOS =====
    COMENTARIO_CREATE("Comentario creado"),
    COMENTARIO_UPDATE("Comentario actualizado"),
    COMENTARIO_DELETE("Comentario eliminado"),

    // ===== ACTIVIDADES GENERALES =====
    SEARCH("Búsqueda realizada"),
    VIEW("Elemento visualizado"),
    SHARE("Contenido compartido");

    private final String descripcion;

    ActividadTipo(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /**
     * ✅ MÉTODO PARA COMPATIBILIDAD CON TU CÓDIGO EXISTENTE
     * Permite usar ActividadTipo.RESENYA.getValue() igual que antes
     */
    public String getValue() {
        return this.name();
    }

    /**
     * Obtiene el tipo de actividad por su nombre (String)
     * ✅ MANTIENE COMPATIBILIDAD CON CÓDIGO QUE USA STRINGS
     */
    public static ActividadTipo fromString(String tipo) {
        try {
            return ActividadTipo.valueOf(tipo.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Manejar casos especiales para compatibilidad
            switch (tipo.toUpperCase()) {
                case "FAVORITO_REMOVIDO":
                    return FAVORITO_REMOVE;
                case "RESENYA":
                    return RESENYA_CREATE;
                default:
                    throw new IllegalArgumentException("Tipo de actividad no válido: " + tipo);
            }
        }
    }

    /**
     * ✅ MÉTODO ESTÁTICO PARA COMPATIBILIDAD TOTAL
     * Permite seguir usando: ActividadTipo.RESENYA (como String)
     */
    public static final String RESENYA_STR = RESENYA.name();
    public static final String VALORACION_STR = VALORACION.name();
    public static final String FAVORITO_ADD_STR = FAVORITO_ADD.name();
    public static final String FAVORITO_REMOVE_STR = FAVORITO_REMOVE.name();
    public static final String LISTA_CREADA_STR = LISTA_CREADA.name();
    public static final String LISTA_ACTUALIZADA_STR = LISTA_ACTUALIZADA.name();
    public static final String ANIME_AGREGADO_LISTA_STR = ANIME_AGREGADO_LISTA.name();
    public static final String SEGUIR_USUARIO_STR = SEGUIR_USUARIO.name();
    public static final String RESENYA_UPDATE_STR = RESENYA_UPDATE.name();
    public static final String RESENYA_DELETE_STR = RESENYA_DELETE.name();

    /**
     * Verifica si el tipo de actividad es relacionado con favoritos
     */
    public boolean isFavoritoActivity() {
        return this == FAVORITO_ADD || this == FAVORITO_REMOVE || this == FAVORITO_REMOVE_ALT;
    }

    /**
     * Verifica si el tipo de actividad es relacionado con reseñas
     */
    public boolean isResenyaActivity() {
        return this == RESENYA || this == RESENYA_CREATE || this == RESENYA_UPDATE || this == RESENYA_DELETE;
    }

    /**
     * Verifica si el tipo de actividad es relacionado con usuarios
     */
    public boolean isUserActivity() {
        return this == USER_LOGIN || this == USER_LOGOUT || this == USER_REGISTER ||
                this == USER_UPDATE_PROFILE || this == USER_FOLLOW || this == USER_UNFOLLOW ||
                this == SEGUIR_USUARIO;
    }

    /**
     * Verifica si el tipo de actividad es relacionado con listas
     */
    public boolean isListaActivity() {
        return this == LISTA_CREADA || this == LISTA_ACTUALIZADA || this == ANIME_AGREGADO_LISTA;
    }

    @Override
    public String toString() {
        return this.name() + " - " + this.descripcion;
    }
}