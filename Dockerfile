# ================================================================
# EcoTrace — Multi-stage Docker Build
# Stage 1: Build the JAR with Maven
# Stage 2: Run with a minimal JRE image
# ================================================================

# ---- Build Stage ----
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Cache dependencies first (layer caching optimization)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Create data directory for H2 file-based storage
RUN mkdir -p /app/data

# Expose the default Spring Boot port
EXPOSE 8080

# Health check to verify the app is running
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -qO- http://localhost:8080/api/auth/login || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
