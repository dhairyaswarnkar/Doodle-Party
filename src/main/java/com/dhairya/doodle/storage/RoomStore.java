package com.dhairya.doodle.storage;

import com.dhairya.doodle.model.Room;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Atomic JSON snapshots for a single Java server; no JDBC or Hibernate. */
@Repository
public class RoomStore {
    private final Path directory;
    private final ObjectMapper mapper;
    public RoomStore(ObjectMapper mapper, @Value("${app.storage-directory:./data/rooms}") String directory) throws IOException {
        this.mapper = mapper;
        this.directory = directory.isBlank() ? null : Path.of(directory);
        if (this.directory != null) Files.createDirectories(this.directory);
    }
    public List<Room> load() throws IOException {
        if (directory == null) return List.of();
        List<Room> rooms = new ArrayList<>();
        try (var files = Files.list(directory)) {
            for (Path path : files.filter(p -> p.getFileName().toString().matches("[A-Z0-9]{6}\\.json")).toList()) {
                Room room = mapper.readValue(Files.readAllBytes(path), Room.class);
                if (!room.code.matches("[A-Z0-9]{6}")) throw new IOException("Invalid stored room code");
                rooms.add(room);
            }
        }
        return rooms;
    }
    public byte[] snapshot(Room room) throws IOException { return mapper.writeValueAsBytes(room); }
    public synchronized void save(String code, byte[] bytes) throws IOException {
        if (directory == null) return;
        Path temporary = Files.createTempFile(directory, code + "-", ".tmp");
        try {
            Files.write(temporary, bytes);
            try { Files.move(temporary, directory.resolve(code + ".json"), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (java.nio.file.AtomicMoveNotSupportedException e) { Files.move(temporary, directory.resolve(code + ".json"), StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
    public synchronized void delete(String code) throws IOException {
        if (directory != null) Files.deleteIfExists(directory.resolve(code + ".json"));
    }
}
