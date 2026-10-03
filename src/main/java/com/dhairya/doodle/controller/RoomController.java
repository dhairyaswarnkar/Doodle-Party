package com.dhairya.doodle.controller;

import com.dhairya.doodle.model.RoomView;
import com.dhairya.doodle.service.GameException;
import com.dhairya.doodle.service.RoomService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RoomController {
    private final RoomService rooms;
    public RoomController(RoomService rooms) { this.rooms = rooms; }
    @GetMapping("/health")
    public Map<String,Object> health() { return Map.of("ok", true, "backend", "Java Spring Boot", "transport", "WebSocket"); }
    @GetMapping("/csrf")
    public Map<String,String> csrf(CsrfToken token) { return Map.of("headerName", token.getHeaderName(), "token", token.getToken()); }
    @PostMapping("/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomView create(@RequestBody JsonNode body, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return rooms.create(body.path("name").asText(null));
    }
    @GetMapping("/rooms/{code}")
    public RoomView view(@PathVariable String code, @RequestHeader(value="X-Player-Token",defaultValue="") String token,
                         @RequestParam(value="cv",required=false) Long cv, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return rooms.view(code, token, cv);
    }
    @PostMapping("/rooms/{code}/{action}")
    public RoomView action(@PathVariable String code, @PathVariable String action, @RequestBody JsonNode body,
                           @RequestHeader(value="X-Player-Token",defaultValue="") String token,
                           @RequestParam(value="cv",required=false) Long cv, HttpServletResponse response) {
        if (!List.of("join", "start", "settings", "choose", "stroke", "undo", "clear", "guess", "leave").contains(action)) throw new GameException("Unknown action.", 404);
        response.setHeader("Cache-Control", "no-store");
        return rooms.action(code, action, body, token, cv);
    }
}
