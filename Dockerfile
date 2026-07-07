# Étape 1 : Construction du projet (Build) avec Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Étape 2 : Création de l'image finale avec Java 21
FROM eclipse-temurin:21-jre-alpine
# Copie le .jar généré depuis l'étape précédente
COPY --from=build /app/target/*.jar app.jar

# Commande pour démarrer l'application :
# 1. -Dserver.port=${PORT} : injecte le port dynamique de Render
# 2. -Dspring.profiles.active=prod : force l'application à utiliser le profil 'prod'
ENTRYPOINT ["java", "-Dserver.port=${PORT}", "-Dspring.profiles.active=prod", "-jar", "/app.jar"]
