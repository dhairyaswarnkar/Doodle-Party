package com.dhairya.doodle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"app.storage-directory=", "server.address=127.0.0.1"})
class MultiplayerIntegrationTest {
    @LocalServerPort int port;
    final ObjectMapper mapper=new ObjectMapper();
    String base(){return "http://127.0.0.1:"+port;}
    class Client {
        final HttpClient http=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        String csrfHeader,csrf,token="";
        Client() throws Exception {
            JsonNode protection=request("/api/csrf",null,200);csrfHeader=protection.path("headerName").asText();csrf=protection.path("token").asText();
        }
        JsonNode request(String path,Object body,int expected) throws Exception {
            var builder=HttpRequest.newBuilder(URI.create(base()+path)).timeout(java.time.Duration.ofSeconds(10));
            if(!token.isEmpty())builder.header("X-Player-Token",token);
            if(body==null)builder.GET();else builder.header("Content-Type","application/json").header(csrfHeader,csrf).POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
            var response=http.send(builder.build(),HttpResponse.BodyHandlers.ofString());
            assertEquals(expected,response.statusCode(),response.body());return mapper.readTree(response.body());
        }
        Socket socket(String code) throws Exception {
            Socket listener=new Socket();listener.ws=http.newWebSocketBuilder().header("Origin",base()).buildAsync(URI.create("ws://127.0.0.1:"+port+"/ws/game"),listener).get(5,TimeUnit.SECONDS);
            listener.ws.sendText(mapper.writeValueAsString(Map.of("type","subscribe","code",code,"token",token)),true).join();return listener;
        }
    }
    class Socket implements WebSocket.Listener {
        WebSocket ws;final BlockingQueue<JsonNode> messages=new LinkedBlockingQueue<>();final StringBuilder partial=new StringBuilder();
        public void onOpen(WebSocket socket){socket.request(1);}
        public CompletionStage<?> onText(WebSocket socket,CharSequence data,boolean last){
            partial.append(data);if(last){try{messages.add(mapper.readTree(partial.toString()));}catch(Exception e){throw new RuntimeException(e);}partial.setLength(0);}socket.request(1);return CompletableFuture.completedFuture(null);
        }
        JsonNode state(Predicate<JsonNode> predicate,long timeoutMillis) throws Exception {
            long end=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
            while(System.nanoTime()<end){var message=messages.poll(Math.max(1,TimeUnit.NANOSECONDS.toMillis(end-System.nanoTime())),TimeUnit.MILLISECONDS);if(message!=null&&message.path("type").asText().equals("state")&&predicate.test(message.path("state")))return message.path("state");}
            throw new AssertionError("Expected WebSocket state did not arrive");
        }
        void close(){if(ws!=null)ws.sendClose(WebSocket.NORMAL_CLOSURE,"done");}
    }
    @Test void twoPlayersReceiveLiveCanvasScoresTimersAndReplay() throws Exception {
        Client host=new Client(),guest=new Client();
        JsonNode created=host.request("/api/rooms",Map.of("name","Dhairya"),201);host.token=created.path("token").asText();
        String code=created.path("code").asText(),path="/api/rooms/"+code;
        JsonNode joined=guest.request(path+"/join",Map.of("name","Friend"),200);guest.token=joined.path("token").asText();
        Socket hostSocket=host.socket(code),guestSocket=guest.socket(code);
        try {
            hostSocket.state(s->s.path("players").size()==2,3000);guestSocket.state(s->s.path("players").size()==2,3000);
            guest.request(path+"/start",Map.of(),403);
            host.request(path+"/settings",Map.of("laps",1,"duration",30),200);
            var choosing=host.request(path+"/start",Map.of(),200);String word=choosing.path("choices").get(0).asText();
            guestSocket.state(s->s.path("phase").asText().equals("choosing")&&s.path("choices").isEmpty(),3000);
            host.request(path+"/choose",Map.of("word",word),200);
            var hidden=guestSocket.state(s->s.path("phase").asText().equals("drawing"),3000);assertTrue(hidden.path("word").isNull());
            var stroke=Map.of("round",1,"stroke",Map.of("id","stroke-1","gestureId","gesture-1","color","#ef6848","width",7,"points",java.util.List.of(java.util.List.of(.2,.3),java.util.List.of(.6,.7))));
            guest.request(path+"/stroke",stroke,403);host.request(path+"/stroke",stroke,200);
            guestSocket.state(s->s.path("strokes").isArray()&&s.path("strokes").size()==1,3000);
            guest.request(path+"/guess",Map.of("text","wrong"),200);
            hostSocket.state(s->{for(var m:s.path("messages"))if(m.path("text").asText().equals("wrong"))return true;return false;},3000);
            Thread.sleep(500);guest.request(path+"/guess",Map.of("text",word),200);
            var reveal=hostSocket.state(s->s.path("phase").asText().equals("reveal"),3000);
            assertTrue(reveal.path("players").get(1).path("score").asInt()>100);assertEquals(75,reveal.path("players").get(0).path("score").asInt());
            // No polling or client action: scheduler must advance and push the next turn.
            var turn=guestSocket.state(s->s.path("phase").asText().equals("choosing")&&s.path("round").asInt()==2,9000);
            guest.request(path+"/choose",Map.of("word",turn.path("choices").get(0).asText()),200);
            var timeout=hostSocket.state(s->s.path("phase").asText().equals("reveal")&&s.path("round").asInt()==2,35000);
            assertEquals("Time's up!",timeout.path("lastRound").path("reason").asText());
            hostSocket.state(s->s.path("phase").asText().equals("finished"),9000);
            var replay=host.request(path+"/start",Map.of(),200);assertEquals("choosing",replay.path("phase").asText());assertEquals(0,replay.path("players").get(0).path("score").asInt());
            guestSocket.close();guestSocket=guest.socket(code);guestSocket.state(s->s.path("players").size()==2,3000);
            guest.request(path+"/join",Map.of(),200);assertEquals(2,guest.request(path,null,200).path("players").size());
            host.request(path+"/leave",Map.of(),200);
            var migrated=guestSocket.state(s->s.path("hostId").asText().equals(joined.path("me").path("id").asText()),3000);
            assertEquals(joined.path("me").path("id").asText(),migrated.path("hostId").asText());
        } finally {hostSocket.close();guestSocket.close();}
    }
    @Test void csrfAndInvalidJsonAreRejected() throws Exception {
        var noCsrf=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(base()+"/api/rooms")).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{} ")).build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(403,noCsrf.statusCode());
        Client client=new Client();var bad=client.http.send(HttpRequest.newBuilder(URI.create(base()+"/api/rooms")).header("Content-Type","application/json").header(client.csrfHeader,client.csrf).POST(HttpRequest.BodyPublishers.ofString("{")).build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(400,bad.statusCode());
    }
}
