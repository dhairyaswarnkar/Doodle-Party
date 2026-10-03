package com.dhairya.doodle;

import com.dhairya.doodle.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.net.*;
import java.net.http.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"app.storage-directory=", "app.private-mode=true", "app.access-password=TestContextOnly123", "server.address=127.0.0.1"})
class PrivateAccessIntegrationTest {
    @LocalServerPort int port;
    @Test void anonymousVisitorsCannotReadGameAndLoginEstablishesPrivateSession() throws Exception {
        var client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        String base="http://127.0.0.1:"+port;
        var anonymous=client.send(HttpRequest.newBuilder(URI.create(base+"/")).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(302,anonymous.statusCode());assertTrue(anonymous.headers().firstValue("location").orElse("").endsWith("/login"));
        var login=client.send(HttpRequest.newBuilder(URI.create(base+"/login")).GET().build(),HttpResponse.BodyHandlers.ofString());
        var matcher=Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(login.body());assertTrue(matcher.find(),login.body());
        String form="username=host&password=TestContextOnly123&_csrf="+URLEncoder.encode(matcher.group(1),java.nio.charset.StandardCharsets.UTF_8);
        var authenticated=client.send(HttpRequest.newBuilder(URI.create(base+"/login")).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(form)).build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(302,authenticated.statusCode());
        var page=client.send(HttpRequest.newBuilder(URI.create(base+"/")).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,page.statusCode());assertTrue(page.body().contains("Your little art club"));
    }
    @Test void privateModeCannotStartWithoutAProperPassword() {
        assertThrows(IllegalStateException.class,()->new SecurityConfig("",true));
        assertThrows(IllegalStateException.class,()->new SecurityConfig("short",true));
    }
}
