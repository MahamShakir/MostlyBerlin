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
#FROM eclipse-temurin:17-jre-alpine
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN addgroup --system app && adduser --system --ingroup app --no-create-home app

RUN apt-get update && apt-get install -y --no-install-recommends wget && rm -rf /var/lib/apt/lists/*

USER app
COPY --from=build /workspace/target/*.jar /app/app.jar
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "/app/app.jar"]