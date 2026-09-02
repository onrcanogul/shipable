# Two stages: build with the JDK, run on the JRE. The runtime image carries no compiler,
# no Maven, and no source.

# ---------- build ----------
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /build

# Copy the wrapper and every pom first, then resolve dependencies. Docker caches this
# layer, so editing a Java file does not re-download the internet on every build.
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY platform/pom.xml platform/
COPY platform/core/pom.xml platform/core/
COPY platform/identity/pom.xml platform/identity/
COPY platform/billing/pom.xml platform/billing/
COPY platform/quota/pom.xml platform/quota/
COPY platform/notifications/pom.xml platform/notifications/
COPY platform/analytics/pom.xml platform/analytics/
COPY platform/appconfig/pom.xml platform/appconfig/
COPY platform/privacy/pom.xml platform/privacy/
COPY domain/pom.xml domain/
COPY host/pom.xml host/
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY platform platform
COPY domain domain
COPY host host
# Tests ran in CI before this image was built. Running them again here would need a
# database and a Docker daemon inside the build.
RUN ./mvnw -B -q package -DskipTests

# ---------- run ----------
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# wget for the compose healthcheck; the alpine JRE image ships without curl.
RUN apk add --no-cache wget \
    && addgroup -S app && adduser -S -G app app

COPY --from=build /build/host/target/host-*.jar app.jar
USER app

EXPOSE 8080

# MaxRAMPercentage rather than a fixed -Xmx: the JVM then sizes the heap from the
# container limit, so changing the limit does not silently leave the heap wrong.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
