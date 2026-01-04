## Multi-stage Dockerfile for building the ChatBot backend (Java 21, Maven) with Vue.js frontend

# Build frontend stage
FROM node:18 AS frontend-build
WORKDIR /frontend
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ ./
RUN npm run build

# Build backend stage: use Maven with Java 21 to build the jar
FROM maven:3.9.4-eclipse-temurin-21 AS backend-build
WORKDIR /workspace

# Copy only what we need to take advantage of build cache
# The repository may not include the Maven wrapper (`mvnw`); use the image's `mvn` instead.
COPY pom.xml ./
COPY src ./src

# Copy built frontend to Spring Boot static resources
COPY --from=frontend-build /frontend/dist ./src/main/resources/static/

# Build the application (skip tests for faster builds; change if desired)
RUN mvn -B -DskipTests package

# Runtime stage: slim JRE
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy runnable jar from build stage
COPY --from=backend-build /workspace/target/*.jar ./app.jar

# Expose application port
EXPOSE 8080

# Use a non-root user where possible
USER 1000:1000

ENTRYPOINT ["java","-jar","/app/app.jar"]
