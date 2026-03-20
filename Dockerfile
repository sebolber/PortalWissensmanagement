# Stage 1: Build Angular Frontend
FROM node:20-alpine AS frontend-build
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json* ./
RUN npm install
COPY frontend/ ./
RUN npx ng build --configuration production

# Stage 2: Build Spring Boot Backend
FROM maven:3.9-eclipse-temurin-21-alpine AS backend-build
WORKDIR /app/backend
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B
COPY backend/src ./src
# Copy Angular build output into Spring Boot static resources
COPY --from=frontend-build /app/frontend/dist/browser/ ./src/main/resources/static/
RUN mvn package -DskipTests -B

# Stage 3: Runtime
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache postgresql-client
WORKDIR /app
COPY --from=backend-build /app/backend/target/*.jar app.jar
COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh
EXPOSE 8080
ENTRYPOINT ["/app/entrypoint.sh"]
