# HTTP and WebSocket API

All endpoints are same-origin on the Spring Boot server. Protected hosting first requires a Spring Security login. Game identity is a separate per-tab `X-Player-Token` header. Responses are JSON, and mutable game responses have `Cache-Control: no-store`.

## CSRF

Fetch `GET /api/csrf` before your first POST. It returns `headerName` and `token`. Preserve the session cookies and send that token using the reported header. The browser client handles this automatically. Java integration tests use a `CookieManager` to do the same.

## HTTP routes

| Method | Route | Body / meaning |
| --- | --- | --- |
| GET | `/api/health` | Public minimal health info: Java Spring Boot backend and WebSocket transport. |
| GET | `/api/csrf` | CSRF token; protected by login in private mode. |
| POST | `/api/rooms` | `{"name":"Dhairya"}`; create room, status 201. |
| POST | `/api/rooms/{code}/join` | `{"name":"Friend"}`; existing token reconnects its player. |
| GET | `/api/rooms/{code}` | One filtered room snapshot; frontend does not continuously poll this. |
| POST | `/api/rooms/{code}/settings` | `{"laps":1,"duration":30}`; room host only. |
| POST | `/api/rooms/{code}/start` | `{}`; room host, at least two active players. |
| POST | `/api/rooms/{code}/choose` | `{"word":"rocket"}`; artist must choose an offered word. |
| POST | `/api/rooms/{code}/stroke` | Normalized stroke below; artist during drawing. |
| POST | `/api/rooms/{code}/undo` | `{"round":1}`; undo last complete gesture. |
| POST | `/api/rooms/{code}/clear` | `{"round":1}`; clear current drawing. |
| POST | `/api/rooms/{code}/guess` | `{"text":"rocket"}`; guess or chat, according to phase. |
| POST | `/api/rooms/{code}/leave` | `{}`; mark departed and transfer host if necessary. |

Use `?cv=<canvasRevision>` to omit unchanged strokes. Standard snapshots include code, phase, host/artist IDs, round/settings, deadline/serverNow, monotonically increasing room revision, word/hint/choices, player list, `me`, canvas revision/strokes, messages, and last-round details. `token` is populated only on create/join responses; it is null in ordinary state pushes. Full words and choices are filtered according to role.

```json
{
  "round": 1,
  "stroke": {
    "id": "chunk-1",
    "gestureId": "gesture-1",
    "color": "#ef6848",
    "width": 7,
    "points": [[0.2, 0.3], [0.6, 0.7]]
  }
}
```

Stroke colors are six-digit hex values. Widths are 3, 7, 14, or 24. A chunk contains 1–100 finite coordinate pairs between 0 and 1. Repeated chunk IDs are deduplicated. Up to 1800 chunks are stored per round. A `gestureId` groups chunks for undo. Names are 1–20 characters and guesses/chat are 1–100 characters.

Expected errors are `{"error":"message"}`: 400 for invalid input/phase or malformed JSON, 401 for missing game token, 403 for role or security violations, 404 for unknown room/action, 409 for stale round, 410 for expired room, and 503 for capacity limits. Security-filter errors can use Spring's standard response rather than this controller DTO.

## WebSocket

Connect to `/ws/game` using `ws://` locally and `wss://` over HTTPS. Same-origin restrictions apply. Authenticate the room session in the first message, keeping its token out of URLs:

```json
{"type":"subscribe","code":"ABC234","token":"YOUR_PLAYER_TOKEN"}
```

The server sends `{"type":"state","state":{...RoomView...}}`. It sends independent filtered views to each player. Unchanged canvas geometry is represented by `strokes: null`. A fresh subscription gets the full canvas.

The browser sends `{"type":"ping"}` every ten seconds; this refreshes presence. The server replies with `{"type":"pong","serverNow":123456789}` and may push updated presence state. Invalid messages receive a `type: error` response. Incoming text messages are limited to 16,000 bytes/characters according to the underlying WebSocket buffer implementation.

WebSockets push game state; game mutations use the CSRF-protected HTTP routes. Reconnect uses the same room/player token and obtains a fresh snapshot. This protocol is direct JSON, with no STOMP broker or SockJS dependency.
