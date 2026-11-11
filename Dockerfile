# syntax=docker/dockerfile:1

ARG JAVA_VERSION=25
ARG ALPINE_VERSION=3.24

# --- 1. Build the JAR. Runs on the build machine's architecture: the JAR is the same everywhere.
FROM --platform=$BUILDPLATFORM eclipse-temurin:${JAVA_VERSION}-jdk-alpine AS build
WORKDIR /src
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q -pl dulo-service -am package -DskipTests -Dfmt.skip -Denforcer.skip \
 && cp dulo-service/target/dulo-server-*.jar /dulo-server.jar \
 && java -Djarmode=tools -jar /dulo-server.jar extract --layers --destination /layers \
 && java -Djarmode=tools -jar /dulo-server.jar extract --destination /exploded

# --- 2. Trimmed Java runtime with only the modules Dulo uses, for the target architecture.
FROM eclipse-temurin:${JAVA_VERSION}-jdk-alpine AS jre
ARG JAVA_VERSION
COPY --from=build /exploded /exploded
# Modules found by jdeps, plus modules that are only loaded at runtime (charsets for database
# drivers, DNS lookups, management for health checks).
RUN modules="$(jdeps --ignore-missing-deps -q --recursive --multi-release ${JAVA_VERSION} \
        --print-module-deps --class-path '/exploded/lib/*' /exploded/dulo-server.jar)" \
 && jlink \
        --add-modules "${modules},jdk.charsets,jdk.naming.dns,jdk.management,jdk.zipfs" \
        --strip-debug --no-man-pages --no-header-files --compress=zip-6 \
        --output /jre

# --- 3. Runtime image.
FROM alpine:${ALPINE_VERSION}

ARG VERSION=dev
LABEL org.opencontainers.image.title="Dulo Server" \
      org.opencontainers.image.description="Self-hosted family tree server" \
      org.opencontainers.image.source="https://github.com/rdlf0/dulo-server" \
      org.opencontainers.image.version="${VERSION}"

ENV JAVA_HOME=/opt/java \
    PATH=/opt/java/bin:$PATH \
    DULO_DATA_DIR=/data \
    LANG=C.UTF-8

RUN addgroup -S -g 10001 dulo \
 && adduser -S -D -H -u 10001 -G dulo dulo \
 && mkdir -p /data /config \
 && chown dulo:dulo /data

COPY --from=jre /jre /opt/java
WORKDIR /app
COPY --from=build /layers/dependencies/ ./
COPY --from=build /layers/spring-boot-loader/ ./
COPY --from=build /layers/snapshot-dependencies/ ./
COPY --from=build /layers/application/ ./

USER dulo
EXPOSE 8080
VOLUME /data

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO /dev/null http://127.0.0.1:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "--enable-native-access=ALL-UNNAMED", "-jar", "/app/dulo-server.jar"]
