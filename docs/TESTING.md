# Java tests

Run `./mvnw test` or `.\mvnw.cmd test`. Java tests launch their own embedded servers on random local ports and use temporary/in-memory persistence. A separate running application is not required.

| Test class | Coverage |
| --- | --- |
| `GameRulesTest` | Roles, two-player requirement, hidden words/choices, score formula, timer transitions, replay, geometry/stale-round validation, retry deduplication, full-gesture undo, reconnect, host migration, repeated-answer prevention. |
| `RoomStoreTest` | Atomic snapshot serialization, player-session restore, and stored-file deletion. |
| `MultiplayerIntegrationTest` | Actual Java HTTP and WebSocket clients, CSRF, two independent sessions, pushed canvas/chat/scores, real timeout, scheduled next turn without polling, replay, reconnect, host transfer, and malformed JSON. |
| `PrivateAccessIntegrationTest` | Anonymous visitors redirected to login, real form login establishing a session, private startup password requirement. |

The multiplayer integration test waits for one actual 30-second drawing period and two six-second intermissions. Allow roughly a minute plus JVM startup/build/network time. Unit tests use supplied timestamps and finish quickly.

Test-only Mockito configuration uses the subclass mock maker, which avoids JVM agent self-attachment in restricted environments. These tests do not depend on inline/final-class mocking.

## Manual browser checks

Use two fresh tabs at localhost. Verify room creation/join, drawing appearance, eraser/undo/clear, guesses, role changes, timer, final ranking, replay, and refresh reconnection. Use your browser's device toolbar at about 390 pixels wide to check mobile wrapping and touch input.

The automated suite tests the actual Spring HTTP/WebSocket backend. It does not automate browser pointer events or establish mobile-browser/accessibility compatibility. Test outcomes for this delivered rebuild are recorded in `VERIFICATION.md` at the project root.
