# syntax=docker/dockerfile:1

# ---- Build stage: compile the app and unpack the jar into layers ----
FROM eclipse-temurin:21-jdk-noble AS build
WORKDIR /src

# Download Gradle and dependencies first, so this layer is cached until the build files change.
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies > /dev/null

COPY config config
COPY src src
# Tests run in CI with Testcontainers; the image build only packages the app.
RUN ./gradlew --no-daemon bootJar -x test \
 && java -Djarmode=tools -jar build/libs/cuckoos-nest-*.jar extract --layers --launcher --destination /layers

# ---- Runtime stage: a small JRE image that runs as a non-root user ----
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S nest && adduser -S nest -G nest
WORKDIR /app

# Copy the jar layers from least to most often changed, for better caching.
COPY --from=build /layers/dependencies/ ./
COPY --from=build /layers/spring-boot-loader/ ./
COPY --from=build /layers/snapshot-dependencies/ ./
COPY --from=build /layers/application/ ./

USER nest
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"UP"' || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "org.springframework.boot.loader.launch.JarLauncher"]
