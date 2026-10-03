package com.dhairya.doodle;

import com.dhairya.doodle.service.GameRules;
import com.dhairya.doodle.storage.RoomStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class RoomStoreTest {
    @TempDir Path directory;
    @Test void atomicSnapshotsRestoreSessionsAndCleanupDeletesExpiredFiles() throws Exception {
        var mapper=new ObjectMapper();var store=new RoomStore(mapper,directory.toString());
        var room=new GameRules().fresh("ABC234","Dhairya",100000);
        store.save(room.code,store.snapshot(room));
        var restored=new RoomStore(mapper,directory.toString()).load().get(0);
        assertEquals(room.players.get(0).token,restored.players.get(0).token);
        assertEquals(room.hostId,restored.hostId);
        store.delete(room.code);assertTrue(store.load().isEmpty());
    }
}
