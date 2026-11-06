# Etapa 1: compila o jar executavel do Spring Boot com Gradle 9 e Java 21
FROM gradle:9.3.1-jdk21 AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY src ./src
RUN gradle --no-daemon bootJar

# Etapa 2: imagem final apenas com o JRE e o jar
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
# O Render indica a porta na variavel PORT (ver application.properties)
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
