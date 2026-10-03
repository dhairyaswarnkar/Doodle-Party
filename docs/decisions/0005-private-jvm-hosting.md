# ADR 0005: Private JVM hosting

**Status:** Accepted; provider connection pending

Package the application as a JAR and optionally a Java container. Use Spring Security form login, CSRF protection, and a configured password for a private deployed prototype. Default local binding is loopback-only. Require a password before private-mode startup.

The current Sites runtime hosts the earlier Worker and cannot be treated as a Spring Boot JVM. A separate Java hosting connection is needed for a live rebuild. A shared private login is simpler than implementing individual accounts, but differs from Sites' per-account access policy. Deployment and broader sharing require an explicit hosting/access choice; no public audience is introduced automatically.
