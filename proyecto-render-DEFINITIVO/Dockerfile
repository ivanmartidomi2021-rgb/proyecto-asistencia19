# ---------- Etapa 1: build ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copiamos primero el pom.xml para aprovechar la cache de dependencias
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Copiamos el resto del código y compilamos
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Etapa 2: runtime ----------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copiamos el jar generado en la etapa de build
COPY --from=build /app/target/*.jar app.jar

# Render asigna el puerto real mediante la variable de entorno PORT
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
