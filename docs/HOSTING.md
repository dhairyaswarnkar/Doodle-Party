# Hosting the Java application privately

## Status

This source builds an executable Spring Boot JAR that needs a JVM or Java container host. The earlier Sites deployment executes a JavaScript Worker and does not execute this rebuild. A new Java-hosting account connection is required before a new private URL can be deployed.

No live URL for the Java rebuild is claimed by this package. No public deployment or audience change is included in the rebuild operation.

## Deployment requirements

- Java 17+ or a Docker runtime.
- One application instance with a writable persistent storage volume.
- HTTPS and WebSocket proxy forwarding for `/ws/game`.
- `BIND_ADDRESS=0.0.0.0` behind the hosting platform.
- `GAME_PRIVATE_MODE=true` and your own secret `GAME_ACCESS_PASSWORD` of at least 12 characters.
- `GAME_STORAGE_DIRECTORY` pointing to the mounted volume.
- The host's assigned `PORT` environment variable when applicable.

The login username is `host`; only you choose/share the password. This shared private gate differs from Sites account allowlists. Room hosts are independent game roles. Do not broaden access or publish without the owner's approval. Use a host secret manager for passwords and HTTPS for remote sessions.

## Docker locally

Set a password in a local `.env` copied from `.env.example`, then run:

```sh
docker compose up --build
```

Compose exposes the application only on `127.0.0.1:8080` and mounts a named data volume. The image runs as a non-root account and requires private mode credentials. `docker compose down` stops it while retaining the named volume; adding `--volumes` deletes stored room data, so avoid that unless you intend to reset it.

The Dockerfile is supplied as deployment configuration; a Docker image build requires a Docker installation and was not part of the local JVM verification.

## Boundaries

This version supports one JVM. Separate replicas do not share in-memory room locks, WebSocket subscribers, or file snapshots. Horizontal scaling requires a shared room actor/event bus and shared persistence. Room access has no individual application accounts, public matchmaking, room passwords, moderation/ban tools, or production load-test results. The shared login is intended for controlled prototype testers.

Room snapshots can lose the latest unsaved updates on an abrupt crash, and network failures can delay pushed drawing updates. The app has player/word/geometry limits and secret filtering, but no comprehensive global traffic limiter. Keep the service private until production hardening and hosting/access decisions are reviewed.
