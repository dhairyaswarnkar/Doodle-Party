package com.dhairya.doodle.model;

import java.util.List;

/** Role-filtered response: no stored player tokens or lastSeen timestamps. */
public record RoomView(
        String code, String phase, String hostId, String drawerId,
        int round, int totalRounds, int laps, int duration,
        long deadline, long serverNow, long revision,
        String word, String hint, List<String> choices,
        List<PlayerView> players, Me me,
        long canvasRevision, List<Room.Stroke> strokes,
        List<Room.Message> messages, Room.RoundResult lastRound,
        String token) {
    public record PlayerView(String id, String name, int score, boolean guessed, boolean online) {}
    public record Me(String id, String name, boolean guessed) {}
}
