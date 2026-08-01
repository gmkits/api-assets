# syntax=docker/dockerfile:1
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY apis/calendar/runtime/java apis/calendar/runtime/java
COPY apis/calendar/assets/runtime apis/calendar/assets/runtime
COPY apis/calendar/tests/golden apis/calendar/tests/golden
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -Dmaven.wagon.http.retryHandler.count=5 \
    -f apis/calendar/runtime/java/pom.xml \
    -pl calendar-service -am package -Dmaven.test.skip=true

FROM bellsoft/liberica-openjre-debian:21
ARG VERSION=1.0.0-rc.1
ARG SOURCE_COMMIT=unknown
ENV CALENDAR_RELEASE_VERSION=${VERSION} \
    SOURCE_COMMIT=${SOURCE_COMMIT} \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC -Djava.io.tmpdir=/tmp"
RUN useradd --system --uid 10001 --create-home calendar
WORKDIR /app
COPY --from=build /workspace/apis/calendar/runtime/java/calendar-service/target/calendar-service-*.jar app.jar
USER 10001
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=20s --retries=3 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080 && printf "GET /internal/health/live HTTP/1.0\r\n\r\n" >&3 && grep -q "200" <&3'
ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar /app/app.jar"]
