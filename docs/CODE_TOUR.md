# Read the codebase

This project follows familiar Spring Boot layers. Start with `GameRules`, then follow one action through the controller and service.

| Class / file | Responsibility |
| --- | --- |
| `DoodleApplication` | Boot entry point and scheduled-task activation. |
| `controller/RoomController` | HTTP routes, player-token header, JSON request/response handling. |
| `controller/ApiExceptionHandler` | Expected errors and malformed JSON responses. |
| `service/GameRules` | Pure Java rules: roles, words, strokes, scores, rounds, and filtered views. |
| `service/RoomService` | Room registry, per-room locking, lifecycle, scheduled ticking/saving/cleanup. |
| `storage/RoomStore` | Load JSON files and atomically replace snapshots. |
| `model/Room` | Mutable server-only aggregate, players, immutable stroke/message records. |
| `model/RoomView` | Response DTO with secrets filtered by player role. |
| `websocket/GameSocketHandler` | Validate subscriptions, heartbeats, and push each player's view. |
| `config/WebSocketConfig` | Register `/ws/game` with same-origin restrictions. |
| `config/SecurityConfig` | Private form login, CSRF, and password policy. |
| `resources/static/index.html` | Browser structure and controls. |
| `resources/static/style.css` | Responsive styling. |
| `resources/static/app.js` | Browser drawing/rendering, fetch actions, WebSocket reconnection. |

## Trace a guess

The browser calls `POST /api/rooms/{code}/guess`. Spring Security checks login/CSRF. `RoomController` forwards the body/token to `RoomService.action()`. The service locks that room and calls `GameRules.act()`. Java checks the role, normalizes the answer, awards points, and possibly ends the round. The service builds a `RoomView`, releases the room lock, and publishes `RoomChanged`. The socket handler sends a separate filtered view to each subscribed player.

## Trace a timer

`RoomService.tick()` runs about every 250 ms. It calls `GameRules.advance()` for each room and publishes changes. A client does not need to poll to start the next round. Rule tests pass an explicit `now` timestamp so they can simulate expiry immediately.

## Trace drawing

`app.js` captures pointer events and paints pending points immediately. It sends normalized chunks with a `gestureId`. The Java engine validates geometry and deduplicates stroke IDs. Undo removes all consecutive chunks belonging to the last gesture. WebSocket subscribers receive the changed canvas. A matching canvas revision avoids resending unchanged geometry.

## Common edits

| Change | Edit |
| --- | --- |
| Add words | `WORDS` in `GameRules` |
| Change points | Correct-answer branch in `GameRules.act()` |
| Change round options | Settings validation in Java and options in `app.js` |
| Change colors/layout | `style.css`, palette in `app.js` |
| Add an action | Controller allowlist, rule branch, frontend control, Java tests |
| Add database storage later | Replace `RoomStore` and reconsider multi-instance room coordination |

Backend JavaScript from the earlier prototype is not part of this project's runtime or build. JavaScript in this rebuild is browser-side code only.
