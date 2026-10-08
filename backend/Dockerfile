FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY backend/pom.xml .
COPY backend/src ./src
COPY frontend ./frontend

RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar
COPY --from=build /app/frontend ./frontend

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]