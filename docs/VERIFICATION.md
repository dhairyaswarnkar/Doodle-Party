# Verification record

Verified on 2026-10-03 using Java 17.0.20, Maven 3.9.11, and Spring Boot 3.5.16.

## Automated checks

`mvn clean package` completed successfully: **9 tests, 0 failures, 0 errors, 0 skipped**.

| Test suite | Tests | Coverage |
| --- | ---: | --- |
| GameRulesTest | 4 | Authority, scoring, secrets, drawing validation, full-gesture undo, reconnect, host migration, replay |
| MultiplayerIntegrationTest | 2 | Two real HTTP/WebSocket clients, drawing and chat pushes, scoring, scheduled round transitions and timeout, reconnect, CSRF and invalid JSON |
| PrivateAccessIntegrationTest | 2 | Anonymous login gate, successful password login, required private password configuration |
| RoomStoreTest | 1 | Atomic JSON save/load and deletion |

The integration tests start real Spring Boot HTTP servers. The timer check waits for a real 30-second drawing timeout. Live updates are received through Java WebSocket clients.

The packaged executable JAR was launched independently using `java -jar`. Health, HTML, CSS, JavaScript, and CSRF endpoints returned successfully. Browser JavaScript passed a syntax check. Documentation links and the source archive contents were checked.

## Not verified here

- Automated browser pointer interactions and responsive visual behavior.
- Docker image build or a remote Java hosting deployment.
- Production load, multi-instance operation, or recovery from machine/storage failure.

The old Sites URL remains the previous implementation. No deployed URL for this Java rebuild is asserted.
