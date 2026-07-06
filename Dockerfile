# Étape 1 : Construction du projet (Build) avec Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Étape 2 : Création de l'image finale avec Java 21
FROM eclipse-temurin:21-jre-alpine
# Copie le .jar généré depuis l'étape précédente
COPY --from=build /app/target/*.jar app.jar
# Expose le port par défaut de Spring Boot (généralement 8080)
EXPOSE 8080
# Commande pour démarrer l'application
ENTRYPOINT ["java", "-jar", "/app.jar"]