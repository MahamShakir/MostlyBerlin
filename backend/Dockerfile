# ============================================================================
# backend/Dockerfile -- TICKET-I125
# Multi-stage: Maven builder -> JRE runtime. Final image ~180 MB.
# ============================================================================

# ---- Stage 1: build -------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy pom.xml FIRST so the dependency layer caches independently of source.
COPY pom.xml ./
RUN mvn -B -e dependency:go-offline

# Now copy source and build the JAR.
COPY src ./src
RUN mvn -B -e clean package -DskipTests

# ---- Stage 2: lean runtime ------------------------------------------------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S app && adduser -S app -G app

# wget is needed by HEALTHCHECK (alpine ships without curl by default).
RUN apk add --no-cache wget

USER app

COPY --from=build /workspace/target/*.jar /app/app.jar

EXPOSE 8080

# Liveness probe -- `docker ps` shows `(healthy)` once /actuator/health returns 200.
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

# -XX:+UseContainerSupport lets the JVM read cgroup limits so heap sizing
# honours the container's memory cap (default on JDK 17 but explicit is safer).
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "/app/app.jar"]