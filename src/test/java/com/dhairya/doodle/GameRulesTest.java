package com.dhairya.doodle;

import com.dhairya.doodle.model.Room;
import com.dhairya.doodle.service.GameException;
import com.dhairya.doodle.service.GameRules;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GameRulesTest {
    private final GameRules rules = new GameRules();
    private final ObjectMapper mapper = new ObjectMapper();
    private com.fasterxml.jackson.databind.JsonNode body(String json) throws Exception { return mapper.readTree(json); }
    @Test void completeGameIsAuthoritativeAndReplayable() throws Exception {
        long now = 100000; Room r = rules.fresh("ABC234", "Dhairya", now); String host = r.players.get(0).token;
        assertThrows(GameException.class, () -> rules.act(r, "start", mapper.createObjectNode(), host, now));
        String guest = rules.act(r, "join", body("{\"name\":\"Friend\"}"), "", now).token;
        assertThrows(GameException.class, () -> rules.act(r, "settings", body("{\"laps\":1,\"duration\":30}"), guest, now));
        rules.act(r, "settings", body("{\"laps\":1,\"duration\":30}"), host, now);
        rules.act(r, "start", body("{}"), host, now);
        assertEquals("choosing", r.phase); assertTrue(rules.view(r, guest, now, null, false).choices().isEmpty());
        String word = r.choices.get(0); rules.act(r, "choose", body("{\"word\":\"" + word + "\"}"), host, now);
        assertNull(rules.view(r, guest, now, null, false).word());
        assertEquals(word, rules.view(r, host, now, null, false).word());
        rules.act(r, "guess", body("{\"text\":\"" + word.toUpperCase() + "\"}"), guest, now + 1500);
        assertEquals("reveal", r.phase); assertEquals(385, r.players.get(1).score); assertEquals(75, r.players.get(0).score);
        assertTrue(r.messages.stream().noneMatch(m -> m.type().equals("guess") && m.text().equals(word)));
        rules.advance(r, now + 8000); assertEquals(2, r.round); assertEquals(r.players.get(1).id, r.drawerId);
        rules.act(r, "choose", body("{\"word\":\"" + r.choices.get(0) + "\"}"), guest, now + 8000);
        rules.advance(r, now + 38001); assertEquals("reveal", r.phase);
        rules.advance(r, now + 45000); assertEquals("finished", r.phase);
        rules.act(r, "start", body("{}"), host, now + 45000); assertTrue(r.players.stream().allMatch(p -> p.score == 0));
    }
    @Test void geometryIsValidatedAndUndoRemovesWholeGesture() throws Exception {
        Room r = rules.fresh("ABC234", "Host", 100000); String host = r.players.get(0).token;
        String guest = rules.act(r,"join",body("{\"name\":\"Guest\"}"),"",100000).token;
        rules.act(r,"start",body("{}"),host,100000);
        rules.act(r,"choose",body("{\"word\":\""+r.choices.get(0)+"\"}"),host,100000);
        String stroke="{\"round\":1,\"stroke\":{\"id\":\"one\",\"gestureId\":\"gesture\",\"color\":\"#ef6848\",\"width\":7,\"points\":[[0.2,0.3],[0.6,0.7]]}}";
        assertThrows(GameException.class,()->rules.act(r,"stroke",body(stroke),guest,100000));
        rules.act(r,"stroke",body(stroke),host,100000); rules.act(r,"stroke",body(stroke),host,100000);
        assertEquals(1,r.strokes.size()); // Same stroke ID is idempotent.
        rules.act(r,"stroke",body(stroke.replace("one","two")),host,100000);
        rules.act(r,"undo",body("{\"round\":1}"),host,100000); assertTrue(r.strokes.isEmpty());
        assertThrows(GameException.class,()->rules.act(r,"stroke",body(stroke.replace("0.2","2.0")),host,100000));
        assertThrows(GameException.class,()->rules.act(r,"stroke",body(stroke.replace("\"round\":1","\"round\":0")),host,100000));
    }
    @Test void reconnectHostMigrationAndAutomaticChoiceWork() throws Exception {
        Room r=rules.fresh("ABC234","Host",100000);String host=r.players.get(0).token;
        String guest=rules.act(r,"join",body("{\"name\":\"Guest\"}"),"",100000).token;
        rules.act(r,"join",body("{}"),guest,100000);assertEquals(2,r.players.size());
        rules.act(r,"start",body("{}"),host,100000);rules.advance(r,116000);assertEquals("drawing",r.phase);
        rules.act(r,"leave",body("{}"),host,117000);assertEquals(r.players.get(1).id,r.hostId);
        String json=mapper.writeValueAsString(rules.view(r,guest,117000,null,false));
        assertFalse(json.contains(host));assertFalse(json.contains(guest));
    }
    @Test void repeatedCorrectGuessesCannotFarmPoints() throws Exception {
        Room r=rules.fresh("ABC234","Host",100000);String host=r.players.get(0).token;
        String guest=rules.act(r,"join",body("{\"name\":\"Guest\"}"),"",100000).token;
        rules.act(r,"join",body("{\"name\":\"Third\"}"),"",100000);
        rules.act(r,"start",body("{}"),host,100000);String word=r.choices.get(0);
        rules.act(r,"choose",body("{\"word\":\""+word+"\"}"),host,100000);
        rules.act(r,"guess",body("{\"text\":\""+word+"\"}"),guest,101000);
        int score=r.players.get(1).score;
        assertThrows(GameException.class,()->rules.act(r,"guess",body("{\"text\":\""+word+"\"}"),guest,102000));
        assertEquals(score,r.players.get(1).score);
    }
}
