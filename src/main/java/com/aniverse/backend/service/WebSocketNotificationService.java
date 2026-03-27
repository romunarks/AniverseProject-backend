package com.aniverse.backend.service;

import com.aniverse.backend.dto.ActividadDTO;
import com.aniverse.backend.dto.NotificacionDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class WebSocketNotificationService {

    private static final Logger log = LoggerFactory.getLogger(WebSocketNotificationService.class);

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendNotificationToUser(Long userId, NotificacionDTO notification) {
        log.info("Enviando notificación a usuario: {}", userId);
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                notification
        );
    }

    public void broadcastActivity(ActividadDTO activity) {
        log.info("Broadcasting actividad: {}", activity.getTipo());
        messagingTemplate.convertAndSend(
                "/topic/activities",
                activity
        );
    }

    public void sendToTopic(String topic, Object message) {
        log.info("Enviando mensaje a topic: {}", topic);
        messagingTemplate.convertAndSend("/topic/" + topic, message);
    }

    public void sendToQueue(String queue, Object message) {
        log.info("Enviando mensaje a queue: {}", queue);
        messagingTemplate.convertAndSend("/queue/" + queue, message);
    }
}