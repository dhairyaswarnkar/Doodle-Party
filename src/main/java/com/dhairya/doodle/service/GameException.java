package com.dhairya.doodle.service;

public class GameException extends RuntimeException {
    private final int status;
    public GameException(String message) { this(message, 400); }
    public GameException(String message, int status) { super(message); this.status = status; }
    public int status() { return status; }
}
