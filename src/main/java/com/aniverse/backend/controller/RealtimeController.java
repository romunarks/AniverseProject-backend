package com.aniverse.backend.controller;

import com.aniverse.backend.dto.NotificacionDTO;
import com.aniverse.backend.dto.response.AniverseResponse;
import com.aniverse.backend.service.WebSocketNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/realtime")
@Tag(name = "Real Time", description = "API para testing de notificaciones en tiempo real")
public class RealtimeController {

    private final WebSocketNotificationService webSocketNotificationService;

    public RealtimeController(WebSocketNotificationService webSocketNotificationService) {
        this.webSocketNotificationService = webSocketNotificationService;
    }

    @PostMapping("/test-notification/{userId}")
    @Operation(summary = "Enviar notificación de prueba a un usuario")
    public ResponseEntity<AniverseResponse<String>> testNotification(
            @PathVariable Long userId,
            @RequestParam String message) {

        NotificacionDTO testNotification = new NotificacionDTO();
        testNotification.setId(999L);
        testNotification.setDestinatarioId(userId);
        testNotification.setMensaje(message);
        testNotification.setTipo("TEST");
        testNotification.setLeida(false);

        webSocketNotificationService.sendNotificationToUser(userId, testNotification);

        return ResponseEntity.ok(
                AniverseResponse.success("Notificación de prueba enviada al usuario " + userId));
    }

    @PostMapping("/test-broadcast")
    @Operation(summary = "Enviar broadcast de prueba")
    public ResponseEntity<AniverseResponse<String>> testBroadcast(@RequestParam String message) {
        webSocketNotificationService.sendToTopic("announcements", message);

        return ResponseEntity.ok(
                AniverseResponse.success("Broadcast enviado correctamente"));
    }
}