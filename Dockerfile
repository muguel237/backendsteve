# Étape 1 : Construction du projet (Build) avec Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Étape 2 : Création de l'image finale avec Java 21
FROM eclipse-temurin:21-jre-alpine
# Copie le .jar généré depuis l'étape précédente
COPY --from=build /app/target/*.jar app.jar

# Commande pour démarrer l'application en injectant le port de Render
# et en permettant à Spring de mieux lire les variables d'environnement
ENTRYPOINT ["java", "-Dserver.port=${PORT}", "-jar", "/app.jar"]
