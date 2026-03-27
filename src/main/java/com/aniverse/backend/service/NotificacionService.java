package com.aniverse.backend.service;

import com.aniverse.backend.dto.NotificacionDTO;
import com.aniverse.backend.model.Notificacion;
import com.aniverse.backend.model.Usuario;
import com.aniverse.backend.repository.NotificacionRepository;
import com.aniverse.backend.repository.UsuarioRepository;
import com.aniverse.backend.exception.ResourceNotFoundException;
import com.aniverse.backend.util.NotificacionTipo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final WebSocketNotificationService webSocketNotificationService;

    @Value("${notificacion.duracion.dias:30}")
    private int diasRetencionNotificaciones;

    public NotificacionService(NotificacionRepository notificacionRepository,
                               UsuarioRepository usuarioRepository,
                               WebSocketNotificationService webSocketNotificationService) {
        this.notificacionRepository = notificacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.webSocketNotificationService = webSocketNotificationService;
    }

    /**
     * Crea una nueva notificación
     */
    @Transactional
    public NotificacionDTO crearNotificacion(Long destinatarioId, Long emisorId,
                                             String tipo, String mensaje,
                                             Long objetoId, String objetoTipo,
                                             String url) {

        Usuario destinatario = usuarioRepository.findById(destinatarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario destinatario", destinatarioId));

        // El emisor puede ser opcional
        Usuario emisor = null;
        if (emisorId != null) {
            emisor = usuarioRepository.findById(emisorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario emisor", emisorId));
        }

        // No enviar notificaciones a uno mismo
        if (emisor != null && emisor.getId().equals(destinatario.getId())) {
            return null;
        }

        Notificacion notificacion = new Notificacion();
        notificacion.setDestinatario(destinatario);
        notificacion.setEmisor(emisor);
        notificacion.setTipo(tipo);
        notificacion.setMensaje(mensaje);
        notificacion.setLeida(false);
        notificacion.setFecha(LocalDateTime.now());
        notificacion.setObjetoId(objetoId);
        notificacion.setObjetoTipo(objetoTipo);
        notificacion.setUrl(url);

        Notificacion guardada = notificacionRepository.save(notificacion);
        NotificacionDTO notificacionDTO = convertToDTO(guardada);

        // Enviar notificación en tiempo real
        webSocketNotificationService.sendNotificationToUser(destinatarioId, notificacionDTO);

        return convertToDTO(guardada);
    }

    /**
     * Obtener notificaciones de un usuario
     */
    public Page<NotificacionDTO> getNotificacionesUsuario(Long usuarioId, Pageable pageable) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", usuarioId));

        Page<Notificacion> notificaciones = notificacionRepository.findByDestinatarioOrderByFechaDesc(usuario, pageable);

        return notificaciones.map(this::convertToDTO);
    }

    /**
     * Obtener notificaciones no leídas de un usuario
     */
    public Page<NotificacionDTO> getNotificacionesNoLeidas(Long usuarioId, Pageable pageable) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", usuarioId));

        Page<Notificacion> notificaciones = notificacionRepository.findByDestinatarioAndLeidaFalseOrderByFechaDesc(usuario, pageable);

        return notificaciones.map(this::convertToDTO);
    }

    /**
     * Contar notificaciones no leídas
     */
    public long contarNotificacionesNoLeidas(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", usuarioId));

        return notificacionRepository.countByDestinatarioAndLeidaFalse(usuario);
    }

    /**
     * Marcar una notificación como leída
     */
    @Transactional
    public NotificacionDTO marcarComoLeida(Long notificacionId) {
        Notificacion notificacion = notificacionRepository.findById(notificacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación", notificacionId));

        notificacion.setLeida(true);
        Notificacion actualizada = notificacionRepository.save(notificacion);

        return convertToDTO(actualizada);
    }

    /**
     * Marcar todas las notificaciones de un usuario como leídas
     */
    @Transactional
    public void marcarTodasComoLeidas(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", usuarioId));

        notificacionRepository.markAllAsRead(usuario);
    }

    /**
     * Eliminar notificaciones antiguas (tarea programada)
     */
    @Scheduled(cron = "0 0 1 * * ?") // Ejecutar diariamente a la 1 AM
    @Transactional
    public void limpiarNotificacionesAntiguas() {
        LocalDateTime fechaLimite = LocalDateTime.now().minusDays(diasRetencionNotificaciones);
        notificacionRepository.deleteOldReadNotifications(fechaLimite);
    }

    /**
     * Eliminar una notificación
     */
    @Transactional
    public void eliminarNotificacion(Long notificacionId) {
        if (!notificacionRepository.existsById(notificacionId)) {
            throw new ResourceNotFoundException("Notificación", notificacionId);
        }
        notificacionRepository.deleteById(notificacionId);
    }
    /**
     * Convertir entidad a DTO
     */
    private NotificacionDTO convertToDTO(Notificacion notificacion) {
        NotificacionDTO dto = new NotificacionDTO();
        dto.setId(notificacion.getId());
        dto.setDestinatarioId(notificacion.getDestinatario().getId());
        dto.setDestinatarioNombre(notificacion.getDestinatario().getNombre());

        if (notificacion.getEmisor() != null) {
            dto.setEmisorId(notificacion.getEmisor().getId());
            dto.setEmisorNombre(notificacion.getEmisor().getNombre());
        }

        dto.setTipo(notificacion.getTipo());
        dto.setMensaje(notificacion.getMensaje());
        dto.setLeida(notificacion.isLeida());
        dto.setFecha(notificacion.getFecha());
        dto.setObjetoId(notificacion.getObjetoId());
        dto.setObjetoTipo(notificacion.getObjetoTipo());
        dto.setUrl(notificacion.getUrl());

        return dto;
    }
    // En NotificacionService.java, añadir un método para generar URLs apropiadas y mensajes personalizados

    private String generarUrlNotificacion(String tipo, Long objetoId, Long animeId) {
        switch (tipo) {
            case NotificacionTipo.COMENTARIO:
            case NotificacionTipo.RESPUESTA_COMENTARIO:
                return "/anime/" + animeId + "/resenas/" + objetoId;
            case NotificacionTipo.RESENYA:
                return "/anime/" + animeId + "/resenas/" + objetoId;
            case NotificacionTipo.VALORACION:
                return "/anime/" + animeId;
            case NotificacionTipo.SEGUIDOR_NUEVO:
                return "/usuarios/" + objetoId;
            default:
                return "/";
        }
    }

    private String generarMensajeNotificacion(String tipo, String nombreEmisor, String nombreObjeto) {
        switch (tipo) {
            case NotificacionTipo.COMENTARIO:
                return nombreEmisor + " ha comentado en tu reseña de " + nombreObjeto;
            case NotificacionTipo.RESPUESTA_COMENTARIO:
                return nombreEmisor + " ha respondido a tu comentario en la reseña de " + nombreObjeto;
            case NotificacionTipo.RESENYA:
                return nombreEmisor + " ha publicado una reseña de " + nombreObjeto;
            case NotificacionTipo.VALORACION:
                return nombreEmisor + " ha valorado " + nombreObjeto;
            case NotificacionTipo.SEGUIDOR_NUEVO:
                return nombreEmisor + " ha comenzado a seguirte";
            default:
                return "Tienes una nueva notificación";
        }
    }

}