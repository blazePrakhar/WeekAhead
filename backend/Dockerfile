FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY backend/pom.xml backend/pom.xml
COPY backend/src backend/src

RUN mvn -f backend/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/backend/target/weekahead-backend-0.0.1-SNAPSHOT.jar app.jar
COPY database/migrations /app/database/migrations

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]