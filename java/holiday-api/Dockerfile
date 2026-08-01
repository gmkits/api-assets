FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -f java/pom.xml -pl holiday-api -am package -DskipTests

FROM bellsoft/liberica-openjre-debian:17
WORKDIR /app
COPY --from=build /workspace/java/holiday-api/target/holiday-api-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:-} -jar app.jar"]
