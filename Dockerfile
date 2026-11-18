FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY gradlew gradlew.bat build.gradle settings.gradle gradle.properties ./
COPY gradle ./gradle
RUN chmod +x gradlew
COPY src ./src
RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/build/libs/spvb-trading.jar app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
