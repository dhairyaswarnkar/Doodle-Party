# ADR 0002: WebSocket push

**Status:** Accepted

Use Spring's direct WebSocket API with native browser WebSockets. HTTP accepts CSRF-protected actions; sockets push player-specific state and carry subscribe/heartbeat messages. A Java scheduler advances timers without polling.

Polling was easier for the earlier Worker prototype but introduced update delay and repeated database writes. STOMP/broker messaging would add protocol/infrastructure complexity. Direct JSON sockets fit this small game, but require connection recovery, concurrent-send protection, reverse-proxy upgrade support, and careful synchronization. Matching canvas revisions avoid resending unchanged strokes.
