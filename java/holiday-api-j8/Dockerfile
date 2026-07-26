FROM maven:3.9-eclipse-temurin-8 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -f java/pom.xml -pl holiday-api-j8 -am package -DskipTests

FROM bellsoft/liberica-openjre-debian:8
WORKDIR /app
COPY --from=build /workspace/java/holiday-api-j8/target/holiday-api-j8-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:-} -jar app.jar"]
