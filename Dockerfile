# Stage 1: Build Angular Frontend
FROM node:18-alpine AS frontend-build
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Stage 2: Build Spring Boot Backend
FROM maven:3.9-eclipse-temurin-21-alpine AS backend-build
WORKDIR /app
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B
COPY backend/src ./src
# Angular-Build in Spring Boot static resources kopieren
COPY --from=frontend-build /app/dist/browser/ ./src/main/resources/static/
RUN mvn package -DskipTests -B

# Stage 3: Runtime
FROM eclipse-temurin:21-jre-alpine
LABEL app.version="3.8.0"
WORKDIR /app
COPY --from=backend-build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
