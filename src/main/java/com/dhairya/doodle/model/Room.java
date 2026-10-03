package com.dhairya.doodle.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Server-only state. Controllers return RoomView instead of serializing this model. */
public class Room {
    public String code, hostId, drawerId, word = "", phase = "lobby";
    public long createdAt, updatedAt, deadline, revision, canvasRevision;
    public int laps = 2, duration = 60, round, totalRounds;
    public List<Player> players = new ArrayList<>();
    public List<String> order = new ArrayList<>(), choices = new ArrayList<>();
    public List<Stroke> strokes = new ArrayList<>();
    public List<Message> messages = new ArrayList<>();
    public RoundResult lastRound;

    public static class Player {
        public String id, token, name;
        public int score;
        public boolean guessed, left;
        public long lastSeen, lastGuess;
        public Player() {}
        public Player(String name, long now) {
            this.id = UUID.randomUUID().toString();
            this.token = UUID.randomUUID().toString();
            this.name = name;
            this.lastSeen = now;
        }
    }

    public record Stroke(String id, String gestureId, String color, int width, List<List<Double>> points) {}
    public record Message(String id, String text, String type, String name, String playerId, long at) {}
    public record RoundResult(String word, String reason) {}
}
