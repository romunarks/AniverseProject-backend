package com.aniverse.backend.service;

import com.aniverse.backend.dto.ActividadDTO;
import com.aniverse.backend.exception.ResourceNotFoundException;
import com.aniverse.backend.model.Actividad;
import com.aniverse.backend.model.Anime;
import com.aniverse.backend.model.Lista;
import com.aniverse.backend.model.Resenya;
import com.aniverse.backend.model.Usuario;
import com.aniverse.backend.repository.*;
import com.aniverse.backend.util.ActividadTipo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ActividadService {

    private final ActividadRepository actividadRepository;
    private final UsuarioRepository usuarioRepository;
    private final AnimeRepository animeRepository;
    private final ResenyaRepository resenyaRepository;
    private final ListaRepository listaRepository;
    private final ObjectMapper objectMapper;
    private final FavoritoRepository favoritoRepository;
    private final VotacionRepository votacionRepository;
    private final WebSocketNotificationService webSocketNotificationService;

    @Value("${actividad.duracion.dias:60}")
    private int diasRetencionActividades;

    public ActividadService(ActividadRepository actividadRepository,
                            UsuarioRepository usuarioRepository,
                            AnimeRepository animeRepository,
                            ResenyaRepository resenyaRepository,
                            ListaRepository listaRepository,
                            ObjectMapper objectMapper,
                            FavoritoRepository favoritoRepository,
                            VotacionRepository votacionRepository,
                            WebSocketNotificationService webSocketNotificationService) {
        this.actividadRepository = actividadRepository;
        this.usuarioRepository = usuarioRepository;
        this.animeRepository = animeRepository;
        this.resenyaRepository = resenyaRepository;
        this.listaRepository = listaRepository;
        this.objectMapper = objectMapper;
        this.favoritoRepository = favoritoRepository;
        this.votacionRepository = votacionRepository;
        this.webSocketNotificationService = webSocketNotificationService;
    }


    /**
     * Registrar una nueva actividad
     */
    @Transactional
    public void registrarActividad(Long usuarioId, String tipo, Long objetoId, String objetoTipo, Map<String, Object> datos) {
        Usuario usuario = usuarioRepository.findByIdAndEliminadoFalse(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", usuarioId));

        Actividad actividad = new Actividad();
        actividad.setUsuario(usuario);
        actividad.setTipo(tipo);
        actividad.setObjetoId(objetoId);
        actividad.setObjetoTipo(objetoTipo);
        actividad.setFecha(LocalDateTime.now());

        // Convertir datos adicionales a JSON
        if (datos != null && !datos.isEmpty()) {
            try {
                actividad.setDatos(objectMapper.writeValueAsString(datos));
            } catch (JsonProcessingException e) {
                // Log error
                e.printStackTrace();
            }
        }
        Actividad savedActividad = actividadRepository.save(actividad);

        // Convertir a DTO y broadcast
        ActividadDTO actividadDTO = convertirADTO(savedActividad);
        webSocketNotificationService.broadcastActivity(actividadDTO);

        actividadRepository.save(actividad);
    }

    /**
     * Obtener actividades de un usuario
     */
    public Page<ActividadDTO> getActividadesUsuario(Long usuarioId, Pageable pageable) {
        Usuario usuario = usuarioRepository.findByIdAndEliminadoFalse(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", usuarioId));

        Page<Actividad> actividades = actividadRepository.findByUsuarioOrderByFechaDesc(usuario, pageable);

        return actividades.map(this::convertirADTO);
    }

    /**
     * Obtener feed de actividades para un usuario (actividades de usuarios seguidos)
     */
    public Page<ActividadDTO> getFeedActividades(Long usuarioId, Pageable pageable) {
        Usuario usuario = usuarioRepository.findByIdAndEliminadoFalse(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", usuarioId));

        Page<Actividad> actividades = actividadRepository.findFeedActivities(usuario, pageable);

        return actividades.map(this::convertirADTO);
    }

    /**
     * Obtener actividades relacionadas con un anime
     */
    public Page<ActividadDTO> getActividadesAnime(Long animeId, Pageable pageable) {
        // Verificar que el anime existe
        if (!animeRepository.existsById(animeId)) {
            throw new ResourceNotFoundException("Anime", animeId);
        }

        Page<Actividad> actividades = actividadRepository.findByAnime(animeId, pageable);

        return actividades.map(this::convertirADTO);
    }

    /**
     * Eliminar actividades antiguas (tarea programada)
     */
    @Scheduled(cron = "0 0 2 * * ?") // Ejecutar diariamente a las 2 AM
    @Transactional
    public void limpiarActividadesAntiguas() {
        LocalDateTime fechaLimite = LocalDateTime.now().minusDays(diasRetencionActividades);
        actividadRepository.deleteByFechaBefore(fechaLimite);
    }

    /**
     * Convertir entidad a DTO
     */
    private ActividadDTO convertirADTO(Actividad actividad) {
        ActividadDTO dto = new ActividadDTO();
        dto.setId(actividad.getId());
        dto.setUsuarioId(actividad.getUsuario().getId());
        dto.setUsuarioNombre(actividad.getUsuario().getNombre());
        dto.setTipo(actividad.getTipo());
        dto.setFecha(actividad.getFecha());
        dto.setObjetoId(actividad.getObjetoId());
        dto.setObjetoTipo(actividad.getObjetoTipo());

        // Obtener nombre del objeto según su tipo
        String objetoNombre = "";
        String mensaje = "";
        String url = "";

        try {
            switch (actividad.getObjetoTipo()) {
                case "ANIME":
                    Anime anime = animeRepository.findById(actividad.getObjetoId())
                            .orElse(null);
                    if (anime != null) {
                        objetoNombre = anime.getTitulo();
                        url = "/anime/" + anime.getId();

                        // Generar mensaje según el tipo de actividad
                        switch (actividad.getTipo()) {
                            case "VALORACION":
                                mensaje = actividad.getUsuario().getNombre() + " ha valorado " + objetoNombre;
                                break;
                            case "FAVORITO_ADD":
                                mensaje = actividad.getUsuario().getNombre() + " ha añadido " + objetoNombre + " a sus favoritos";
                                break;
                            default:
                                mensaje = actividad.getUsuario().getNombre() + " ha interactuado con " + objetoNombre;
                        }
                    }
                    break;

                case "RESENYA":
                    Resenya resenya = resenyaRepository.findById(actividad.getObjetoId())
                            .orElse(null);
                    if (resenya != null) {
                        objetoNombre = "reseña de " + resenya.getAnime().getTitulo();
                        url = "/anime/" + resenya.getAnime().getId() + "/resenas/" + resenya.getId();
                        mensaje = actividad.getUsuario().getNombre() + " ha publicado una " + objetoNombre;
                    }
                    break;

                case "LISTA":
                    Lista lista = listaRepository.findById(actividad.getObjetoId())
                            .orElse(null);
                    if (lista != null) {
                        objetoNombre = lista.getNombre();
                        url = "/usuario/" + lista.getUsuario().getId() + "/listas/" + lista.getId();

                        switch (actividad.getTipo()) {
                            case "LISTA_CREADA":
                                mensaje = actividad.getUsuario().getNombre() + " ha creado la lista " + objetoNombre;
                                break;
                            case "LISTA_ACTUALIZADA":
                                mensaje = actividad.getUsuario().getNombre() + " ha actualizado la lista " + objetoNombre;
                                break;
                            case "ANIME_AGREGADO_LISTA":
                                // Extraer información del anime de los datos
                                if (actividad.getDatos() != null) {
                                    try {
                                        Map<String, Object> datos = objectMapper.readValue(actividad.getDatos(), Map.class);
                                        Long animeId = ((Number) datos.get("animeId")).longValue();
                                        String animeTitulo = (String) datos.get("animeTitulo");

                                        objetoNombre = animeTitulo;
                                        mensaje = actividad.getUsuario().getNombre() + " ha añadido " + animeTitulo + " a la lista " + lista.getNombre();
                                    } catch (Exception e) {
                                        mensaje = actividad.getUsuario().getNombre() + " ha añadido un anime a la lista " + objetoNombre;
                                    }
                                } else {
                                    mensaje = actividad.getUsuario().getNombre() + " ha añadido un anime a la lista " + objetoNombre;
                                }
                                break;
                            default:
                                mensaje = actividad.getUsuario().getNombre() + " ha interactuado con la lista " + objetoNombre;
                        }
                    }
                    break;

                case "USUARIO":
                    Usuario usuarioObjeto = usuarioRepository.findById(actividad.getObjetoId())
                            .orElse(null);
                    if (usuarioObjeto != null) {
                        objetoNombre = usuarioObjeto.getNombre();
                        url = "/usuario/" + usuarioObjeto.getId();

                        if ("SEGUIR_USUARIO".equals(actividad.getTipo())) {
                            mensaje = actividad.getUsuario().getNombre() + " ha comenzado a seguir a " + objetoNombre;
                        } else {
                            mensaje = actividad.getUsuario().getNombre() + " ha interactuado con " + objetoNombre;
                        }
                    }
                    break;

                default:
                    mensaje = actividad.getUsuario().getNombre() + " ha realizado una actividad";
            }
        } catch (Exception e) {
            // En caso de error, usar un mensaje genérico
            mensaje = actividad.getUsuario().getNombre() + " ha realizado una actividad";
        }

        dto.setObjetoNombre(objetoNombre);
        dto.setMensaje(mensaje);
        dto.setUrl(url);

        return dto;
    }
    // En ActividadService.java, añadir:

    /**
     * Obtener feed personalizado de actividades para un usuario
     * Prioriza actividades relacionadas con animes favoritos y géneros preferidos
     */
    public Page<ActividadDTO> getFeedPersonalizado(Long usuarioId, Pageable pageable) {
        Usuario usuario = usuarioRepository.findByIdAndEliminadoFalse(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", usuarioId));

        // Obtener IDs de animes favoritos del usuario
        List<Long> animesFavoritosIds = favoritoRepository.findByUsuario(usuario)
                .stream()
                .map(favorito -> favorito.getAnime().getId())
                .collect(Collectors.toList());

        // Obtener géneros preferidos basados en favoritos y valoraciones
        Set<String> generosPreferidos = new HashSet<>();

        favoritoRepository.findByUsuario(usuario).forEach(favorito -> {
            if (favorito.getAnime().getGenero() != null) {
                generosPreferidos.add(favorito.getAnime().getGenero());
            }
        });

        votacionRepository.findByUsuario(usuario, pageable).forEach(votacion -> {
            if (votacion.getPuntuacion() >= 4.0 && votacion.getAnime().getGenero() != null) {
                generosPreferidos.add(votacion.getAnime().getGenero());
            }
        });

        // Obtener el feed básico
        Page<Actividad> feedBasico = actividadRepository.findFeedActivities(usuario, pageable);

        // Personalizar el orden basado en relevancia
        // Este es un enfoque simplificado. En una implementación real,
        // se podría usar un sistema de puntuación más sofisticado o incluso
        // implementar un algoritmo de recomendación.

        List<Actividad> actividadesOrdenadas = feedBasico.getContent().stream()
                .sorted((a1, a2) -> {
                    int score1 = calcularPuntuacionRelevancia(a1, animesFavoritosIds, generosPreferidos);
                    int score2 = calcularPuntuacionRelevancia(a2, animesFavoritosIds, generosPreferidos);

                    if (score1 != score2) {
                        return Integer.compare(score2, score1); // Mayor puntuación primero
                    } else {
                        return a2.getFecha().compareTo(a1.getFecha()); // Más reciente primero
                    }
                })
                .collect(Collectors.toList());

        // Crear un Page a partir de la lista ordenada
        // Nota: Esto es una simplificación, en una implementación real
        // deberías considerar la paginación apropiadamente
        Page<Actividad> actividadesPersonalizadas = new PageImpl<>(
                actividadesOrdenadas,
                pageable,
                feedBasico.getTotalElements()
        );

        return actividadesPersonalizadas.map(this::convertirADTO);
    }

    /**
     * Calcular puntuación de relevancia para una actividad
     */
    private int calcularPuntuacionRelevancia(Actividad actividad,
                                             List<Long> animesFavoritosIds,
                                             Set<String> generosPreferidos) {
        int puntuacion = 0;

        // Actividades relacionadas con animes favoritos tienen mayor relevancia
        if ("ANIME".equals(actividad.getObjetoTipo()) &&
                animesFavoritosIds.contains(actividad.getObjetoId())) {
            puntuacion += 5;
        }

        // Actividades relacionadas con géneros preferidos
        if ("ANIME".equals(actividad.getObjetoTipo())) {
            try {
                Anime anime = animeRepository.findById(actividad.getObjetoId()).orElse(null);
                if (anime != null && anime.getGenero() != null &&
                        generosPreferidos.contains(anime.getGenero())) {
                    puntuacion += 3;
                }
            } catch (Exception e) {
                // Ignorar errores y continuar
            }
        }

        // Tipos de actividad que suelen ser más interesantes
        if (ActividadTipo.RESENYA.equals(actividad.getTipo())) {
            puntuacion += 2;
        }

        // Actividades más recientes tienen mayor relevancia
        if (actividad.getFecha().isAfter(LocalDateTime.now().minusDays(3))) {
            puntuacion += 1;
        }

        return puntuacion;
    }
}