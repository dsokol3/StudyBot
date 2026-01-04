## Multi-stage Dockerfile for building the ChatBot backend (Java 17, Maven)

# Build stage: use Maven with Java 17 to build the jar
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy only what we need to take advantage of build cache
# The repository may not include the Maven wrapper (`mvnw`); use the image's `mvn` instead.
COPY pom.xml ./
COPY src ./src

# Build the application (skip tests for faster builds; change if desired)
RUN mvn -B -DskipTests package

# Runtime stage: slim JRE
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy runnable jar from build stage
COPY --from=build /workspace/target/*.jar ./app.jar

# Expose application port
EXPOSE 8080

# Use a non-root user where possible
USER 1000:1000

ENTRYPOINT ["java","-jar","/app/app.jar"]
