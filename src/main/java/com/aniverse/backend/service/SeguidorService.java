package com.aniverse.backend.service;

import com.aniverse.backend.dto.SeguidorDTO;
import com.aniverse.backend.dto.UsuarioResumidoDTO;
import com.aniverse.backend.exception.DuplicateResourceException;
import com.aniverse.backend.exception.ResourceNotFoundException;
import com.aniverse.backend.model.Seguidor;
import com.aniverse.backend.model.Usuario;
import com.aniverse.backend.repository.SeguidorRepository;
import com.aniverse.backend.repository.UsuarioRepository;
import com.aniverse.backend.util.ActividadTipo;
import com.aniverse.backend.util.NotificacionTipo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class SeguidorService {

    private final SeguidorRepository seguidorRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;
    private final ActividadService actividadService;

    public SeguidorService(SeguidorRepository seguidorRepository,
                           UsuarioRepository usuarioRepository,
                           NotificacionService notificacionService, ActividadService actividadService) {
        this.seguidorRepository = seguidorRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificacionService = notificacionService;
        this.actividadService = actividadService;
    }

    /**
     * Seguir a un usuario
     */
    @Transactional
    public SeguidorDTO seguirUsuario(Long seguidorId, Long seguidoId) {
        // Validar que los IDs son diferentes
        if (seguidorId.equals(seguidoId)) {
            throw new IllegalArgumentException("Un usuario no puede seguirse a sí mismo");
        }

        Usuario seguidor = usuarioRepository.findByIdAndEliminadoFalse(seguidorId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario seguidor", seguidorId));

        Usuario seguido = usuarioRepository.findByIdAndEliminadoFalse(seguidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario a seguir", seguidoId));

        // Verificar si ya existe relación de seguimiento
        if (seguidorRepository.existsBySeguidorAndSeguido(seguidor, seguido)) {
            throw new DuplicateResourceException("Ya estás siguiendo a este usuario");
        }

        // Crear nueva relación de seguimiento
        Seguidor seguimiento = new Seguidor();
        seguimiento.setSeguidor(seguidor);
        seguimiento.setSeguido(seguido);

        Seguidor guardado = seguidorRepository.save(seguimiento);

        // Registrar actividad
        actividadService.registrarActividad(
                seguidorId,
                ActividadTipo.SEGUIR_USUARIO.name(),
                seguidoId,
                "USUARIO",
                null
        );


        // Enviar notificación al usuario seguido
        notificacionService.crearNotificacion(
                seguidoId,                                    // destinatario
                seguidorId,                                   // emisor
                NotificacionTipo.SEGUIDOR_NUEVO,              // tipo
                seguidor.getNombre() + " ha comenzado a seguirte", // mensaje
                seguidorId,                                   // objetoId
                "USUARIO",                                    // objetoTipo
                "/usuarios/" + seguidorId                     // URL
        );

        return convertirADTO(guardado);
    }

    /**
     * Dejar de seguir a un usuario
     */
    @Transactional
    public void dejarDeSeguir(Long seguidorId, Long seguidoId) {
        Usuario seguidor = usuarioRepository.findByIdAndEliminadoFalse(seguidorId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario seguidor", seguidorId));

        Usuario seguido = usuarioRepository.findByIdAndEliminadoFalse(seguidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario seguido", seguidoId));

        Seguidor seguimiento = seguidorRepository.findBySeguidorAndSeguido(seguidor, seguido)
                .orElseThrow(() -> new ResourceNotFoundException("Relación de seguimiento no encontrada"));

        seguidorRepository.delete(seguimiento);
    }

    /**
     * Obtener usuarios a los que sigue un usuario
     */
    public Page<UsuarioResumidoDTO> obtenerSeguidos(Long usuarioId, Pageable pageable, Long usuarioActualId) {
        Usuario usuario = usuarioRepository.findByIdAndEliminadoFalse(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", usuarioId));

        Page<Seguidor> seguidos = seguidorRepository.findBySeguidor(usuario, pageable);

        return seguidos.map(seguidor -> {
            Usuario seguido = seguidor.getSeguido();

            boolean siguiendo = false;
            if (usuarioActualId != null) {
                siguiendo = seguidorRepository.existsBySeguidorAndSeguido(
                        usuarioRepository.getReferenceById(usuarioActualId),
                        seguido
                );
            }

            return new UsuarioResumidoDTO(
                    seguido.getId(),
                    seguido.getNombre(),
                    seguido.getEmail(),
                    siguiendo
            );
        });
    }

    /**
     * Obtener seguidores de un usuario
     */
    public Page<UsuarioResumidoDTO> obtenerSeguidores(Long usuarioId, Pageable pageable, Long usuarioActualId) {
        Usuario usuario = usuarioRepository.findByIdAndEliminadoFalse(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", usuarioId));

        Page<Seguidor> seguidores = seguidorRepository.findBySeguido(usuario, pageable);

        return seguidores.map(seguidor -> {
            Usuario seguidor_usuario = seguidor.getSeguidor();

            boolean siguiendo = false;
            if (usuarioActualId != null) {
                siguiendo = seguidorRepository.existsBySeguidorAndSeguido(
                        usuarioRepository.getReferenceById(usuarioActualId),
                        seguidor_usuario
                );
            }

            return new UsuarioResumidoDTO(
                    seguidor_usuario.getId(),
                    seguidor_usuario.getNombre(),
                    seguidor_usuario.getEmail(),
                    siguiendo
            );
        });
    }

    /**
     * Obtener seguidores mutuos
     */
    public Page<UsuarioResumidoDTO> obtenerSeguidoresMutuos(Long usuarioId, Pageable pageable) {
        Usuario usuario = usuarioRepository.findByIdAndEliminadoFalse(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", usuarioId));

        Page<Usuario> mutuos = seguidorRepository.findMutualFollowers(usuario, pageable);

        return mutuos.map(mutuo -> new UsuarioResumidoDTO(
                mutuo.getId(),
                mutuo.getNombre(),
                mutuo.getEmail(),
                true // Son mutuos, así que por definición hay seguimiento en ambas direcciones
        ));
    }

    /**
     * Verificar si un usuario sigue a otro
     */
    public boolean verificarSeguimiento(Long seguidorId, Long seguidoId) {
        Usuario seguidor = usuarioRepository.findByIdAndEliminadoFalse(seguidorId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario seguidor", seguidorId));

        Usuario seguido = usuarioRepository.findByIdAndEliminadoFalse(seguidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario seguido", seguidoId));

        return seguidorRepository.existsBySeguidorAndSeguido(seguidor, seguido);
    }
    /**
     * Obtener estadísticas de seguidores
     */
    public Map<String, Long> obtenerEstadisticasSeguimiento(Long usuarioId) {
        Usuario usuario = usuarioRepository.findByIdAndEliminadoFalse(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", usuarioId));

        long seguidos = seguidorRepository.countBySeguidor(usuario);
        long seguidores = seguidorRepository.countBySeguido(usuario);

        Map<String, Long> estadisticas = new HashMap<>();
        estadisticas.put("seguidores", seguidores);
        estadisticas.put("seguidos", seguidos);

        return estadisticas;
    }

    /**
     * Convertir entidad a DTO
     */
    private SeguidorDTO convertirADTO(Seguidor seguidor) {
        return new SeguidorDTO(
                seguidor.getId(),
                seguidor.getSeguidor().getId(),
                seguidor.getSeguidor().getNombre(),
                seguidor.getSeguidor().getEmail(),
                seguidor.getSeguido().getId(),
                seguidor.getSeguido().getNombre(),
                seguidor.getSeguido().getEmail(),
                seguidor.getFechaSeguimiento()
        );
    }



}