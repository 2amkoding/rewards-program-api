# Multi-stage build for smaller image size
# Stage 1: Build
FROM docker.io/library/maven:3.9-eclipse-temurin-17 AS builder

# Set working directory
WORKDIR /app

# Copy POM and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM docker.io/eclipse-temurin:17-jre-alpine

# Create user (Podman runs rootless by default, but we add for compatibility)
RUN addgroup -g 1000 spring && \
    adduser -D -u 1000 -G spring spring

# Set working directory
WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Create logs directory
RUN mkdir -p /app/logs && chown -R spring:spring /app

# Switch to non-root user
USER 1000:1000

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Set JVM options for container environment
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]