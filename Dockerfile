# Etapa 1: Compilar el proyecto dentro de la subcarpeta
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY . .
WORKDIR /app/proyecto-render-DEFINITIVO
RUN mvn clean package -DskipTests

# Etapa 2: Ejecutar la aplicación
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=build /app/proyecto-render-DEFINITIVO/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
