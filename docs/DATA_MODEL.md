# Data model and persistence

`Room` is a server-only aggregate. One instance contains the entire game room, guarded by a per-room Java lock.

| Group | Fields |
| --- | --- |
| Identity | `code`, `hostId`, `drawerId` |
| Lifecycle | `createdAt`, `updatedAt`, `deadline`, `phase`, `revision` |
| Settings / schedule | `laps`, `duration`, `round`, `totalRounds`, `order` |
| Players | ID, token, name, score, guessed/left flags, lastSeen/lastGuess timestamps |
| Secret | `word`, `choices` |
| Drawing | `strokes`, `canvasRevision` |
| Conversation/results | `messages`, `lastRound` |

Stroke/message/result types are immutable Java records. Stored point lists are immutable after validation. Public `RoomView` records copy lists and filter secret fields; raw `Room` is never returned by a controller.

`revision` tracks accepted room actions or scheduled transitions. `canvasRevision` tracks geometry changes independently. Client views reject older revisions and use the canvas revision to avoid receiving redundant stroke geometry.

## Snapshots

`RoomStore` serializes rooms with Jackson and writes `<room-code>.json` under the configured storage folder. Dirty rooms are saved at a one-second fixed delay. Writing uses a temporary file and atomic replacement where supported. Startup loads matching snapshot files; normal shutdown flushes dirty rooms.

A hard crash can lose unsaved updates. A persistent disk or volume is required to preserve files across redeployments. Corrupted snapshots fail startup rather than being silently discarded. Files include hidden words and room session tokens and must remain private.

Rooms inactive for more than 24 hours are removed during the cleanup task, including their snapshot file. Active heartbeats refresh activity. Player records are retained during a room lifetime, with an eight-active-player limit and a maximum of twenty accumulated records.

This implementation uses Java filesystem persistence. It has no SQL schema, JDBC/JPA/Hibernate dependency, or database credentials. Future database persistence would replace the store and require coordination changes if deploying multiple JVM replicas.
