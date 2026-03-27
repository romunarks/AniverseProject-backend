package com.aniverse.backend.controller;

import com.aniverse.backend.model.ChatMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import java.security.Principal;

@Controller
@RequestMapping("/websocket")
public class WebSocketController {

    @MessageMapping("/connect")
    @SendTo("/topic/connected")
    public String handleConnection(Principal principal) {
        return "Usuario conectado: " + principal.getName();
    }

    @MessageMapping("/chat.send")
    @SendTo("/topic/chat")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
        return chatMessage;
    }
}