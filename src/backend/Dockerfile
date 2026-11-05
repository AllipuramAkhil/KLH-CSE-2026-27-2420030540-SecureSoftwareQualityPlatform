# =========================================================
# ASSQP Backend - Multi-stage Dockerfile
# Stage 1: build the Spring Boot jar with Maven
# Stage 2: run it on a slim JRE
# =========================================================

# ---------- Stage 1: Build ----------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Cache dependencies first (only re-downloaded when pom.xml changes)
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Now copy sources and build
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:21-jre-jammy AS runtime

# Run as a non-root user
RUN groupadd -r assqp && useradd -r -g assqp assqp

WORKDIR /app

# curl is used by the container HEALTHCHECK to hit the actuator endpoint
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /build/target/*.jar app.jar

RUN mkdir -p /app/uploads && chown -R assqp:assqp /app

USER assqp

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=5 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
