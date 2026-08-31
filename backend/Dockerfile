# ---- build ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar

# ---- run ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
RUN groupadd -r app && useradd -r -g app app
COPY --from=build /app/build/libs/app.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
