# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy backend sources and build the Spring Boot jar
COPY . .
RUN chmod +x mvnw && ./mvnw -q -DskipTests clean package

FROM eclipse-temurin:21-jre
WORKDIR /app

# Render provides PORT at runtime; default locally to 8080
ENV PORT=8080

COPY --from=build /build/target/lyricmind-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

# -XX:+UseContainerSupport: honour cgroup memory/cpu limits set by Render
# -XX:MaxRAMPercentage: cap heap at 75% of container RAM instead of a hard -Xmx
# -Djava.security.egd: swap SecureRandom from blocking /dev/random to /dev/urandom,
#   which cuts several seconds off Spring Boot startup on Linux containers
ENTRYPOINT ["sh", "-c", "java \
  -Dserver.port=${PORT} \
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -Djava.security.egd=file:/dev/./urandom \
  -jar /app/app.jar"]

