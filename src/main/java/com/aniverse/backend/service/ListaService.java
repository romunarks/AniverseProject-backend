package com.aniverse.backend.service;

import com.aniverse.backend.dto.*;
import com.aniverse.backend.exception.DuplicateResourceException;
import com.aniverse.backend.exception.ResourceNotFoundException;
import com.aniverse.backend.model.*;
import com.aniverse.backend.repository.*;
import com.aniverse.backend.util.ActividadTipo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ListaService {

    private final ListaRepository listaRepository;
    private final ListaAnimeRepository listaAnimeRepository;
    private final UsuarioService usuarioService;
    private final AnimeService animeService;
    private final ActividadService actividadService;

    public ListaService(
            ListaRepository listaRepository,
            ListaAnimeRepository listaAnimeRepository,
            UsuarioService usuarioService,
            AnimeService animeService, ActividadService actividadService) {
        this.listaRepository = listaRepository;
        this.listaAnimeRepository = listaAnimeRepository;
        this.usuarioService = usuarioService;
        this.animeService = animeService;
        this.actividadService = actividadService;
    }

    /**
     * Crear una nueva lista para un usuario
     */
    @Transactional
    public ListaDTO createLista(Long usuarioId, ListaCreateDTO listaDTO) {
        Usuario usuario = usuarioService.getUsuarioEntityById(usuarioId);

        // Verificar permisos
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!usuario.getEmail().equals(currentUsername)) {
            throw new AccessDeniedException("No puedes crear listas para otro usuario");
        }

        Lista lista = new Lista();
        lista.setNombre(listaDTO.getNombre());
        lista.setDescripcion(listaDTO.getDescripcion());
        lista.setPublica(listaDTO.isPublica());
        lista.setUsuario(usuario);

        Lista savedLista = listaRepository.save(lista);
        // Registrar actividad
        actividadService.registrarActividad(
                usuarioId,
                String.valueOf(ActividadTipo.LISTA_CREADA),
                savedLista.getId(),
                "LISTA",
                Map.of("publica", savedLista.isPublica())
        );

        return new ListaDTO(
                savedLista.getId(),
                savedLista.getNombre(),
                savedLista.getDescripcion(),
                savedLista.getUsuario().getId(),
                savedLista.getUsuario().getNombre(),
                savedLista.isPublica(),
                savedLista.getCreatedAt(),
                0 // Cantidad de animes inicialmente 0
        );
    }

    /**
     * Obtener todas las listas de un usuario
     */
    // Actualizar método getListasByUsuario
    @Transactional(readOnly = true)
    public Page<ListaDTO> getListasByUsuario(Long usuarioId, Pageable pageable) {
        Usuario usuario = usuarioService.getUsuarioEntityById(usuarioId);

        return listaRepository.findByUsuarioAndEliminadoFalse(usuario, pageable)
                .map(lista -> new ListaDTO(
                        lista.getId(),
                        lista.getNombre(),
                        lista.getDescripcion(),
                        lista.getUsuario().getId(),
                        lista.getUsuario().getNombre(),
                        lista.isPublica(),
                        lista.getCreatedAt(),
                        lista.getAnimes().size()
                ));
    }

    /**
     * Obtener listas públicas
     */
    // Actualizar método getPublicLists
    public Page<ListaDTO> getPublicLists(Pageable pageable) {
        return listaRepository.findPublicListsActive(pageable)
                .map(lista -> new ListaDTO(
                        lista.getId(),
                        lista.getNombre(),
                        lista.getDescripcion(),
                        lista.getUsuario().getId(),
                        lista.getUsuario().getNombre(),
                        lista.isPublica(),
                        lista.getCreatedAt(),
                        lista.getAnimes().size()
                ));
    }

    /**
     * Obtener una lista por ID
     */
    // Actualizar método getListaById para considerar soft delete
    @Transactional(readOnly = true) //
    public Lista getListaById(Long id) {
        return listaRepository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lista", id));
    }
    /**
     * Obtener detalles de una lista con sus animes
     */
    @Transactional(readOnly = true)
    public ListaDTO getListaDetalles(Long id) {
        Lista lista = getListaById(id);

        // Verificar permisos si la lista no es pública
        if (!lista.isPublica()) {
            String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
            if (!lista.getUsuario().getEmail().equals(currentUsername)) {
                throw new AccessDeniedException("No tienes acceso a esta lista privada");
            }
        }

        return new ListaDTO(
                lista.getId(),
                lista.getNombre(),
                lista.getDescripcion(),
                lista.getUsuario().getId(),
                lista.getUsuario().getNombre(),
                lista.isPublica(),
                lista.getCreatedAt(),
                lista.getAnimes().size()
        );
    }

    /**
     * Actualizar una lista
     */
    @Transactional
    public ListaDTO updateLista(Long id, ListaCreateDTO listaDTO) {
        Lista lista = getListaById(id);

        // Verificar permisos
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!lista.getUsuario().getEmail().equals(currentUsername)) {
            throw new AccessDeniedException("No puedes modificar esta lista");
        }

        if (listaDTO.getNombre() != null) {
            lista.setNombre(listaDTO.getNombre());
        }

        if (listaDTO.getDescripcion() != null) {
            lista.setDescripcion(listaDTO.getDescripcion());
        }

        lista.setPublica(listaDTO.isPublica());

        Lista updatedLista = listaRepository.save(lista);

        return new ListaDTO(
                updatedLista.getId(),
                updatedLista.getNombre(),
                updatedLista.getDescripcion(),
                updatedLista.getUsuario().getId(),
                updatedLista.getUsuario().getNombre(),
                updatedLista.isPublica(),
                updatedLista.getCreatedAt(),
                updatedLista.getAnimes().size()
        );
    }

    /**
     * Eliminar una lista
     */
    // Modificar método deleteLista para usar soft delete
    @Transactional
    public void softDeleteLista(Long id) {
        Lista lista = getListaById(id);

        // Verificar permisos
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!lista.getUsuario().getEmail().equals(currentUsername)) {
            throw new AccessDeniedException("No puedes eliminar esta lista");
        }

        lista.setEliminado(true);
        lista.setFechaEliminacion(LocalDateTime.now());

        listaRepository.save(lista);
    }

    /**
     * Añadir un anime a una lista
     */
    @Transactional
    public ListaAnimeDTO addAnimeToLista(Long listaId, ListaAnimeCreateDTO animeDTO) {
        Lista lista = getListaById(listaId);

        // Verificar permisos
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!lista.getUsuario().getEmail().equals(currentUsername)) {
            throw new AccessDeniedException("No puedes modificar esta lista");
        }

        Anime anime = animeService.getAnimeById(animeDTO.getAnimeId());

        // Verificar si el anime ya está en la lista
        if (listaAnimeRepository.existsByListaAndAnime(lista, anime)) {
            throw new DuplicateResourceException("Este anime ya está en la lista");
        }

        ListaAnime listaAnime = new ListaAnime();
        listaAnime.setLista(lista);
        listaAnime.setAnime(anime);
        listaAnime.setNotas(animeDTO.getNotas());
        listaAnime.setEpisodiosVistos(animeDTO.getEpisodiosVistos());
        listaAnime.setEstado(animeDTO.getEstado());

        ListaAnime savedListaAnime = listaAnimeRepository.save(listaAnime);

        // Registrar actividad
        actividadService.registrarActividad(
                lista.getUsuario().getId(),
                String.valueOf(ActividadTipo.ANIME_AGREGADO_LISTA),
                lista.getId(),
                "LISTA",
                Map.of("animeId", anime.getId(),
                        "animeTitulo", anime.getTitulo())
        );

        return new ListaAnimeDTO(
                savedListaAnime.getId(),
                savedListaAnime.getAnime().getId(),
                savedListaAnime.getAnime().getTitulo(),
                savedListaAnime.getAnime().getImagenUrl(),
                savedListaAnime.getNotas(),
                savedListaAnime.getEpisodiosVistos(),
                savedListaAnime.getEstado()
        );
    }

    /**
     * Obtener animes de una lista
     */
    @Transactional(readOnly = true) //
    public List<ListaAnimeDTO> getAnimesFromLista(Long listaId) {
        Lista lista = getListaById(listaId);

        // Verificar permisos si la lista no es pública
        if (!lista.isPublica()) {
            String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
            if (!lista.getUsuario().getEmail().equals(currentUsername)) {
                throw new AccessDeniedException("No tienes acceso a esta lista privada");
            }
        }

        return listaAnimeRepository.findByLista(lista).stream()
                .map(listaAnime -> new ListaAnimeDTO(
                        listaAnime.getId(),
                        listaAnime.getAnime().getId(),
                        listaAnime.getAnime().getTitulo(),
                        listaAnime.getAnime().getImagenUrl(),
                        listaAnime.getNotas(),
                        listaAnime.getEpisodiosVistos(),
                        listaAnime.getEstado()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Actualizar un anime en una lista
     */
    @Transactional
    public ListaAnimeDTO updateAnimeInLista(Long listaId, Long animeId, ListaAnimeCreateDTO updateDTO) {
        Lista lista = getListaById(listaId);

        // Verificar permisos
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!lista.getUsuario().getEmail().equals(currentUsername)) {
            throw new AccessDeniedException("No puedes modificar esta lista");
        }

        Anime anime = animeService.getAnimeById(animeId);

        ListaAnime listaAnime = listaAnimeRepository.findByListaAndAnime(lista, anime)
                .orElseThrow(() -> new ResourceNotFoundException("Anime no encontrado en esta lista"));

        if (updateDTO.getNotas() != null) {
            listaAnime.setNotas(updateDTO.getNotas());
        }

        if (updateDTO.getEpisodiosVistos() != null) {
            listaAnime.setEpisodiosVistos(updateDTO.getEpisodiosVistos());
        }

        if (updateDTO.getEstado() != null) {
            listaAnime.setEstado(updateDTO.getEstado());
        }

        ListaAnime updatedListaAnime = listaAnimeRepository.save(listaAnime);

        return new ListaAnimeDTO(
                updatedListaAnime.getId(),
                updatedListaAnime.getAnime().getId(),
                updatedListaAnime.getAnime().getTitulo(),
                updatedListaAnime.getAnime().getImagenUrl(),
                updatedListaAnime.getNotas(),
                updatedListaAnime.getEpisodiosVistos(),
                updatedListaAnime.getEstado()
        );
    }

    /**
     * Eliminar un anime de una lista
     */
    @Transactional
    public void removeAnimeFromLista(Long listaId, Long animeId) {
        Lista lista = getListaById(listaId);

        // Verificar permisos
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!lista.getUsuario().getEmail().equals(currentUsername)) {
            throw new AccessDeniedException("No puedes modificar esta lista");
        }

        Anime anime = animeService.getAnimeById(animeId);

        ListaAnime listaAnime = listaAnimeRepository.findByListaAndAnime(lista, anime)
                .orElseThrow(() -> new ResourceNotFoundException("Anime no encontrado en esta lista"));

        listaAnimeRepository.delete(listaAnime);
    }

    // Añadir método para restaurar listas eliminadas
    @Transactional
    public ListaDTO restoreLista(Long id) {
        Lista lista = listaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lista", id));

        // Verificar permisos
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!lista.getUsuario().getEmail().equals(currentUsername)) {
            throw new AccessDeniedException("No puedes restaurar esta lista");
        }

        if (!lista.isEliminado()) {
            throw new IllegalStateException("La lista no está eliminada, no se puede restaurar");
        }

        lista.setEliminado(false);
        lista.setFechaEliminacion(null);

        Lista listaRestaurada = listaRepository.save(lista);

        return new ListaDTO(
                listaRestaurada.getId(),
                listaRestaurada.getNombre(),
                listaRestaurada.getDescripcion(),
                listaRestaurada.getUsuario().getId(),
                listaRestaurada.getUsuario().getNombre(),
                listaRestaurada.isPublica(),
                listaRestaurada.getCreatedAt(),
                listaRestaurada.getAnimes().size()
        );
    }
    // Añadir método para obtener listas eliminadas (para administradores)
    @Transactional(readOnly = true)
    public Page<ListaDTO> getDeletedListas(Pageable pageable) {
        return listaRepository.findAllDeleted(pageable)
                .map(lista -> new ListaDTO(
                        lista.getId(),
                        lista.getNombre(),
                        lista.getDescripcion(),
                        lista.getUsuario().getId(),
                        lista.getUsuario().getNombre(),
                        lista.isPublica(),
                        lista.getCreatedAt(),
                        lista.getAnimes().size()
                ));
    }
}