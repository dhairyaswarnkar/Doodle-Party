package com.dhairya.doodle.controller;

import com.dhairya.doodle.service.GameException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(GameException.class)
    public ResponseEntity<Map<String,String>> game(GameException e) { return ResponseEntity.status(e.status()).body(Map.of("error", e.getMessage())); }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String,String>> malformed() { return ResponseEntity.badRequest().body(Map.of("error", "Send a valid JSON request.")); }
}
