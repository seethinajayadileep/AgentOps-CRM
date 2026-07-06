# AgentOps CRM - Production Dockerfile
# Multi-stage build for Java 21 Spring Boot application

# ============================================================================
# Stage 1: Build Stage
# ============================================================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

# Set working directory
WORKDIR /app

# Copy POM and resolve dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Run tests
RUN mvn test -B

# Package application (skip tests since we already ran them)
RUN mvn clean package -DskipTests -B

# ============================================================================
# Stage 2: Runtime Stage
# ============================================================================
FROM eclipse-temurin:21-jre-alpine

# Install CA certificates for Apify TLS and other external APIs
RUN apk add --no-cache ca-certificates tzdata curl

# Create non-root user for security
RUN addgroup -g 1001 appgroup && \
    adduser -D -u 1001 -G appgroup appuser

# Set working directory
WORKDIR /app

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Change ownership
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

#

 Expose port (Railway will override with PORT env var)
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:${PORT:-8080}/actuator/health || exit 1

# JVM settings for container
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:InitialRAMPercentage=50.0 \
               -XX:+ExitOnOutOfMemoryError \
               -Djava.security.egd=file:/dev/./urandom"

# Graceful shutdown timeout (30 seconds)
ENV SERVER_SHUTDOWN=graceful
ENV SPRING_LIFECYCLE_TIMEOUT_PER_SHUTDOWN_PHASE=30s

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
