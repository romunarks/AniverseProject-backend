package com.aniverse.backend.repository;

import com.aniverse.backend.model.Notificacion;
import com.aniverse.backend.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    // Encontrar notificaciones de un usuario ordenadas por fecha descendente (más recientes primero)
    Page<Notificacion> findByDestinatarioOrderByFechaDesc(Usuario destinatario, Pageable pageable);

    // Encontrar notificaciones no leídas de un usuario
    Page<Notificacion> findByDestinatarioAndLeidaFalseOrderByFechaDesc(Usuario destinatario, Pageable pageable);

    // Contar notificaciones no leídas de un usuario
    long countByDestinatarioAndLeidaFalse(Usuario destinatario);

    // Marcar todas las notificaciones de un usuario como leídas
    @Query("UPDATE Notificacion n SET n.leida = true WHERE n.destinatario = :destinatario AND n.leida = false")
    void markAllAsRead(@Param("destinatario") Usuario destinatario);

    // Eliminar notificaciones más antiguas que cierta fecha
    @Modifying
    @Query("DELETE FROM Notificacion n WHERE n.fecha < :fecha AND n.leida = true")
    void deleteOldReadNotifications(@Param("fecha") LocalDateTime fecha);
}