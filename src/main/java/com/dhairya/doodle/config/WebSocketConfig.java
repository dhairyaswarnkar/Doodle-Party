package com.dhairya.doodle.config;

import com.dhairya.doodle.websocket.GameSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final GameSocketHandler handler;
    public WebSocketConfig(GameSocketHandler handler) { this.handler = handler; }
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Default same-origin restriction. No wildcard origin configuration.
        registry.addHandler(handler, "/ws/game");
    }
}
