# Stage 1: Build Spring Boot Backend (includes static HTML)
FROM maven:3.9-eclipse-temurin-21-alpine AS backend-build
WORKDIR /app
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B
COPY backend/src ./src
RUN mvn package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
LABEL app.version="2.1.0"
RUN apk add --no-cache postgresql-client
WORKDIR /app
COPY --from=backend-build /app/target/*.jar app.jar
COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh
EXPOSE 8080
ENTRYPOINT ["/app/entrypoint.sh"]
