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
# glibc otherwise allocates a malloc arena per thread, which costs tens of MB of RSS
ENV MALLOC_ARENA_MAX=2
# Sized for a handful of concurrent tutors. Measured live heap after a full GC is
# ~36 MB, so 256m leaves plenty of headroom; SerialGC costs ~0.5 MB of native
# structures where G1 committed ~59 MB. MaxRAMPercentage is deliberately not used:
# it sizes the heap from the container limit, which on Railway is far larger than
# this app needs, and the JVM never hands that memory back.
ENTRYPOINT ["java", \
  "-Xms128m", "-Xmx256m", \
  "-XX:+UseSerialGC", \
  "-Xss512k", \
  "-XX:MaxMetaspaceSize=160m", \
  "-XX:ReservedCodeCacheSize=64m", \
  "-jar", "app.jar"]
