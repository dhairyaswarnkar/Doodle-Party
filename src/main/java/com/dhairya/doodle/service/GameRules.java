package com.dhairya.doodle.service;

import com.dhairya.doodle.model.Room;
import com.dhairya.doodle.model.Room.Player;
import com.dhairya.doodle.model.RoomView;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** All authoritative rules are Java. No database, HTTP, or WebSocket dependencies here. */
@Component
public class GameRules {
    private static final List<String> WORDS = List.of("bicycle", "rainbow", "elephant", "pizza", "guitar",
            "rocket", "butterfly", "snowman", "umbrella", "cactus", "penguin", "watermelon", "castle",
            "toothbrush", "airplane", "sunflower", "jellyfish", "robot", "lighthouse", "popcorn", "turtle",
            "volcano", "octopus", "camera", "banana", "basketball", "helicopter", "dragon", "pancakes",
            "dinosaur", "sandcastle", "fireworks", "backpack", "mushroom", "skateboard", "sunglasses",
            "treasure", "coffee", "ice cream", "hot air balloon", "cricket", "train", "tiger", "mountain",
            "panda", "campfire", "hamburger", "key", "moon", "apple");
    private final SecureRandom random = new SecureRandom();
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    public String newCode() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 6; i++) b.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        return b.toString();
    }
    public String normalizeCode(String code) {
        String result = code == null ? "" : code.trim().toUpperCase(java.util.Locale.ROOT).replaceAll("[\\s-]", "");
        if (!result.matches("[A-Z0-9]{6}")) throw new GameException("Enter a six-character room code.");
        return result;
    }
    private String nickname(String name) {
        String result = name == null ? "" : name.trim().replaceAll("[\\p{Cntrl}]", "");
        if (result.isEmpty() || result.length() > 20) throw new GameException("Choose a nickname with 1–20 characters.");
        return result;
    }
    public Room fresh(String code, String name, long now) {
        Room r = new Room(); r.code = code; r.createdAt = r.updatedAt = now;
        Player p = new Player(nickname(name), now); r.players.add(p); r.hostId = p.id;
        message(r, p.name + " opened the room.", "system", null, now);
        return r;
    }
    public Player player(Room r, String token) {
        return r.players.stream().filter(p -> Objects.equals(p.token, token)).findFirst()
                .orElseThrow(() -> new GameException("Please join this room first.", 401));
    }
    private List<Player> active(Room r) { return r.players.stream().filter(p -> !p.left).toList(); }
    private boolean playing(Room r) { return r.phase.equals("choosing") || r.phase.equals("drawing"); }
    private void host(Room r, Player p) {
        if (!p.id.equals(r.hostId)) throw new GameException("Only the room host can do that.", 403);
    }
    private void message(Room r, String text, String type, Player p, long now) {
        r.messages.add(new Room.Message(UUID.randomUUID().toString(), text, type,
                p == null ? "" : p.name, p == null ? null : p.id, now));
        while (r.messages.size() > 80) r.messages.remove(0);
    }
    public Player act(Room r, String action, JsonNode body, String token, long now) {
        Player p;
        if (action.equals("join")) {
            p = r.players.stream().filter(x -> Objects.equals(x.token, token)).findFirst().orElse(null);
            if (p == null) {
                advance(r, now);
                if (active(r).size() >= 8) throw new GameException("This room is full (8 players).");
                if (r.players.size() >= 20) throw new GameException("Please create a fresh room.");
                p = new Player(nickname(body.path("name").asText(null)), now); r.players.add(p);
                message(r, p.name + " joined the room.", "system", null, now);
            }
        } else p = player(r, token);
        if (!action.equals("leave")) { p.left = false; p.lastSeen = now; }
        advance(r, now);

        switch (action) {
            case "join", "heartbeat" -> { }
            case "settings" -> {
                host(r, p);
                if (!List.of("lobby", "finished").contains(r.phase)) throw new GameException("Change settings between games.");
                int laps = body.path("laps").asInt(-1), duration = body.path("duration").asInt(-1);
                if (!List.of(1, 2, 3).contains(laps) || !List.of(30, 60, 90).contains(duration)) throw new GameException("Invalid room settings.");
                r.laps = laps; r.duration = duration;
            }
            case "start" -> {
                host(r, p);
                if (!List.of("lobby", "finished").contains(r.phase)) throw new GameException("A game is already running.");
                if (active(r).size() < 2) throw new GameException("Invite at least one more player to start.");
                r.players.forEach(x -> { x.score = 0; x.guessed = false; });
                r.order = new ArrayList<>(active(r).stream().map(x -> x.id).toList());
                r.totalRounds = r.order.size() * r.laps; r.round = 0; nextRound(r, now);
            }
            case "choose" -> {
                if (!r.phase.equals("choosing") || !p.id.equals(r.drawerId)) throw new GameException("Only the current artist can choose a word.", 403);
                String word = body.path("word").asText("");
                if (!r.choices.contains(word)) throw new GameException("Choose one of the offered words.");
                r.word = word; r.choices.clear(); r.phase = "drawing"; r.deadline = now + r.duration * 1000L;
                message(r, "The drawing round has started.", "system", null, now);
            }
            case "stroke", "undo", "clear" -> {
                if (!r.phase.equals("drawing") || !p.id.equals(r.drawerId)) throw new GameException("Only the current artist can draw.", 403);
                if (body.path("round").asInt(-1) != r.round) throw new GameException("This drawing belongs to an earlier round.", 409);
                if (action.equals("clear")) r.strokes.clear();
                else if (action.equals("undo")) {
                    if (!r.strokes.isEmpty()) {
                        String gesture = r.strokes.get(r.strokes.size() - 1).gestureId();
                        while (!r.strokes.isEmpty() && r.strokes.get(r.strokes.size() - 1).gestureId().equals(gesture)) r.strokes.remove(r.strokes.size() - 1);
                    }
                } else {
                    if (r.strokes.size() >= 1800) throw new GameException("Canvas is full. Clear it to keep drawing.");
                    Room.Stroke stroke = stroke(body.path("stroke"));
                    // An acknowledged retry with the same ID is safe: don't duplicate geometry.
                    if (r.strokes.stream().noneMatch(s -> s.id().equals(stroke.id()))) r.strokes.add(stroke);
                }
                r.canvasRevision++;
            }
            case "guess" -> {
                String text = body.path("text").asText("").trim();
                if (text.isEmpty() || text.length() > 100) throw new GameException("Keep guesses between 1 and 100 characters.");
                if (p.lastGuess != 0 && now - p.lastGuess < 450) throw new GameException("A little too fast — try again.");
                if (r.phase.equals("choosing")) throw new GameException("Wait for the artist to choose a word.");
                if (r.phase.equals("drawing")) {
                    if (p.id.equals(r.drawerId)) throw new GameException("Artists draw; everyone else guesses.");
                    if (p.guessed) throw new GameException("You already got it! Keep the answer secret.");
                    if (normalized(text).equals(normalized(r.word))) {
                        p.guessed = true; int points = 100 + (int)Math.round(300.0 * Math.max(0, r.deadline - now) / (r.duration * 1000.0));
                        p.score += points; r.players.stream().filter(x -> x.id.equals(r.drawerId)).findFirst().ifPresent(x -> x.score += 75);
                        message(r, p.name + " guessed the word! +" + points, "correct", p, now);
                        if (active(r).stream().filter(x -> !x.id.equals(r.drawerId)).allMatch(x -> x.guessed)) endRound(r, now, "Everyone guessed it!");
                    } else message(r, text, "guess", p, now);
                } else message(r, text, "chat", p, now);
                p.lastGuess = now;
            }
            case "leave" -> { p.left = true; message(r, p.name + " left the room.", "system", null, now); advance(r, now); }
            default -> throw new GameException("Unknown room action.", 404);
        }
        r.updatedAt = now; r.revision++;
        return p;
    }
    private String normalized(String text) { return text.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", ""); }
    private Room.Stroke stroke(JsonNode s) {
        String color = s.path("color").asText(""); int width = s.path("width").asInt(-1);
        JsonNode raw = s.path("points");
        if (!color.matches("#[0-9a-fA-F]{6}") || !List.of(3, 7, 14, 24).contains(width) || !raw.isArray() || raw.isEmpty() || raw.size() > 100) throw new GameException("Invalid brush stroke.");
        List<List<Double>> points = new ArrayList<>();
        for (JsonNode pt : raw) {
            if (!pt.isArray() || pt.size() != 2 || !pt.get(0).isNumber() || !pt.get(1).isNumber()) throw new GameException("Invalid canvas coordinates.");
            double x = pt.get(0).asDouble(), y = pt.get(1).asDouble();
            if (!Double.isFinite(x) || !Double.isFinite(y) || x < 0 || x > 1 || y < 0 || y > 1) throw new GameException("Invalid canvas coordinates.");
            points.add(List.of(Math.round(x * 10000) / 10000.0, Math.round(y * 10000) / 10000.0));
        }
        String id = s.path("id").asText(UUID.randomUUID().toString());
        String gesture = s.path("gestureId").asText(id);
        if (id.isBlank() || id.length() > 64 || gesture.isBlank() || gesture.length() > 64) throw new GameException("Invalid stroke identifier.");
        return new Room.Stroke(id, gesture, color, width, List.copyOf(points));
    }
    public boolean advance(Room r, long now) {
        boolean changed = false;
        for (Player p : r.players) if (!p.left && now - p.lastSeen > 90000) {
            p.left = true; message(r, p.name + " disconnected.", "system", null, now); changed = true;
        }
        List<Player> active = active(r);
        if (active.stream().noneMatch(p -> p.id.equals(r.hostId)) && !active.isEmpty()) { r.hostId = active.get(0).id; changed = true; }
        if (playing(r) && active.stream().noneMatch(p -> p.id.equals(r.drawerId))) { endRound(r, now, "The artist left."); changed = true; }
        if (playing(r) && active.size() < 2) {
            r.phase = "lobby"; r.deadline = 0; r.word = ""; r.choices.clear();
            message(r, "Game paused. At least two players are needed.", "system", null, now); changed = true;
        }
        if (r.phase.equals("choosing") && now >= r.deadline) {
            r.word = r.choices.get(0); r.choices.clear(); r.phase = "drawing"; r.deadline = now + r.duration * 1000L;
            message(r, "Time to draw!", "system", null, now); changed = true;
        }
        if (r.phase.equals("drawing") && now >= r.deadline) { endRound(r, now, "Time's up!"); changed = true; }
        if (r.phase.equals("reveal") && now >= r.deadline) { nextRound(r, now); changed = true; }
        return changed;
    }
    private void nextRound(Room r, long now) {
        r.round++;
        while (r.round <= r.totalRounds && active(r).stream().noneMatch(p -> p.id.equals(r.order.get((r.round - 1) % r.order.size())))) r.round++;
        if (r.round > r.totalRounds) { r.phase = "finished"; r.deadline = 0; message(r, "Game over! Your final scores are ready.", "system", null, now); return; }
        r.drawerId = r.order.get((r.round - 1) % r.order.size()); r.phase = "choosing"; r.deadline = now + 15000;
        List<String> words = new ArrayList<>(WORDS); java.util.Collections.shuffle(words, random);
        r.choices = new ArrayList<>(words.subList(0, 3)); r.word = ""; r.strokes.clear(); r.canvasRevision++; r.lastRound = null;
        r.players.forEach(p -> p.guessed = false);
        String name = r.players.stream().filter(p -> p.id.equals(r.drawerId)).findFirst().orElseThrow().name;
        message(r, name + " is choosing a word.", "system", null, now);
    }
    private void endRound(Room r, long now, String reason) {
        if (!playing(r)) return;
        if (r.word.isEmpty() && !r.choices.isEmpty()) r.word = r.choices.get(0);
        r.phase = "reveal"; r.deadline = now + 6000; r.lastRound = new Room.RoundResult(r.word, reason); r.choices.clear();
        message(r, reason + " The word was “" + r.word + "”.", "reveal", null, now);
    }
    public RoomView view(Room r, String token, long now, Long canvasRevision, boolean includeToken) {
        Player p = player(r, token);
        boolean reveal = List.of("reveal", "finished").contains(r.phase) || p.id.equals(r.drawerId) || p.guessed;
        int budget = (r.deadline - now) < r.duration * 250L ? 2 : (r.deadline - now) < r.duration * 500L ? 1 : 0;
        StringBuilder hint = new StringBuilder();
        for (int i = 0; i < r.word.length(); i++) {
            char ch = r.word.charAt(i);
            if (ch == ' ') hint.append(' ');
            else if (budget > 0 && i > 0 && i % 3 == 1) { hint.append(ch); budget--; }
            else hint.append('_');
        }
        return new RoomView(r.code, r.phase, r.hostId, r.drawerId, r.round, r.totalRounds, r.laps, r.duration,
                r.deadline, now, r.revision, reveal ? r.word : null, r.phase.equals("drawing") ? hint.toString() : "",
                p.id.equals(r.drawerId) && r.phase.equals("choosing") ? List.copyOf(r.choices) : List.of(),
                active(r).stream().map(x -> new RoomView.PlayerView(x.id, x.name, x.score, x.guessed, now - x.lastSeen < 15000)).toList(),
                new RoomView.Me(p.id, p.name, p.guessed), r.canvasRevision,
                canvasRevision != null && canvasRevision == r.canvasRevision ? null : List.copyOf(r.strokes),
                List.copyOf(r.messages), r.lastRound, includeToken ? p.token : null);
    }
}
