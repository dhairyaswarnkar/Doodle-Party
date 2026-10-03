# Doodle Party — Java / Spring Boot rebuild

A complete multiplayer drawing-and-guessing website for Dhairya Swarnkar. **All backend and test code is Java.** The browser uses HTML, CSS, and JavaScript, as agreed. Spring Boot serves both the frontend and API from one server.

## Start with the ready-to-run JAR

Install **Java 17 or newer**, then run the companion `Doodle_Party_Java.jar`:

```sh
java -jar Doodle_Party_Java.jar
```

Open **http://localhost:8080**. A JRE is enough for the JAR; building the source requires a JDK. The default server listens on the local machine only. Stop it with Ctrl+C.

## Run from source

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

macOS / Linux:

```sh
./mvnw spring-boot:run
```

The Maven Wrapper downloads Maven/dependencies on first use. No Node.js/npm installation is needed. Build an executable JAR with `./mvnw clean package` or `.\mvnw.cmd clean package`; the result is `target/doodle-party.jar`.

## Features

- Create/join private game rooms by code or invite link; 2–8 active players.
- Shared drawing canvas, colors, brush sizes, eraser, full-gesture undo, and clear.
- Live WebSocket updates for players, drawing, guesses, scores, and round transitions.
- Three word choices, automatic selection, timed rounds, hints, scoring, and replay.
- Server-controlled rules and scheduled timers; timers advance even without client polling.
- Refresh/reconnect support and host migration.
- Local JSON persistence, atomic file replacement, periodic saving, and expired-room cleanup.
- Optional private login using Spring Security, session cookies, and CSRF protection.

## Technology

| Area | Technology |
| --- | --- |
| Backend language | Java 17 |
| Application framework | Spring Boot 3.5.16, Spring MVC |
| Live transport | Spring WebSocket; native browser WebSocket client |
| Private access | Spring Security form login; BCrypt password encoding; CSRF |
| Persistence | Java filesystem + Jackson JSON snapshots; no JDBC, JPA, or Hibernate |
| Frontend | HTML, CSS, browser JavaScript |
| Build | Maven + Maven Wrapper |
| Tests | JUnit 5, Spring Boot integration tests, Java HTTP/WebSocket clients |
| Deployment artifact | Executable Java JAR; optional Docker image |

## Try a full game

Open two fresh tabs, use different nicknames, create a room in the first, and join in the second. Choose one turn per player and a 30-second timer. Start, pick a word, draw, and submit guesses from the other tab. Check the scores, next turn, timeout, results, and replay. Avoid duplicating a live game tab: browsers can copy its session storage.

Rooms are saved beneath `./data/rooms` relative to the working directory. They can survive a normal server restart. The browser's per-tab player token is required to restore the same player.

## Private mode

For a password-protected instance, set `GAME_PRIVATE_MODE=true` and `GAME_ACCESS_PASSWORD` to your own value of at least 12 characters. The login username is **host**. No deployment password is included.

PowerShell:

```powershell
$env:GAME_PRIVATE_MODE = "true"
$env:GAME_ACCESS_PASSWORD = Read-Host "Choose your private access password"
java -jar Doodle_Party_Java.jar
```

Use a hosting provider's secret manager for deployed values; do not commit them. The Docker configuration requires a password and keeps its Compose port bound to localhost. See [hosting](docs/HOSTING.md) for HTTPS, volumes, and connection requirements.

## Documentation

- [Setup and configuration](docs/GETTING_STARTED.md)
- [Code tour and Spring Boot layers](docs/CODE_TOUR.md)
- [Architecture](docs/ARCHITECTURE.md) and [standalone diagram](docs/architecture.mmd)
- [Game flow](docs/GAME_FLOW.md) and [state diagram](docs/game-flow.mmd)
- [HTTP and WebSocket API](docs/API.md)
- [Languages and dependencies](docs/LANGUAGES.md)
- [Persistence and data model](docs/DATA_MODEL.md)
- [Decision records](docs/decisions/README.md)
- [Testing](docs/TESTING.md) and [verification results](docs/VERIFICATION.md)
- [Hosting and limitations](docs/HOSTING.md)

## Hosting status

This is a genuine JVM application. The earlier Sites URL runs the previous JavaScript Worker version; it does not run this rebuild. A Java/container hosting connection is needed for a new live URL. The source and JAR run locally, and no public deployment or change to the earlier site's audience is performed by this download.
