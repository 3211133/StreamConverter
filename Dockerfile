# Multi-stage Docker build for StreamConverter
# Ensures consistent build environment across all platforms

# Build stage - Use OpenJDK 21 with Gradle
FROM openjdk:21-jdk-slim AS builder

LABEL maintainer="StreamConverter Team"
LABEL description="Build stage for StreamConverter Java library"

# Install necessary build tools
RUN apt-get update && apt-get install -y \
    curl \
    unzip \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy Gradle wrapper and build files first (for better caching)
COPY gradle/ gradle/
COPY gradlew gradlew.bat gradle.properties settings.gradle.kts ./
COPY build.gradle.kts ./

# Copy module build configurations
COPY streamconverter-core/build.gradle.kts streamconverter-core/
COPY streamconverter-web/build.gradle.kts streamconverter-web/
COPY streamconverter-examples/build.gradle.kts streamconverter-examples/
COPY streamconverter-tools/build.gradle.kts streamconverter-tools/

# Download dependencies (cached layer)
RUN ./gradlew --no-daemon dependencies

# Copy source code
COPY streamconverter-core/src/ streamconverter-core/src/
COPY streamconverter-web/src/ streamconverter-web/src/
COPY streamconverter-examples/src/ streamconverter-examples/src/
COPY streamconverter-tools/src/ streamconverter-tools/src/

# Copy configuration files
COPY spotbugs-exclude.xml ./

# Build the application (skip tests for Docker build speed)
RUN ./gradlew --no-daemon clean build -x test

# Production stage - Lightweight JRE
FROM openjdk:21-jre-alpine AS production

LABEL maintainer="StreamConverter Team"
LABEL description="Production StreamConverter Web API"
LABEL version="1.2.0"

# Create non-root user for security
RUN addgroup -g 1001 streamconverter && \
    adduser -D -s /bin/sh -u 1001 -G streamconverter streamconverter

# Set working directory
WORKDIR /app

# Copy built JAR from builder stage
COPY --from=builder /app/streamconverter-web/build/libs/*-boot.jar app.jar

# Create directories for data processing
RUN mkdir -p /app/data/input /app/data/output /app/logs && \
    chown -R streamconverter:streamconverter /app

# Switch to non-root user
USER streamconverter

# Expose Spring Boot default port
EXPOSE 8080

# Health check endpoint
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# JVM memory configuration optimized for container
ENV JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Spring Boot configuration
ENV SPRING_PROFILES_ACTIVE=production

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# Development stage - Full development environment
FROM builder AS development

LABEL description="Development environment for StreamConverter"

# Install additional development tools
RUN apt-get update && apt-get install -y \
    git \
    vim \
    htop \
    && rm -rf /var/lib/apt/lists/*

# Copy built artifacts
RUN ./gradlew --no-daemon publishToMavenLocal

# Set development environment
ENV SPRING_PROFILES_ACTIVE=development
ENV JAVA_OPTS="-Xms1g -Xmx3g -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

# Expose ports for Spring Boot and debugger
EXPOSE 8080 5005

# Keep container running for development
CMD ["tail", "-f", "/dev/null"]