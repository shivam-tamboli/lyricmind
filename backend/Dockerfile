# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy backend sources and build the Spring Boot jar
COPY backend/ ./
RUN chmod +x mvnw && ./mvnw -q -DskipTests clean package

FROM eclipse-temurin:21-jre
WORKDIR /app

# Render provides PORT at runtime; default locally to 8080
ENV PORT=8080

COPY --from=build /build/target/lyricmind-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT} -jar /app/app.jar"]

