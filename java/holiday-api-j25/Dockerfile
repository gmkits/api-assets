FROM bellsoft/liberica-openjdk-alpine:25 AS build
RUN apk add --no-cache maven
WORKDIR /workspace
COPY java ./java
RUN mvn -B -f java/pom.xml -Pj25 -pl holiday-api-j25 -am package -DskipTests

FROM bellsoft/liberica-openjre-alpine:25
WORKDIR /app
COPY --from=build /workspace/java/holiday-api-j25/target/holiday-api-j25-*.jar app.jar
EXPOSE 18080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
