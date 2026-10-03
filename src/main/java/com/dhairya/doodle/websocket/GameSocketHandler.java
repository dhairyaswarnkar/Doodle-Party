package com.dhairya.doodle.websocket;

import com.dhairya.doodle.model.RoomView;
import com.dhairya.doodle.service.GameException;
import com.dhairya.doodle.service.RoomService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameSocketHandler extends TextWebSocketHandler {
    private final RoomService rooms;
    private final ObjectMapper mapper;
    private final ConcurrentHashMap<String, Subscription> subscriptions = new ConcurrentHashMap<>();
    private static class Subscription {
        volatile String code, token;
        long canvasRevision = -1;
        final WebSocketSession session;
        Subscription(WebSocketSession session) { this.session = new ConcurrentWebSocketSessionDecorator(session, 5000, 8 * 1024 * 1024); }
    }
    public GameSocketHandler(RoomService rooms, ObjectMapper mapper) { this.rooms = rooms; this.mapper = mapper; }
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        session.setTextMessageSizeLimit(16000);
        subscriptions.put(session.getId(), new Subscription(session));
    }
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        Subscription s = subscriptions.get(session.getId());
        if (s == null) return;
        try {
            JsonNode body = mapper.readTree(message.getPayload());
            switch (body.path("type").asText("")) {
                case "subscribe" -> {
                    String code = body.path("code").asText(""), token = body.path("token").asText("");
                    rooms.view(code, token, null); // Validate before attaching to a room.
                    synchronized (s) { s.code = code.toUpperCase(java.util.Locale.ROOT); s.token = token; s.canvasRevision = -1; }
                    rooms.heartbeat(s.code, s.token);
                    sendState(s);
                }
                case "ping" -> {
                    if (s.code == null) throw new GameException("Subscribe to a room first.", 401);
                    rooms.heartbeat(s.code, s.token);
                    send(s, Map.of("type", "pong", "serverNow", System.currentTimeMillis()));
                }
                default -> throw new GameException("Unknown WebSocket message.");
            }
        } catch (GameException e) { send(s, Map.of("type", "error", "error", e.getMessage(), "status", e.status())); }
        catch (IOException e) { send(s, Map.of("type", "error", "error", "Invalid WebSocket JSON.")); }
    }
    private void send(Subscription s, Object message) throws IOException {
        if (s.session.isOpen()) s.session.sendMessage(new TextMessage(mapper.writeValueAsString(message)));
    }
    private void sendState(Subscription s) throws IOException {
        // Prevent a newer revision being sent ahead of an older snapshot for this subscriber.
        synchronized (s) {
            if (s.code == null) return;
            RoomView view = rooms.view(s.code, s.token, s.canvasRevision);
            send(s, Map.of("type", "state", "state", view));
            s.canvasRevision = view.canvasRevision();
        }
    }
    @EventListener
    public void changed(RoomService.RoomChanged event) {
        for (Subscription s : subscriptions.values()) {
            if (event.code().equals(s.code)) try { sendState(s); }
            catch (IOException | GameException e) {
                try { s.session.close(CloseStatus.SERVER_ERROR); } catch (IOException ignored) {}
            }
        }
    }
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) { subscriptions.remove(session.getId()); }
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws IOException {
        subscriptions.remove(session.getId()); if (session.isOpen()) session.close(CloseStatus.SERVER_ERROR);
    }
}
