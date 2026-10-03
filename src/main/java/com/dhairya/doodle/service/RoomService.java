package com.dhairya.doodle.service;

import com.dhairya.doodle.model.Room;
import com.dhairya.doodle.model.RoomView;
import com.dhairya.doodle.storage.RoomStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Each room is its own lock; unrelated rooms can run concurrently. */
@Service
public class RoomService {
    public record RoomChanged(String code) {}
    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();
    private final Set<String> dirty = ConcurrentHashMap.newKeySet();
    private final GameRules rules;
    private final RoomStore store;
    private final ApplicationEventPublisher events;
    private final Logger log = LoggerFactory.getLogger(getClass());
    public RoomService(GameRules rules, RoomStore store, ApplicationEventPublisher events) throws IOException {
        this.rules = rules; this.store = store; this.events = events;
        for (Room r : store.load()) rooms.put(r.code, r);
    }
    private Room room(String code) {
        Room r = rooms.get(rules.normalizeCode(code));
        if (r == null) throw new GameException("Room not found. Check the code or create a new room.", 404);
        return r;
    }
    public RoomView create(String name) {
        if (rooms.size() >= 1024) throw new GameException("Room capacity reached. Please try later.", 503);
        Room r;
        do { r = rules.fresh(rules.newCode(), name, System.currentTimeMillis()); }
        while (rooms.putIfAbsent(r.code, r) != null);
        dirty.add(r.code);
        return rules.view(r, r.players.get(0).token, System.currentTimeMillis(), null, true);
    }
    public RoomView action(String code, String action, JsonNode body, String token, Long canvasRevision) {
        Room r = room(code); RoomView result;
        synchronized (r) {
            long now = System.currentTimeMillis();
            if (now - r.updatedAt > 86400000L) throw new GameException("This room has expired. Create a fresh room.", 410);
            Room.Player p = rules.act(r, action, body, token, now);
            result = rules.view(r, p.token, now, canvasRevision, action.equals("join"));
            dirty.add(r.code);
        }
        events.publishEvent(new RoomChanged(r.code));
        return result;
    }
    public RoomView view(String code, String token, Long canvasRevision) {
        Room r = room(code);
        synchronized (r) {
            if (System.currentTimeMillis() - r.updatedAt > 86400000L) throw new GameException("This room has expired.", 410);
            return rules.view(r, token, System.currentTimeMillis(), canvasRevision, false);
        }
    }
    public void heartbeat(String code, String token) {
        action(code, "heartbeat", JsonNodeFactory.instance.objectNode(), token, null);
    }
    @Scheduled(fixedDelay = 250)
    public void tick() {
        long now = System.currentTimeMillis();
        for (Room r : rooms.values()) {
            boolean changed;
            synchronized (r) {
                changed = rules.advance(r, now);
                if (changed) { r.revision++; dirty.add(r.code); }
            }
            if (changed) events.publishEvent(new RoomChanged(r.code));
        }
    }
    @Scheduled(fixedDelay = 1000)
    public void flush() {
        for (String code : Set.copyOf(dirty)) {
            if (!dirty.remove(code)) continue;
            Room r = rooms.get(code); if (r == null) continue;
            try {
                byte[] bytes; synchronized (r) { bytes = store.snapshot(r); }
                store.save(code, bytes);
            } catch (IOException e) { dirty.add(code); log.error("Room snapshot could not be saved for {}", code); }
        }
    }
    @Scheduled(fixedDelay = 60000)
    public void cleanup() {
        long now = System.currentTimeMillis();
        for (Room r : rooms.values()) synchronized (r) {
            if (now - r.updatedAt > 86400000L && rooms.remove(r.code, r)) {
                dirty.remove(r.code);
                try { store.delete(r.code); } catch (IOException e) { log.error("Expired room snapshot cleanup failed for {}", r.code); }
            }
        }
    }
    @PreDestroy
    public void close() { flush(); }
}
