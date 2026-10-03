# Setup and configuration

## Requirements

Use Java 17+ with `java --version`. Use a JDK for source builds; a JRE is enough for the executable JAR. The project targets Java 17 bytecode. Its Maven Wrapper provides Maven 3.9.11; the first build needs internet access to Maven Central.

Open the project folder in IntelliJ IDEA or VS Code with Java support. Import `pom.xml` as a Maven project and run `DoodleApplication.main()`, or use the wrapper commands in the README.

| Command | Purpose |
| --- | --- |
| `./mvnw spring-boot:run` | Compile and serve locally. |
| `./mvnw test` | Run Java rule, persistence, security, and HTTP/WebSocket tests. |
| `./mvnw clean package` | Test and produce `target/doodle-party.jar`. |
| `java -jar target/doodle-party.jar` | Run the packaged app. |

On Windows, substitute `.\mvnw.cmd` for `./mvnw`. If an extracted Unix wrapper is not executable, run `chmod +x mvnw` once.

## Configuration

| Environment variable | Default | Meaning |
| --- | --- | --- |
| `PORT` | 8080 | HTTP and WebSocket port. |
| `BIND_ADDRESS` | 127.0.0.1 | Local-only bind; use `0.0.0.0` behind authorized hosting. |
| `GAME_STORAGE_DIRECTORY` | `./data/rooms` | Writable folder for room snapshots; empty string disables persistence. |
| `GAME_PRIVATE_MODE` | false | When true, startup requires an access password of at least 12 characters. |
| `GAME_ACCESS_PASSWORD` | empty | Enables private form login when nonblank. Username: `host`. |

Spring Boot reads environment variables automatically through `application.properties`. A `.env` file is not automatically loaded by the JAR; the included example is for Docker Compose.

PowerShell example for another port:

```powershell
$env:PORT = "8081"
.\mvnw.cmd spring-boot:run
```

macOS/Linux:

```sh
PORT=8081 ./mvnw spring-boot:run
```

## Troubleshooting

| Symptom | Check |
| --- | --- |
| Build reports unsupported Java release | Ensure the JDK selected by Maven/IDE is Java 17+. |
| First build cannot download dependencies | Check your network/proxy settings for Maven Central. No workspace proxy credentials are included. |
| Port in use | Stop the other service or set `PORT`. |
| Private startup fails | Provide your own `GAME_ACCESS_PASSWORD` with at least 12 characters. |
| Browser shows login | Use the configured `host` account/password; local default mode has no login requirement. |
| HTTP action returns 403 | Reload for a fresh CSRF token; ensure you are signed in and using the correct role. |
| Socket keeps reconnecting | Check server status, reverse-proxy WebSocket support, and session login. |
| Room doesn't survive restart | Mount/preserve `GAME_STORAGE_DIRECTORY`; ephemeral hosting disks do not persist across redeploys. |
| Two tabs represent one player | Open fresh tabs instead of duplicating an active tab. |
| Corrupted room snapshot prevents startup | Back up the data folder, identify the invalid JSON file from server diagnostics, then repair or move that snapshot. |

There is no hot-reload watcher configured. Restart after source changes. Never share the data folder: it contains hidden words and player tokens.
