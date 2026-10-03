# Languages and dependencies

| Language / tool | Role |
| --- | --- |
| Java 17 | Every backend class, authoritative game rules, persistence, scheduling, HTTP/WebSocket handling, and tests. |
| HTML | Frontend controls, forms, panels, and canvas element. |
| CSS | Responsive layout and appearance. |
| JavaScript | Browser-only drawing, rendering, API calls, CSRF handling, and WebSocket connection. |
| XML | Maven `pom.xml` and dependency/build configuration. |
| Properties | Spring application settings and wrapper distribution URL. |
| JSON | API messages and server room snapshot format. |
| Markdown / Mermaid | Documentation and diagrams. |
| Shell / Windows batch | Standard Maven Wrapper launchers. |
| Dockerfile / YAML | Optional JVM container build and local Compose deployment. |

The Spring Boot parent pins compatible transitive versions. `spring-boot-starter-web` supplies MVC, embedded Tomcat, and Jackson. `spring-boot-starter-websocket` supplies Spring's WebSocket API. `spring-boot-starter-security` supplies login, CSRF, and password encoding. `spring-boot-starter-test` supplies Java testing libraries.

Persistence uses Java files and Jackson snapshots. There is no JDBC dependency, Hibernate/JPA entity model, database console, npm package, Node backend, or Python runtime requirement. Maven downloads build dependencies on first use; the packaged JAR includes its runtime dependencies.
