# ---- Build stage ----
# Requires network access to Maven Central for `dependency:go-offline` (no way
# around this for a Java build without a pre-baked local repo/mirror). See
# ../backend/.dockerignore — without it a stale local `target/` bloats the
# build context needlessly.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -Dmaven.test.skip=true -B

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S lms && adduser -S lms -G lms
COPY --from=build /app/target/lms-backend.war app.war
USER lms
EXPOSE 8080
# wget is provided by busybox in the alpine base — no extra package needed.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://127.0.0.1:8080/api/v1/actuator/health/liveness || exit 1
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75", "-jar", "app.war"]
