package com.aniverse.backend.service;

import com.aniverse.backend.dto.FavoritoDTO;
import com.aniverse.backend.exception.DuplicateResourceException;
import com.aniverse.backend.exception.ResourceNotFoundException;
import com.aniverse.backend.model.Anime;
import com.aniverse.backend.model.Favorito;
import com.aniverse.backend.model.Usuario;
import com.aniverse.backend.repository.AnimeRepository;
import com.aniverse.backend.repository.FavoritoRepository;
import com.aniverse.backend.repository.UsuarioRepository;
import com.aniverse.backend.util.ActividadTipo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class FavoritoService {

    private static final Logger log = LoggerFactory.getLogger(FavoritoService.class);

    private final FavoritoRepository favoritoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AnimeRepository animeRepository;
    private final ExternalAnimeService externalAnimeService;
    private final ActividadService actividadService;

    public FavoritoService(FavoritoRepository favoritoRepository,
                           UsuarioRepository usuarioRepository,
                           AnimeRepository animeRepository,
                           ExternalAnimeService externalAnimeService,
                           ActividadService actividadService) {
        this.favoritoRepository = favoritoRepository;
        this.usuarioRepository = usuarioRepository;
        this.animeRepository = animeRepository;
        this.externalAnimeService = externalAnimeService;
        this.actividadService = actividadService;
    }

    // ===============================================
    // MÉTODOS PRINCIPALES DE FAVORITOS
    // ===============================================

    /**
     * Agrega un anime a favoritos del usuario
     * Maneja tanto animes locales (por ID) como externos (por JikanId)
     */
    @Transactional
    public FavoritoDTO agregarFavorito(Long usuarioId, Long animeId, Long jikanId) {
        log.info("Agregando favorito - Usuario: {}, AnimeId: {}, JikanId: {}",
                usuarioId, animeId, jikanId);

        // Validar usuario
        Usuario usuario = usuarioRepository.findByIdAndEliminadoFalse(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + usuarioId));

        Anime anime = null;

        // Estrategia 1: Si tenemos animeId (anime local), usarlo directamente
        if (animeId != null) {
            anime = animeRepository.findByIdAndEliminadoFalse(animeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Anime no encontrado con ID: " + animeId));
            log.debug("Usando anime local con ID: {}", animeId);
        }
        // Estrategia 2: Si solo tenemos jikanId, buscar/crear el anime
        else if (jikanId != null) {
            // Primero buscar si ya existe localmente
            Optional<Anime> animeLocal = animeRepository.findByJikanIdAndEliminadoFalse(jikanId);

            if (animeLocal.isPresent()) {
                anime = animeLocal.get();
                log.debug("Usando anime existente con JikanId: {}", jikanId);
            } else {
                // Si no existe, obtenerlo de la API externa y guardarlo
                anime = externalAnimeService.findOrSaveExternalAnime(jikanId);
                log.info("Anime obtenido de API externa y guardado - JikanId: {}, Título: {}",
                        jikanId, anime.getTitulo());
            }
        } else {
            throw new IllegalArgumentException("Se requiere animeId o jikanId para agregar a favoritos");
        }

        // Verificar si ya es favorito
        boolean yaEsFavorito = favoritoRepository.existsByUsuarioAndAnime(usuario, anime);
        if (yaEsFavorito) {
            throw new DuplicateResourceException("Este anime ya está en tus favoritos");
        }

        // Crear y guardar favorito
        Favorito favorito = new Favorito(usuario, anime);
        Favorito savedFavorito = favoritoRepository.save(favorito);

        // Registrar actividad
        try {
            actividadService.registrarActividad(
                    usuarioId,
                    ActividadTipo.FAVORITO_ADD.name(),
                    anime.getId(),
                    "ANIME",
                    null
            );
        } catch (Exception e) {
            log.warn("Error registrando actividad de favorito: {}", e.getMessage());
        }

        log.info("Favorito agregado exitosamente - Usuario: {}, Anime: {}",
                usuario.getNombre(), anime.getTitulo());

        return convertToDTO(savedFavorito);
    }

    /**
     * Elimina un anime de favoritos del usuario por JikanId
     */
    @Transactional
    public boolean eliminarFavoritoPorJikanId(Long usuarioId, Long jikanId) {
        log.info("Eliminando favorito - Usuario: {}, JikanId: {}", usuarioId, jikanId);

        try {
            // Buscar el favorito por usuario y jikanId del anime
            Optional<Favorito> favorito = favoritoRepository.findByUsuario_IdAndAnime_JikanId(usuarioId, jikanId);

            if (favorito.isPresent()) {
                Favorito favoritoAEliminar = favorito.get();

                // Registrar actividad antes de eliminar
                try {
                    actividadService.registrarActividad(
                            usuarioId,
                            ActividadTipo.FAVORITO_REMOVE.name(),
                            favoritoAEliminar.getAnime().getId(),
                            "ANIME",
                            null
                    );
                } catch (Exception e) {
                    log.warn("Error registrando actividad de eliminación de favorito: {}", e.getMessage());
                }

                favoritoRepository.delete(favoritoAEliminar);
                log.info("Favorito eliminado exitosamente - Usuario: {}, Anime: {}",
                        usuarioId, favoritoAEliminar.getAnime().getTitulo());
                return true;
            } else {
                log.warn("Favorito no encontrado para eliminar - Usuario: {}, JikanId: {}", usuarioId, jikanId);
                return false;
            }
        } catch (Exception e) {
            log.error("Error eliminando favorito - Usuario: {}, JikanId: {}: {}",
                    usuarioId, jikanId, e.getMessage());
            return false;
        }
    }

    /**
     * Alterna el estado de favorito (agregar/quitar)
     */
    @Transactional
    public Map<String, Object> toggleFavorito(Long usuarioId, Long jikanId, Map<String, Object> animeData) {
        log.info("Toggle favorito - Usuario: {}, JikanId: {}", usuarioId, jikanId);

        Map<String, Object> result = new HashMap<>();

        try {
            // Verificar si ya es favorito
            boolean esFavorito = existeFavoritoPorJikanId(usuarioId, jikanId);

            if (esFavorito) {
                // Eliminar favorito
                boolean eliminado = eliminarFavoritoPorJikanId(usuarioId, jikanId);
                result.put("success", eliminado);
                result.put("action", "removed");
                result.put("isFavorite", false);
                result.put("message", eliminado ? "Anime eliminado de favoritos" : "Error eliminando favorito");
            } else {
                // Agregar favorito - necesitamos crear el anime si no existe
                Usuario usuario = usuarioRepository.findByIdAndEliminadoFalse(usuarioId)
                        .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

                // Extraer datos del anime del payload
                String titulo = (String) animeData.get("titulo");
                String imagenUrl = (String) animeData.get("imagenUrl");
                String tipo = (String) animeData.get("tipo");
                Integer episodios = animeData.get("episodios") != null ?
                        Integer.valueOf(animeData.get("episodios").toString()) : null;
                String estado = (String) animeData.get("estado");
                Double puntuacion = animeData.get("puntuacion") != null ?
                        Double.valueOf(animeData.get("puntuacion").toString()) : null;
                String generos = (String) animeData.get("generos");
                String sinopsis = (String) animeData.get("sinopsis");

                // Crear o encontrar el anime
                Anime anime = externalAnimeService.findOrCreateAnimeByJikanId(
                        jikanId, titulo, imagenUrl, tipo, episodios, estado, puntuacion, generos, sinopsis
                );

                // Crear favorito
                Favorito favorito = new Favorito(usuario, anime);
                Favorito savedFavorito = favoritoRepository.save(favorito);

                // Registrar actividad
                try {
                    actividadService.registrarActividad(
                            usuarioId,
                            ActividadTipo.FAVORITO_ADD.name(),
                            anime.getId(),
                            "ANIME",
                            null
                    );
                } catch (Exception e) {
                    log.warn("Error registrando actividad: {}", e.getMessage());
                }

                result.put("success", true);
                result.put("action", "added");
                result.put("isFavorite", true);
                result.put("message", "Anime agregado a favoritos");
                result.put("data", Map.of(
                        "favoritoId", savedFavorito.getId(),
                        "animeId", anime.getId(),
                        "animeTitulo", anime.getTitulo()
                ));
            }

            result.put("jikanId", jikanId);
            return result;

        } catch (Exception e) {
            log.error("Error en toggle favorito: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "Error interno: " + e.getMessage());
            return result;
        }
    }

    // ===============================================
    // MÉTODOS DE CONSULTA
    // ===============================================

    /**
     * Verifica si un anime es favorito del usuario por JikanId
     */
    public boolean existeFavoritoPorJikanId(Long usuarioId, Long jikanId) {
        try {
            return favoritoRepository.existsByUsuario_IdAndAnime_JikanId(usuarioId, jikanId);
        } catch (Exception e) {
            log.error("Error verificando favorito - Usuario: {}, JikanId: {}: {}",
                    usuarioId, jikanId, e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene todos los favoritos con paginación
     */
    public Page<FavoritoDTO> getAllFavoritos(Pageable pageable) {
        return favoritoRepository.findAll(pageable)
                .map(this::convertToDTO);
    }

    /**
     * Obtiene favoritos de un usuario específico con paginación
     */
    @Transactional(readOnly = true)
    public Page<FavoritoDTO> getFavoritosByUsuario(Long usuarioId, int page, int size) {
        // Validar que el usuario exista
        Usuario usuario = usuarioRepository.findByIdAndEliminadoFalse(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + usuarioId));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaAgregado"));
        Page<Favorito> favoritos = favoritoRepository.findByUsuario(usuario, pageable);

        return favoritos.map(this::convertToDTO);
    }

    /**
     * Obtiene la lista de animes favoritos de un usuario (para reseñas)
     */
    public List<Anime> getAnimesFavoritosUsuario(Long usuarioId) {
        Usuario usuario = usuarioRepository.findByIdAndEliminadoFalse(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + usuarioId));

        List<Favorito> favoritos = favoritoRepository.findByUsuario(usuario, Pageable.unpaged()).getContent();

        return favoritos.stream()
                .map(Favorito::getAnime)
                .filter(anime -> !anime.isEliminado()) // Solo animes activos
                .toList();
    }

    // ===============================================
    // MÉTODOS DE UTILIDAD Y CONVERSIÓN
    // ===============================================

    /**
     * Convierte entidad Favorito a DTO
     */
    private FavoritoDTO convertToDTO(Favorito favorito) {
        FavoritoDTO dto = new FavoritoDTO(
                favorito.getId(),
                favorito.getUsuario().getId(),
                favorito.getUsuario().getNombre(),
                favorito.getAnime().getId(),
                favorito.getAnime().getTitulo()
        );

        // Establecer información adicional del anime
        dto.setAnimeFromEntity(favorito.getAnime());
        dto.setFechaAgregado(favorito.getFechaAgregado());

        return dto;
    }

    /**
     * Elimina favorito por ID (método legacy)
     */
    @Transactional
    public void eliminarFavorito(Long favoritoId) {
        Favorito favorito = favoritoRepository.findById(favoritoId)
                .orElseThrow(() -> new ResourceNotFoundException("Favorito no encontrado con ID: " + favoritoId));

        // Registrar actividad antes de eliminar
        try {
            actividadService.registrarActividad(
                    favorito.getUsuario().getId(),
                    ActividadTipo.FAVORITO_REMOVE.name(),
                    favorito.getAnime().getId(),
                    "ANIME",
                    null
            );
        } catch (Exception e) {
            log.warn("Error registrando actividad: {}", e.getMessage());
        }

        favoritoRepository.delete(favorito);
        log.info("Favorito eliminado por ID: {}", favoritoId);
    }

    /**
     * Verifica si existe favorito por usuario y anime
     */
    public boolean existsByUsuarioAndAnime(Usuario usuario, Anime anime) {
        return favoritoRepository.existsByUsuarioAndAnime(usuario, anime);
    }
}