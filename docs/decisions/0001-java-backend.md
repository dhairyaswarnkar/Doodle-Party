# ADR 0001: Java backend

**Status:** Accepted

The owner explicitly requested a complete Java/Spring Boot backend and permitted HTML/CSS/JavaScript in the browser. Use Java 17, Spring Boot, MVC controllers, services, records, and Java integration tests. Spring Boot serves static frontend files from the same origin. This also gives a familiar controller/service/storage structure for learning Java backend development.

Alternatives were retaining the Worker backend or building a Java desktop UI. The selected approach remains a website and follows the requested backend language. It requires JVM hosting and Maven dependencies; the packaged executable JAR removes the need for Maven at runtime.
