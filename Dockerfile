FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
RUN groupadd --system doodle && useradd --system --gid doodle --home /app doodle
WORKDIR /app
COPY --from=build /build/target/doodle-party.jar app.jar
RUN mkdir -p /app/data/rooms && chown -R doodle:doodle /app
USER doodle
ENV BIND_ADDRESS=0.0.0.0 GAME_PRIVATE_MODE=true GAME_STORAGE_DIRECTORY=/app/data/rooms
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
